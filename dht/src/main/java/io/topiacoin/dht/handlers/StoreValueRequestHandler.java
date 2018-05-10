package io.topiacoin.dht.handlers;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.intf.ValueStorage;
import io.topiacoin.dht.messages.StoreValueRequest;
import io.topiacoin.dht.messages.StoreValueResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;

public class StoreValueRequestHandler implements ResponseHandler {

    private final DHTComponents _dhtComponents;

    public StoreValueRequestHandler(DHTComponents communicationServer) {
        this._dhtComponents = communicationServer;
    }

    public void receive(Node origin, Message msg, int msgID) {
        if ( msg instanceof StoreValueRequest) {
            StoreValueRequest strMsg = (StoreValueRequest)msg;

            String key = strMsg.getKey();
            String value = strMsg.getValue();
            long timeout = strMsg.getExpirationTime();

            // Put the value in the HashTable
            ValueStorage valueStorage = _dhtComponents.getValueStorage();
            if ( strMsg.isRefresh() ) {
                valueStorage.refreshValue(key, value, timeout);
            } else {
                valueStorage.setValue(key, value, timeout);
            }

            StoreValueResponse response = new StoreValueResponse();
            response.setKey(key);
            response.setSuccess(true) ;

            CommunicationServer communicationServer = _dhtComponents.getCommunicationServer();

            communicationServer.reply(origin, response, msgID);
        }
    }

    public void timeout(int msgID) {

    }
}
