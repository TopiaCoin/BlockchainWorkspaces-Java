package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.routing.RoutingTable;

import java.net.UnknownHostException;
import java.util.List;

public class NodeLookupResponseHandler implements ResponseHandler{

    private final RoutingTable routingTable;

    public NodeLookupResponseHandler(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    public void receive(Message msg, int msgID) {
        if ( msg instanceof NodeLookupResponseMessage ) {
            NodeLookupResponseMessage nodeLookupResponseMessage = (NodeLookupResponseMessage)msg;

            List<NodeID> nodeIDs = nodeLookupResponseMessage.getNodeIDs();
            for ( NodeID nodeID : nodeIDs ) {
                try {
                    Node node = new Node(nodeID, "localhost", 8765);
                    this.routingTable.insert(node);
                } catch ( UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void timeout(int msgID) {

    }
}
