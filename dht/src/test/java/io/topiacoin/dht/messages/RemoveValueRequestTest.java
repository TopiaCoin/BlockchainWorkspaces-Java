package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class RemoveValueRequestTest implements MessageTest {

    @Test
    public void testConstructorAndAccessors() throws Exception {
        String key = "helloKey";
        String value = "arrivederci";
        RemoveValueRequest testMessage = new RemoveValueRequest() ;
        testMessage.setKey(key);
        testMessage.setValue(value);

        assertEquals(key, testMessage.getKey());
        assertEquals(value, testMessage.getValue());
    }

    @Test
    public void testEncodingAndDecoding() {
        String key = "helloKey";
        String value = "arrivederci";
        RemoveValueRequest testMessage = new RemoveValueRequest() ;
        testMessage.setKey(key);
        testMessage.setValue(value);

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        RemoveValueRequest decodedMessage = new RemoveValueRequest(buffer) ;
        assertEquals ( testMessage.getKey(), decodedMessage.getKey() );
        assertEquals ( testMessage.getValue(), decodedMessage.getValue() );
        assertEquals ( testMessage, decodedMessage ) ;
    }
}
