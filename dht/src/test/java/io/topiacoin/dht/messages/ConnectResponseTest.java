package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class ConnectResponseTest {

    @Test
    public void testEncodeDecode() throws Exception {

        // Create and Setup the Message for Testing
        ConnectResponse message = new ConnectResponse();

        // Encode the message
        ByteBuffer buffer = ByteBuffer.allocate(65536);
        message.encodeMessage(buffer);
        buffer.flip();

        // Decode the message
        ConnectResponse decodedMessage = new ConnectResponse(buffer);

        // Verify that the messages match
        assertEquals ( message, decodedMessage);
    }
}
