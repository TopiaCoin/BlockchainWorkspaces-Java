package io.topiacoin.dht.action;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.NodeLookupRequest;
import io.topiacoin.dht.messages.NodeLookupResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.routing.RoutingTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.topiacoin.dht.action.NodeQueryState.*;

public class NodeLookupAction implements Action, ResponseHandler {

    private final Node _thisNode;
    private final DHTComponents _dhtComponents;
    private final NodeLookupRequest _lookupRequest;

    private final long _maxConcurrentQueries;

    private final Map<Node, NodeQueryState> _nodeQueryStateMap;
    private final Map<Integer, Node> _activeQueryMap;

    private final RoutingTable _routingTable;
    private final CommunicationServer _communicationServer;

    public NodeLookupAction(final Node thisNode, final NodeID targetID, final DHTComponents dhtComponents) {
        _thisNode = thisNode;
        _dhtComponents = dhtComponents;

        _maxConcurrentQueries = _dhtComponents.getConfiguration().getMaxConcurrentMessages();

        _nodeQueryStateMap = new TreeMap<Node, NodeQueryState>();
        _activeQueryMap = new HashMap<Integer, Node>();

        _lookupRequest = new NodeLookupRequest(targetID);

        _routingTable = this._dhtComponents.getRoutingTable();
        _communicationServer = this._dhtComponents.getCommunicationServer();
    }

    public synchronized void execute() {

        // Add all known nodes to the list of those to ask
        addUnaskedNodes(_routingTable.getAllNodes());

        // Mark ourselves as having already been asked
        _nodeQueryStateMap.put(_thisNode, NodeQueryState.ASKED);

        try {
            // Loop, waiting for the queries to complete, for up to the request timeout
            long requestTimeLimit = this._dhtComponents.getConfiguration().getOperationTimeout();
            long startTime = System.currentTimeMillis();
            long endTime = startTime + requestTimeLimit ;
            long now = startTime;
            int waitTime = 10;
            while (now < endTime) {
                if (!processQueriesOrFinish()) {
                    wait(waitTime);
                } else {
                    // The queries have all finished
//                    System.out.print ("All Queries Finished!  ");
                    break;
                }
                now = System.currentTimeMillis();
            }
//            now = System.currentTimeMillis();
//            System.out.println ( "Completed Node Lookup in " + (now - startTime) + "ms" ) ;
        } catch (InterruptedException e) {
            throw new RuntimeException("Node Lookup was Interrupted", e);
        }
    }

    public List<Node> getClosestNodes() {
        return getClosestNonFailedNodesWithState(NodeQueryState.ASKED) ;
    }


    // -------- Response Handler Methods --------

    public void receive(Node origin, Message msg, int msgID) {
        if (msg instanceof NodeLookupResponse) {
            NodeLookupResponse response = (NodeLookupResponse) msg;

            // Update the last seen date for the Node from which the message originated
            _routingTable.insert(origin);

            // Update the State in the Query State Map, and remove the entry from the Active Query Map
            this._nodeQueryStateMap.put(origin, NodeQueryState.ASKED);
            this._activeQueryMap.remove(msgID);

            // Add the returned nodes to the list of nodes available for querying
            addUnaskedNodes(response.getNodes());
        }

        processQueriesOrFinish();
    }

    public void timeout(int msgID) {
        Node failedNode = _activeQueryMap.remove(msgID);
        if (failedNode != null) {
            _nodeQueryStateMap.put(failedNode, FAILED);
            _routingTable.setUnresponsiveNode(failedNode);
        }

        this.processQueriesOrFinish();
    }

    // -------- Private Methods --------

    private void addUnaskedNodes(List<Node> nodesToAdd) {
        for (Node node : nodesToAdd) {
            // If this node isn't already in the map, then add it.
            if (!_nodeQueryStateMap.containsKey(node)) {
                _nodeQueryStateMap.put(node, UNASKED);
            }
        }
    }

    private boolean processQueriesOrFinish() {

        if (this._activeQueryMap.size() >= this._maxConcurrentQueries) {
            // Too many queries are currently running
            return false;
        }

        // Get the K closest nodes to the targetID from the Routing Table
        List<Node> closestNodes = getClosestNonFailedNodesWithState(UNASKED);

        if (closestNodes.isEmpty() && this._activeQueryMap.isEmpty()) {
            // We have completed all our queries.  Notify anyone blocked in execute.
            synchronized (this) {
                this.notifyAll();
            }
            return true;
        }

        // For each of the closest nodes
        for (Node node : closestNodes) {

            // Send the Lookup Request to the Node, then mark the node as having a query in progress
            int msgID = _communicationServer.sendMessage(node, _lookupRequest, this);
            _activeQueryMap.put(msgID, node);
            _nodeQueryStateMap.put(node, AWAITING);

            // If we have now reached the maximum concurrent queries, break out of the loop
            if (_activeQueryMap.size() >= this._maxConcurrentQueries) {
                break;
            }
        }

        return false;
    }

    private List<Node> getClosestNonFailedNodesWithState(NodeQueryState state) {
        int nodesRemainingToFetch = _dhtComponents.getConfiguration().getK();
        List<Node> closestNodes = new ArrayList<Node>(nodesRemainingToFetch);

        for (Map.Entry<Node, NodeQueryState> entry : _nodeQueryStateMap.entrySet()) {
            if (entry.getValue() != FAILED) {
                if (entry.getValue() == state) {
                    closestNodes.add(entry.getKey());
                }

                if (--nodesRemainingToFetch == 0) {
                    break;
                }
            }
        }

        return closestNodes;
    }
}
