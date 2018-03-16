package io.topiacoin.dht.action;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.ValueStorage;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.StoreValueRequest;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.routing.RoutingTable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ContentRefreshAction implements Action, ResponseHandler{

    private final Node _thisNode;
    private final DHTComponents _dhtComponents;

    public ContentRefreshAction(Node thisNode, DHTComponents dhtComponents) {
        _thisNode = thisNode;
        _dhtComponents = dhtComponents;
    }

    public void execute() {
        System.out.println ( "Executing a Content Refresh" ) ;

        int numNodes = _dhtComponents.getConfiguration().getK();
        ValueStorage valueStorage = _dhtComponents.getValueStorage();
        RoutingTable routingTable = _dhtComponents.getRoutingTable();
        CommunicationServer communicationServer = _dhtComponents.getCommunicationServer();

        // For each entry in the Storage,
        for ( Map.Entry<String, Collection<String>> entry: valueStorage.getValueMap().entrySet()) {
            String key = entry.getKey();
            Collection<String> values = entry.getValue();

            NodeID keyID = new NodeID(key);

            // Get the K closest nodes that should be storing this
            List<Node> closestNodes = routingTable.findClosest(keyID, numNodes) ;

            for ( String value : values ) {
                StoreValueRequest storeValueRequest = new StoreValueRequest();
                storeValueRequest.setKey(key);
                storeValueRequest.setValue(value);

                // Send a Store Value message to each Node
                for (Node node : closestNodes) {
                    communicationServer.sendMessage(node, storeValueRequest, null);
                }

                // If this node isn't one of the K closest, remove the value from Storage.
                if (!closestNodes.contains(_thisNode)) {
                    valueStorage.removeValue(key, value);
                }
            }
        }

        System.out.println ( "Executed a Content Refresh" ) ;
    }

    public void receive(Node origin, Message msg, int msgID) {

    }

    public void timeout(int msgID) {

    }
}
