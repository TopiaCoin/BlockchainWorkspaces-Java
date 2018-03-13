package io.topiacoin.dht.action;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.StoreValueRequest;
import io.topiacoin.dht.messages.StoreValueResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.routing.RoutingTable;

import java.util.List;

public class StoreValueAction implements Action, ResponseHandler{

    private final Node _thisNode;
    private final NodeID _keyID;
    private final DHTComponents _dhtComponents;
    private final StoreValueRequest _request;

    private int _storageCount = 0;

    private final RoutingTable _routingTable;
    private final CommunicationServer _communicationServer;

    public StoreValueAction(Node thisNode, String key, String value, DHTComponents dhtComponents) {
        _thisNode = thisNode;
        _keyID = new NodeID(key);

        _dhtComponents = dhtComponents;
        _request = new StoreValueRequest(key, value);

        _routingTable = dhtComponents.getRoutingTable();
        _communicationServer = dhtComponents.getCommunicationServer();
    }

    public void execute() {
        // Perform a Node Lookup on our NodeID of the Key to get the K closest nodes to the key's NodeID
        NodeLookupAction nodeLookupAction = new NodeLookupAction(_thisNode, _keyID, this._dhtComponents);
        nodeLookupAction.execute();
        List<Node> closestNodes = nodeLookupAction.getClosestNodes();

        // Send the Store Value Request to each of the K closet nodes
        for ( Node node : closestNodes ) {
            if ( node.equals(_thisNode)) {
                this._dhtComponents.getValueStorage().setValue(_request.getKey(), _request.getValue());
                _storageCount++ ;
            } else {
                _communicationServer.sendMessage(node, _request, this);
            }
        }
    }

    // Returns the number of nodes on which this value has been stored
    public int getStorageCount() {
        return _storageCount;
    }

    public void receive(Node origin, Message msg, int msgID) {
        if ( msg instanceof StoreValueResponse ) {
            StoreValueResponse response = (StoreValueResponse)msg;

            if ( response.isSuccess() ) {
                _storageCount++;
            }
        }
    }

    public void timeout(int msgID) {
        // NOOP - We aren't tracking failures currently
    }
}
