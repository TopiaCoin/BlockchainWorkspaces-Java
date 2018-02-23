package io.topiacoin.dht;

import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.intf.FetchContentCallback;
import io.topiacoin.dht.intf.StoreContentCallback;
import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.messages.StoreValueRequest;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.routing.RoutingTable;

import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.List;

public class DHT {

    private CommunicationServer _communicationServer ;
    private RoutingTable _routingTable;
    private MessageSigner _messageSigner;
    private KeyPair keyPair;

    public DHT(int udpPort, KeyPair keyPair) throws SocketException {

        this.keyPair = keyPair;
        _messageSigner = new DSAMessageSigner();
        _communicationServer = new CommunicationServer(udpPort, new DefaultConfiguration(), new MessageFactory(), keyPair, _messageSigner);
        _routingTable = new RoutingTable();
    }

    public static DHT loadState() {
        return null ;
    }

    public void saveState() {

    }

    public void bootstrap(Object node) {

    }

    public void shutdown(final boolean saveState) {

    }

    public int storeContent(String key, String value) {

        StoreValueRequest svr = new StoreValueRequest(key, value) ;

//        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
//        // Encode the request for transmission
//        svr.encodeMessage(buffer);
//
//        System.out.println ( "buffer.length: " + buffer.position() ) ;
//
//        buffer.flip();
//
//
//        // Generate signature for the request
//        byte[]signature = this._messageSigner.sign(buffer, this.keyPair) ;
//
//        System.out.println ( "signature length: " + signature.length);
//
//        buffer.limit(buffer.array().length);
//        buffer.put(signature) ;
//
//        System.out.println ( "buffer.length with Signature: " + buffer.position() ) ;

        // TODO Figure out where the message should be sent

        Node recipient= null;
        _communicationServer.sendMessage(recipient, svr, null);

        return 0;
    }

    public void storeContent(String key, String value, StoreContentCallback callback, Object context) {
    }

    public List<String> fetchContent(String key) {
        return null ;
    }

    public void fetchContent(String key, FetchContentCallback callback, Object context) {

    }

    public boolean removeContent(String key, String value) {
        return false;
    }

    public void removeContent(String key, String value, Object callback, Object context) {

    }

}
