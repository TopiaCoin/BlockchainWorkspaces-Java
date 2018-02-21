package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class TestMessageTest {

    @Test
    public void testEncodingAndDecoding() {
        TestMessage testMessage = new TestMessage("arrivederci") ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        TestMessage decodedMessage = new TestMessage(buffer) ;
        assertEquals ( testMessage.getMessage(), ((TestMessage)decodedMessage).getMessage() );
    }
}
