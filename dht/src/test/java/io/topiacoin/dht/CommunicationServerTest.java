package io.topiacoin.dht;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.messages.StoreValueRequest;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.Test;

import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static junit.framework.TestCase.*;

public class CommunicationServerTest {

    private boolean success = false ;

    @Test
    public void testSendingAndReceivingMessages() throws Exception {

        String key = "this is a key";
        String value = "this is a value";

        DefaultConfiguration config = new DefaultConfiguration();
        MessageFactory messageFactory = new MessageFactory();
        messageFactory.initialize();

        KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        MessageSigner messageSigner = new DSAMessageSigner();

        Configuration configuration = new DefaultConfiguration();

        DHTComponents dhtComponents = new DHTComponents();
        dhtComponents.setMessageFactory(messageFactory);
        dhtComponents.setMessageSigner(messageSigner);
        dhtComponents.setConfiguration(configuration);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);
        NodeID thisNodeID = nodeIDGenerator.generateNodeID();
        Node node = new Node(thisNodeID, "localhost", 12345);

        CommunicationServer communicationServer = new CommunicationServer(12345, keyPair, node);
        communicationServer.setDHTComponents(dhtComponents);
        communicationServer.start();

        NodeID nodeID = nodeIDGenerator.generateNodeID();

        Node recipient = new Node(nodeID, InetAddress.getLocalHost(), 12345) ;
        Message message = new StoreValueRequest(key, value);

        success = false ;
        communicationServer.sendMessage(recipient, message, new ResponseHandler() {
            public void receive(Node origin, Message msg, int msgID) {
                success = true ;
            }

            public void timeout(int msgID) {
                success = false ;
            }
        });

        Thread.sleep (1000) ;

        assertTrue ( success ) ;
    }

}
