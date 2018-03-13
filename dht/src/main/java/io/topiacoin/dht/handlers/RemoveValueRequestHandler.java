package io.topiacoin.dht.handlers;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.content.ValueStorage;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.RemoveValueRequest;
import io.topiacoin.dht.messages.RemoveValueResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;

public class RemoveValueRequestHandler implements ResponseHandler {

    private final DHTComponents _dhtComponents;

    public RemoveValueRequestHandler(DHTComponents communicationServer) {
        this._dhtComponents = communicationServer;
    }

    public void receive(Node origin, Message msg, int msgID) {
        if ( msg instanceof RemoveValueRequest) {
            RemoveValueRequest strMsg = (RemoveValueRequest)msg;

            String key = strMsg.getKey();
            String value = strMsg.getValue();

            // Put the value in the HashTable
            ValueStorage valueStorage = _dhtComponents.getValueStorage();
            valueStorage.removeValue(key, value);

            RemoveValueResponse response = new RemoveValueResponse();
            response.setKey(key);
            response.setSuccess(true) ;

            CommunicationServer communicationServer = _dhtComponents.getCommunicationServer();

            communicationServer.reply(origin, response, msgID);
        }
    }

    public void timeout(int msgID) {

    }
}
