package io.topiacoin.dht.handlers;

import io.topiacoin.crypto.HashUtils;
import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.intf.ValueStorage;
import io.topiacoin.dht.messages.FetchValueRequest;
import io.topiacoin.dht.messages.FetchValueResponse;
import io.topiacoin.dht.messages.NodeLookupResponse;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FetchValueRequestHandler implements ResponseHandler {

    private final Log _log = LogFactory.getLog(this.getClass());

    private final DHTComponents _dhtComponents;

    public FetchValueRequestHandler(DHTComponents communicationServer) {
        this._dhtComponents = communicationServer;
    }

    public void receive(Node origin, Message msg, int msgID) {
        if (msg instanceof FetchValueRequest) {
            FetchValueRequest fvrMsg = (FetchValueRequest) msg;

            String key = fvrMsg.getKey();
            Set<String> values = new TreeSet<String>();

            // Get the value from the Hash Table
            ValueStorage valueStorage = _dhtComponents.getValueStorage();
            Collection<String> localValues = valueStorage.getValues(key);

            if (localValues != null) {
                values.addAll(localValues);

                FetchValueResponse response = new FetchValueResponse();
                response.setKey(key);
                response.setValues(values);

                CommunicationServer communicationServer = this._dhtComponents.getCommunicationServer();

                communicationServer.reply(origin, response, msgID);
            } else {
                // Respond with a NodeLookUpResponse Message containing the closet nodes to the requested Key
                byte[] keyIDBytes = HashUtils.sha1(key.getBytes());

                NodeID keyID = new NodeID(keyIDBytes, null);
                int numNodes = _dhtComponents.getConfiguration().getK();

                List<Node> closetNodes = _dhtComponents.getRoutingTable().findClosest(keyID, numNodes);

                NodeLookupResponse response = new NodeLookupResponse(closetNodes);

                CommunicationServer communicationServer = this._dhtComponents.getCommunicationServer();

                communicationServer.reply(origin, response, msgID);
            }
        }
    }

    public void timeout(int msgID) {

    }
}
