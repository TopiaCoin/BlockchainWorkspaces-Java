package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class FetchValueRequestTest {

    @Test
    public void testEncodeAndDecode() throws  Exception {

        String key = "TheMeaningOfLife";

        FetchValueRequest message = new FetchValueRequest() ;
        message.setKey(key);

        ByteBuffer buffer = ByteBuffer.allocate(65536);

        message.encodeMessage(buffer);
        buffer.flip();

        FetchValueRequest decodedMessage = new FetchValueRequest(buffer);

        assertEquals ( message, decodedMessage);
        assertEquals ( message.getKey(), decodedMessage.getKey());
    }

    @Test
    public void testEncodeAndDecodeEmptyMessage() throws  Exception {

        FetchValueRequest message = new FetchValueRequest() ;

        ByteBuffer buffer = ByteBuffer.allocate(65536);

        message.encodeMessage(buffer);
        buffer.flip();

        FetchValueRequest decodedMessage = new FetchValueRequest(buffer);

        assertEquals ( message, decodedMessage);
    }
}
