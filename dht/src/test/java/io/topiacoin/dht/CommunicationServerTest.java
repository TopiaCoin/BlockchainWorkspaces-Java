package io.topiacoin.dht;

import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.messages.StoreValueRequest;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
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

        KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        MessageSigner messageSigner = new DSAMessageSigner();

        CommunicationServer communicationServer = new CommunicationServer(12345, config, messageFactory, keyPair, messageSigner);

        Node recipient = new Node(new NodeID(), InetAddress.getLocalHost(), 12345) ;
        Message message = new StoreValueRequest(key, value);

        success = false ;
        communicationServer.sendMessage(recipient, message, new ResponseHandler() {
            public void receive(Message msg, int msgID) {
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
