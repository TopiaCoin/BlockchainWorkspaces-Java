package io.topiacoin.dht.action;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.RemoveValueRequest;
import io.topiacoin.dht.messages.RemoveValueResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.routing.RoutingTable;

import java.util.List;

public class RemoveValueAction implements Action, ResponseHandler{

    private final Node _thisNode;
    private final NodeID _keyID;
    private final DHTComponents _dhtComponents;
    private final RemoveValueRequest _request;

    private int _removeCount ;

    private final RoutingTable _routingTable;
    private final CommunicationServer _communicationServer;

    public RemoveValueAction(Node thisNode, String key, String value, DHTComponents dhtComponents) {
        _thisNode = thisNode;
        _keyID = new NodeID(key);

        _dhtComponents = dhtComponents;
        _request = new RemoveValueRequest();
        _request.setKey(key);
        _request.setValue(value);

        _routingTable = dhtComponents.getRoutingTable();
        _communicationServer = dhtComponents.getCommunicationServer();

        _removeCount = 0;
    }

    public void execute() {
        // Perform a Node Lookup on our NodeID of the Key to get the K closest nodes to the key's NodeID
        NodeLookupAction nodeLookupAction = new NodeLookupAction(_thisNode, _keyID, this._dhtComponents);
        nodeLookupAction.execute();
        List<Node> closestNodes = nodeLookupAction.getClosestNodes();

        // Send the Store Value Request to each of the K closet nodes
        for ( Node node : closestNodes ) {
            if ( node.equals(_thisNode)) {
                this._dhtComponents.getValueStorage().removeValue(_request.getKey(), _request.getValue());
                _removeCount++;
            } else {
                _communicationServer.sendMessage(node, _request, this);
            }
        }
    }

    public int getRemoveCount() {
        return _removeCount;
    }

    // -------- Response Handler Methods --------

    public void receive(Node origin, Message msg, int msgID) {
        if ( msg instanceof RemoveValueResponse) {
            RemoveValueResponse response = (RemoveValueResponse)msg;

            if ( response.isSuccess() ) {
                _removeCount++;
            }
        }
    }

    public void timeout(int msgID) {

    }
}
