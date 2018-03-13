package io.topiacoin.dht.action;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.FetchValueRequest;
import io.topiacoin.dht.messages.FetchValueResponse;
import io.topiacoin.dht.messages.NodeLookupResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.routing.NodeIDComparator;
import io.topiacoin.dht.routing.RoutingTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.topiacoin.dht.action.NodeQueryState.*;

public class FetchValueAction implements Action, ResponseHandler{

    private final DHTComponents _dhtComponents;
    private final NodeID _keyID;

    private final FetchValueRequest _request;

    private final long _maxConcurrentQueries;

    private final Map<Node, NodeQueryState> _nodeQueryStateMap;
    private final Map<Integer, Node> _activeQueryMap;
    private final CommunicationServer _communicationServer;
    private final RoutingTable _routingTable;

    private boolean _contentFound;
    private List<String> _content ;

    public FetchValueAction(final String key, final DHTComponents dhtComponents) {

        _keyID = new NodeID(key);
        _dhtComponents = dhtComponents;

        _maxConcurrentQueries = _dhtComponents.getConfiguration().getMaxConcurrentMessages();

        _nodeQueryStateMap = new TreeMap<Node, NodeQueryState>();
        _activeQueryMap = new HashMap<Integer, Node>();

        _request = new FetchValueRequest();
        _request.setKey( key);

        _routingTable = this._dhtComponents.getRoutingTable();
        _communicationServer = this._dhtComponents.getCommunicationServer();

        _contentFound = false ;
    }

    public synchronized void execute() {

        // Get the K closest nodes to the key's NodeID
        int numNodes = this._dhtComponents.getConfiguration().getK() ;
        List<Node> closestNodes = _routingTable.findClosest(_keyID, numNodes) ;

        addUnaskedNodes(closestNodes);

        try {
            // Loop, waiting for the queries to complete, for up to the request timeout
            long requestTimeLimit = this._dhtComponents.getConfiguration().getOperationTimeout();
            long startTime = System.currentTimeMillis();
            long endTime = startTime + requestTimeLimit ;
            long now = startTime;
            int waitTime = 10;
            while (now < endTime) {
                if (!_contentFound && !processQueriesOrFinish()) {
                    wait(waitTime);
                } else {
                    // The queries have all finished, or we have found the content we are looking for
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

    public boolean isContentFound() {
        return _contentFound;
    }

    public List<String> getContent() {
        return _content;
    }

    // -------- Response Handler Methods --------

    public void receive(Node origin, Message msg, int msgID) {
        if ( msg instanceof FetchValueResponse ) {
            // We receive a Found Reply, save the value and terminate the loop
            FetchValueResponse response = (FetchValueResponse)msg;

            // Update the routing table with origin so that it's last contact info is update
            _routingTable.insert(origin);

            // Mark the Origin node as complete
            _activeQueryMap.remove(msgID) ;
            _nodeQueryStateMap.put(origin, ASKED);

            String content = response.getValue() ;

            this._content = new ArrayList<String>() ;
            this._content.add(content) ;
            this._contentFound = true ;

            // Notify whoever is in the execute method that we have found the requested content
            synchronized (this) {
                this.notifyAll();
            }
        } else if ( msg instanceof NodeLookupResponse ) {
            // We receive a Node Reply, add the nodes to the list of nodes to query and continue
            NodeLookupResponse response = (NodeLookupResponse)msg;

            // Update the routing table with origin so that it's last contact info is update
            _routingTable.insert(origin);

            // Mark the Origin node as complete
            _activeQueryMap.remove(msgID) ;
            _nodeQueryStateMap.put(origin, ASKED);

            this.addUnaskedNodes(response.getNodes());
        }

        this.processQueriesOrFinish();
    }

    public void timeout(int msgID) {

    }

    // -------- Private Methods --------

    private void addUnaskedNodes(final List<Node> nodesToAdd) {
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

        // Sort the closest Nodes List by proximity to the Key ID
        Collections.sort(closestNodes, new NodeIDComparator(this._keyID)) ;

        // For each of the closest nodes
        for (Node node : closestNodes) {

            // Send the Lookup Request to the Node, then mark the node as having a query in progress
            int msgID = _communicationServer.sendMessage(node, _request, this);
            _activeQueryMap.put(msgID, node);
            _nodeQueryStateMap.put(node, AWAITING);

            // If we have now reached the maximum concurrent queries, break out of the loop
            if (_activeQueryMap.size() >= this._maxConcurrentQueries) {
                break;
            }
        }

        return false;
    }

    private List<Node> getClosestNonFailedNodesWithState(final NodeQueryState state) {
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
