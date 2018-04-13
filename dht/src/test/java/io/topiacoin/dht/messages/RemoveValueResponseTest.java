package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class RemoveValueResponseTest implements MessageTest {

    @Test
    public void testConstructorAndAccessors() throws Exception {
        String key = "helloKey";
        boolean success = true ;

        RemoveValueResponse message = new RemoveValueResponse() ;
        message.setKey(key);
        message.setSuccess(success);

        assertEquals(key, message.getKey());
        assertEquals(success, message.isSuccess());
    }

    @Test
    public void testEncodingAndDecoding() {
        String key = "helloKey";
        boolean success = true ;

        RemoveValueResponse message = new RemoveValueResponse() ;
        message.setKey(key);
        message.setSuccess(success);

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        RemoveValueResponse decodedMessage = new RemoveValueResponse(buffer) ;
        assertEquals ( message.getKey(), decodedMessage.getKey() );
        assertEquals ( message.isSuccess(), decodedMessage.isSuccess() );
        assertEquals ( message, decodedMessage ) ;
    }

    @Test
    public void testEncodingAndDecodingEmptyMessage() {
        RemoveValueResponse message = new RemoveValueResponse() ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        message.encodeMessage(buffer);
        buffer.flip();

        RemoveValueResponse decodedMessage = new RemoveValueResponse(buffer) ;
        assertEquals ( message, decodedMessage ) ;
    }
}
