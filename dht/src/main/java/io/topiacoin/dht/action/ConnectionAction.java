package io.topiacoin.dht.action;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.exceptions.ConnectionException;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.ConnectRequest;
import io.topiacoin.dht.messages.ConnectResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.routing.RoutingTable;

import java.io.IOException;

public class ConnectionAction implements Action, ResponseHandler {

    private final Node _thisNode;
    private final Node _targetNode;
    private final DHTComponents _dhtComponents;

    private final RoutingTable _routingTable;
    private final CommunicationServer _communicationServer;

    private final ConnectRequest _request;
    private boolean _connected;

    public ConnectionAction(Node thisNode, Node targetNode, DHTComponents dhtComponents) {
        this._thisNode = thisNode;
        this._targetNode = targetNode;
        this._dhtComponents = dhtComponents;

        _routingTable = _dhtComponents.getRoutingTable();
        _communicationServer = _dhtComponents.getCommunicationServer();
        _request = new ConnectRequest();

        _connected = false;
    }

    public synchronized void execute() throws IOException {

        // Send Connect Message to the _targetNode and wait for a response.
        _communicationServer.sendMessage(_targetNode, _request, this);

        try {
            // Wait for a response to the message
            long requestTimeLimit = this._dhtComponents.getConfiguration().getOperationTimeout();
            long startTime = System.currentTimeMillis();
            long endTime = startTime + requestTimeLimit;
            long now = startTime;
            int waitTime = 10;
            while (!_connected && now < endTime) {
                if (!_connected) {
                    wait(waitTime);
                } else {
                    // We are connected!
                    break;
                }
                now = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while Connecting", e);
        }

        // If no response was received, throw an exception
        if (!_connected) {
            throw new ConnectionException("Failed To connect to the target node");
        }

        // Perform a Node Lookup on our NodeID to pre-populate the routing table
        NodeLookupAction nodeLookupAction = new NodeLookupAction(_thisNode, _targetNode.getNodeID(), _dhtComponents);
        nodeLookupAction.execute();

        // Perform a Bucket Refresh to fully populate the routing table
        BucketRefreshAction bucketRefreshAction = new BucketRefreshAction(_thisNode, _dhtComponents);
        bucketRefreshAction.execute();
    }

    public void receive(Node origin, Message msg, int msgID) {
        if (msg instanceof ConnectResponse) {
            ConnectResponse response = (ConnectResponse) msg;

            _routingTable.insert(origin);
            this._connected = true;
        }
    }

    public void timeout(int msgID) {
        this._connected = false ;
    }
}
