package io.topiacoin.dht.handlers;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.content.ValueStorage;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.FetchValueRequest;
import io.topiacoin.dht.messages.FetchValueResponse;
import io.topiacoin.dht.messages.NodeLookupResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class FetchValueRequestHandler implements ResponseHandler{

    private final Log _log = LogFactory.getLog(this.getClass());

    private final DHTComponents _dhtComponents;

    public FetchValueRequestHandler(DHTComponents communicationServer) {
        this._dhtComponents = communicationServer;
    }

    public void receive(Node origin, Message msg, int msgID) {
        if (msg instanceof FetchValueRequest) {
            FetchValueRequest fvrMsg = (FetchValueRequest)msg;

            String key = fvrMsg.getKey();
            String value = null;

            // Get the value from the Hash Table
            ValueStorage valueStorage = _dhtComponents.getValueStorage();
            value = valueStorage.getValue(key);

            if ( value != null ) {
                FetchValueResponse response = new FetchValueResponse();
                response.setKey(key);
                response.setValue(value);

                CommunicationServer communicationServer = this._dhtComponents.getCommunicationServer();

                communicationServer.reply(origin, response, msgID);
            } else {
                // TODO - Respond with a NodeLookUpResponse Message containing the closet nodes to the requested Key
                try {
                    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                    byte[] keyIDBytes = sha1.digest(key.getBytes());

                    NodeID keyID = new NodeID(keyIDBytes, null);
                    int numNodes = _dhtComponents.getConfiguration().getK();

                    List<Node> closetNodes = _dhtComponents.getRoutingTable().findClosest(keyID, numNodes);

                    NodeLookupResponse response = new NodeLookupResponse(closetNodes);

                    CommunicationServer communicationServer = this._dhtComponents.getCommunicationServer();

                    communicationServer.reply(origin, response, msgID);
                } catch ( NoSuchAlgorithmException e ) {
                    _log.fatal("Java no longer supports SHA-1!");
                }
            }
        }
    }

    public void timeout(int msgID) {

    }
}
