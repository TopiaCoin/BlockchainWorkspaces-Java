package io.topiacoin.dht.handlers;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.ConnectRequest;
import io.topiacoin.dht.messages.ConnectResponse;
import io.topiacoin.dht.network.Node;

public class ConnectRequestHandler implements ResponseHandler {

    private final DHTComponents _dhtComponents;

    public ConnectRequestHandler(DHTComponents dhtComponents) {
        _dhtComponents = dhtComponents;
    }

    public void receive(Node origin, Message msg, int msgID) {
        if ( msg instanceof ConnectRequest) {
            ConnectRequest request = (ConnectRequest)msg;

            ConnectResponse response = new ConnectResponse();
            _dhtComponents.getCommunicationServer().reply(origin, response, msgID);
        }
    }

    public void timeout(int msgID) {

    }
}
