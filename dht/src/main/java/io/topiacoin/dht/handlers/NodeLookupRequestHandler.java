package io.topiacoin.dht.handlers;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.NodeLookupRequest;
import io.topiacoin.dht.messages.NodeLookupResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.routing.RoutingTable;

import java.util.List;

public class NodeLookupRequestHandler implements ResponseHandler {

    private DHTComponents _dhtComponents ;

    public NodeLookupRequestHandler(DHTComponents dhtComponents ) {
        _dhtComponents = dhtComponents;
    }

    public void receive(Node origin, Message msg, int msgID) {
        if (msg instanceof NodeLookupRequest) {
            NodeLookupRequest nodeLookupMessage = (NodeLookupRequest) msg;

            RoutingTable routingTable = _dhtComponents.getRoutingTable();
            CommunicationServer communicationServer = _dhtComponents.getCommunicationServer();

            List<Node> closestNodes = routingTable.findClosest(((NodeLookupRequest) msg).getLookupID(), 20);

            routingTable.insert(origin);

            NodeLookupResponse responseMessage = new NodeLookupResponse(closestNodes);
            communicationServer.reply(origin, responseMessage, msgID);
        }

    }

    public void timeout(int msgID) {

    }
}
