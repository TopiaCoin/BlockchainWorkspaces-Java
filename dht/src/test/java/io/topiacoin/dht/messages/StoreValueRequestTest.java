package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class StoreValueRequestTest {

    @Test
    public void testEncodingAndDecoding() {
        StoreValueRequest testMessage = new StoreValueRequest("helloKey", "arrivederci") ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        StoreValueRequest decodedMessage = new StoreValueRequest(buffer) ;
        assertEquals ( testMessage.getKey(), ((StoreValueRequest)decodedMessage).getKey() );
        assertEquals ( testMessage.getValue(), ((StoreValueRequest)decodedMessage).getValue() );
        assertEquals ( testMessage, decodedMessage ) ;
    }
}
