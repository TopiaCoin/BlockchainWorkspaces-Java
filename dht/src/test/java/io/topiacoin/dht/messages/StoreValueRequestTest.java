package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class StoreValueRequestTest implements MessageTest {

    @Test
    public void testConstructorAndAccessors() throws Exception {
        String key = "helloKey";
        String value = "arrivederci";
        StoreValueRequest testMessage = new StoreValueRequest() ;
        testMessage.setKey(key);
        testMessage.setValue(value);

        assertEquals(key, testMessage.getKey());
        assertEquals(value, testMessage.getValue());
    }

    @Test
    public void testEncodingAndDecoding() {
        String key = "helloKey";
        String value = "arrivederci";
        StoreValueRequest testMessage = new StoreValueRequest() ;
        testMessage.setKey(key);
        testMessage.setValue(value);

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        StoreValueRequest decodedMessage = new StoreValueRequest(buffer) ;
        assertEquals ( testMessage.getKey(), ((StoreValueRequest)decodedMessage).getKey() );
        assertEquals ( testMessage.getValue(), ((StoreValueRequest)decodedMessage).getValue() );
        assertEquals ( testMessage, decodedMessage ) ;
    }
}
