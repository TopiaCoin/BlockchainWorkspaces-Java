package io.topiacoin.dht.messages;

import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.DHTTestConfiguration;
import io.topiacoin.dht.handlers.ConnectRequestHandler;
import io.topiacoin.dht.handlers.FetchValueRequestHandler;
import io.topiacoin.dht.handlers.NodeLookupRequestHandler;
import io.topiacoin.dht.handlers.RemoveValueRequestHandler;
import io.topiacoin.dht.handlers.StoreValueRequestHandler;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MessageFactoryTest {

    @Test
    public void testCreateConnectRequest() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        ConnectRequest message = new ConnectRequest() ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(ConnectRequest.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateConnectResponse() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        ConnectResponse message = new ConnectResponse() ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(ConnectResponse.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateFetchValueRequest() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        FetchValueRequest message = new FetchValueRequest() ;
        message.setKey("argh");

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(FetchValueRequest.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateFetchValueResponse() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        FetchValueResponse message = new FetchValueResponse() ;
        message.setKey("pirates");
        message.setValues(new ArrayList<String>());

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(FetchValueResponse.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateStoreValueRequest() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        StoreValueRequest message = new StoreValueRequest() ;
        message.setKey("key");
        message.setValue("value");

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(StoreValueRequest.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateStoreValueResponse() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        StoreValueResponse message = new StoreValueResponse() ;
        message.setKey("pirates");
        message.setSuccess(true);

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(StoreValueResponse.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateRemoveValueRequest() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        RemoveValueRequest message = new RemoveValueRequest() ;
        message.setKey("key");
        message.setValue("value");

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(RemoveValueRequest.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateRemoveValueResponse() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        RemoveValueResponse message = new RemoveValueResponse() ;
        message.setKey("pirates");
        message.setSuccess(true);

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(RemoveValueResponse.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateNodeLookupRequest() {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);
        NodeID nodeID = nodeIDGenerator.generateNodeID();

        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        NodeLookupRequest message = new NodeLookupRequest(nodeID) ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(NodeLookupRequest.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateNodeLookupResponse() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        List<Node> nodeIDs = new ArrayList<Node>();
        NodeLookupResponse message = new NodeLookupResponse(nodeIDs) ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(NodeLookupResponse.TYPE, buffer) ;

        assertEquals ( message.getClass(), decodedMessage.getClass()) ;
        assertEquals ( message, decodedMessage) ;
    }

    @Test
    public void testCreateConnectHandler() {

        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        ResponseHandler responseHandler = messageFactory.createReceiver(ConnectRequest.TYPE) ;

        assertEquals(ConnectRequestHandler.class, responseHandler.getClass());
    }

    @Test
    public void testCreateFetchValueHandler() {

        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        ResponseHandler responseHandler = messageFactory.createReceiver(FetchValueRequest.TYPE) ;

        assertEquals(FetchValueRequestHandler.class, responseHandler.getClass());
    }

    @Test
    public void testCreateNodeLookupHandler() {

        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        ResponseHandler responseHandler = messageFactory.createReceiver(NodeLookupRequest.TYPE) ;

        assertEquals(NodeLookupRequestHandler.class, responseHandler.getClass());
    }

    @Test
    public void testCreateRemoveValueHandler() {

        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        ResponseHandler responseHandler = messageFactory.createReceiver(RemoveValueRequest.TYPE) ;

        assertEquals(RemoveValueRequestHandler.class, responseHandler.getClass());
    }

    @Test
    public void testCreateStoreValueHandler() {

        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        ResponseHandler responseHandler = messageFactory.createReceiver(StoreValueRequest.TYPE) ;

        assertEquals(StoreValueRequestHandler.class, responseHandler.getClass());
    }
}


