package io.topiacoin.dht;

import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.MessageSigner;
import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.content.InMemoryExpiringValueStorage;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.intf.ValueStorage;
import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.messages.StoreValueRequest;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.Test;

import java.net.InetAddress;
import java.security.KeyPair;

import static junit.framework.TestCase.*;

public class CommunicationServerTest {

    private boolean success = false ;

    @Test
    public void testSendingAndReceivingMessages() throws Exception {

        String key = "this is a key";
        String value = "this is a value";

        MessageFactory messageFactory1 = new MessageFactory();

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        MessageSigner messageSigner1 = new MessageSigner();

        Configuration configuration1 = new DefaultConfiguration();

        ValueStorage valueStorage1 = new InMemoryExpiringValueStorage(10000) ;

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration1);
        NodeID thisNodeID = nodeIDGenerator.generateNodeID();
        Node node1 = new Node(thisNodeID, "localhost", 12345);

        CommunicationServer communicationServer1 = new CommunicationServer(12345, keyPair1, node1);

        DHTComponents dhtComponents1 = new DHTComponents();
        dhtComponents1.setMessageFactory(messageFactory1);
        dhtComponents1.setMessageSigner(messageSigner1);
        dhtComponents1.setConfiguration(configuration1);
        dhtComponents1.setValueStorage(valueStorage1);
        dhtComponents1.setCommunicationServer(communicationServer1);

        communicationServer1.setDHTComponents(dhtComponents1);
        messageFactory1.setDhtComponents(dhtComponents1);

        messageFactory1.initialize();
        valueStorage1.initialize();

        try {
            communicationServer1.start();

            NodeID nodeID = nodeIDGenerator.generateNodeID();

            Node recipient = new Node(nodeID, InetAddress.getLocalHost(), 12345);
            StoreValueRequest message = new StoreValueRequest();
            message.setKey(key);
            message.setValue(value);

            success = false;
            communicationServer1.sendMessage(recipient, message, new ResponseHandler() {
                public void receive(Node origin, Message msg, int msgID) {
                    success = true;
                }

                public void timeout(int msgID) {
                    success = false;
                }
            });

            Thread.sleep(1000);

            assertTrue(success);
        } finally {
            communicationServer1.shutdown();
            Thread.sleep ( 500 ) ;
        }
    }

    @Test
    public void testSendingAndReceivingMessagesBetweenCommunicationServers() throws Exception {

        String key = "this is a key";
        String value = "this is a value";

        // Setup the First Comms Server

        MessageFactory messageFactory1 = new MessageFactory();

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        MessageSigner messageSigner1 = new MessageSigner();

        Configuration configuration1 = new DefaultConfiguration();

        ValueStorage valueStorage1 = new InMemoryExpiringValueStorage(10000) ;

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration1);
        NodeID thisNodeID = nodeIDGenerator.generateNodeID();
        Node node1 = new Node(thisNodeID, "localhost", 12345);

        CommunicationServer communicationServer1 = new CommunicationServer(12345, keyPair1, node1);

        DHTComponents dhtComponents1 = new DHTComponents();
        dhtComponents1.setMessageFactory(messageFactory1);
        dhtComponents1.setMessageSigner(messageSigner1);
        dhtComponents1.setConfiguration(configuration1);
        dhtComponents1.setValueStorage(valueStorage1);
        dhtComponents1.setCommunicationServer(communicationServer1);

        communicationServer1.setDHTComponents(dhtComponents1);
        messageFactory1.setDhtComponents(dhtComponents1);

        messageFactory1.initialize();
        valueStorage1.initialize();

        // Setup the second Comms Server

        MessageFactory messageFactory2 = new MessageFactory();

        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();
        MessageSigner messageSigner2 = new MessageSigner();

        Configuration configuration2 = new DefaultConfiguration();

        ValueStorage valueStorage2 = new InMemoryExpiringValueStorage(10000) ;

        NodeIDGenerator nodeIDGenerator2 = new NodeIDGenerator(configuration2);
        NodeID thisNodeID2 = nodeIDGenerator2.generateNodeID();
        Node node2 = new Node(thisNodeID, "localhost", 23456);

        CommunicationServer communicationServer2 = new CommunicationServer(23456, keyPair2, node2);

        DHTComponents dhtComponents2 = new DHTComponents();
        dhtComponents2.setMessageFactory(messageFactory2);
        dhtComponents2.setMessageSigner(messageSigner2);
        dhtComponents2.setConfiguration(configuration2);
        dhtComponents2.setValueStorage(valueStorage2);
        dhtComponents2.setCommunicationServer(communicationServer2);

        communicationServer2.setDHTComponents(dhtComponents2);
        messageFactory2.setDhtComponents(dhtComponents2);

        messageFactory2.initialize();
        valueStorage2.initialize();

        try {
            communicationServer1.start();
            communicationServer2.start();

            NodeID nodeID = nodeIDGenerator.generateNodeID();

            Node recipient = new Node(nodeID, InetAddress.getLocalHost(), 23456);
            StoreValueRequest message = new StoreValueRequest();
            message.setKey(key);
            message.setValue(value);

            success = false;
            communicationServer1.sendMessage(recipient, message, new ResponseHandler() {
                public void receive(Node origin, Message msg, int msgID) {
                    success = true;
                }

                public void timeout(int msgID) {
                    success = false;
                }
            });

            Thread.sleep(1000);

            assertTrue(success);
        } finally {
            communicationServer1.shutdown();
            communicationServer2.shutdown();

            Thread.sleep(500);
        }
    }
}
