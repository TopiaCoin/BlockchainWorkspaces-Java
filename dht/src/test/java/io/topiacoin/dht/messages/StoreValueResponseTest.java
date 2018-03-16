package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class StoreValueResponseTest implements MessageTest {

    @Test
    public void testConstructorAndAccessors() throws Exception {
        String key = "helloKey";
        boolean success = true ;

        StoreValueResponse message = new StoreValueResponse() ;
        message.setKey(key);
        message.setSuccess(success);

        assertEquals(key, message.getKey());
        assertEquals(success, message.isSuccess());
    }

    @Test
    public void testEncodingAndDecoding() {
        String key = "helloKey";
        boolean success = true ;

        StoreValueResponse message = new StoreValueResponse() ;
        message.setKey(key);
        message.setSuccess(success);

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        StoreValueResponse decodedMessage = new StoreValueResponse(buffer) ;
        assertEquals ( message.getKey(), ((StoreValueResponse)decodedMessage).getKey() );
        assertEquals ( message.isSuccess(), ((StoreValueResponse)decodedMessage).isSuccess() );
        assertEquals ( message, decodedMessage ) ;
    }

    @Test
    public void testEncodingAndDecodeEmptyMessage() {
        StoreValueResponse message = new StoreValueResponse() ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        StoreValueResponse decodedMessage = new StoreValueResponse(buffer) ;
        assertEquals ( message, decodedMessage ) ;
    }
}
