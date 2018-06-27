package io.topiacoin.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class MessageTest {

    @Test
    public void testDefaultConstructor() throws Exception {
        Message message = new Message();

        assertNull(message.getAuthorID());
        assertNull(message.getDigitalSignature());
        assertNull(message.getMimeType());
        assertNull(message.getText());

        assertEquals(0, message.getSeq());
        assertEquals(0, message.getTimestamp());
    }

    @Test
    public void testConstructor() throws Exception {

        String authorID = "JannerWingfeather";
        long entityID = 45678L;
        long guid = 12345L;
        long seq = 12345;
        long timestamp = 1564783953;
        String text = "A great place to live";
        String mimeType = "text/plain";
        byte[] digSig = new byte[128];

        Message message = new Message(authorID, entityID, guid, seq, timestamp, text, mimeType, digSig);

        assertEquals(authorID, message.getAuthorID());
        assertEquals(entityID, message.getEntityID());
        assertEquals(guid, message.getGuid());
        assertEquals(seq, message.getSeq());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(text, message.getText());
        assertEquals(mimeType, message.getMimeType());
        assertEquals(digSig, message.getDigitalSignature());

    }

    @Test
    public void testBasicAccessors() throws Exception {

        String authorID = "JannerWingfeather";
        long entityID = 45678L;
        long guid = 12345L;
        long seq = 12345;
        long timestamp = 1564783953;
        String text = "A great place to live";
        String mimeType = "text/plain";
        byte[] digSig = new byte[128];

        Message message = new Message();

        assertNull(message.getAuthorID());
        message.setAuthorID(authorID);
        assertEquals(authorID, message.getAuthorID());
        message.setAuthorID(null);
        assertNull(message.getAuthorID());

        message.setEntityID(entityID);
        assertEquals(entityID, message.getEntityID());

        message.setGuid(guid);
        assertEquals(guid, message.getGuid());

        assertEquals(0, message.getSeq());
        message.setSeq(seq);
        assertEquals(seq, message.getSeq());
        message.setSeq(0);
        assertEquals(0, message.getSeq());

        assertEquals(0, message.getTimestamp());
        message.setTimestamp(timestamp);
        assertEquals(timestamp, message.getTimestamp());
        message.setTimestamp(0);
        assertEquals(0, message.getTimestamp());

        assertNull(message.getText());
        message.setText(text);
        assertEquals(text, message.getText());
        message.setText(null);
        assertNull(message.getText());

        assertNull(message.getMimeType());
        message.setMimeType(mimeType);
        assertEquals(mimeType, message.getMimeType());
        message.setMimeType(null);
        assertNull(message.getMimeType());

        assertNull(message.getDigitalSignature());
        message.setDigitalSignature(digSig);
        assertEquals(digSig, message.getDigitalSignature());
        message.setDigitalSignature(null);
        assertNull(message.getDigitalSignature());

    }

    @Test
    public void testEqualsAndHashCode() throws Exception {

        String authorID = "JannerWingfeather";
        long entityID = 45678L;
        long guid = 12345L;
        long seq = 12345;
        long timestamp = 1564783953;
        String text = "A great place to live";
        String mimeType = "text/plain";
        byte[] digSig = new byte[128];

        Message message1 = new Message(authorID, entityID, guid, seq, timestamp, text, mimeType, digSig);
        Message message2 = new Message(authorID, entityID, guid, seq, timestamp, text, mimeType, digSig);

        assertEquals(message1, message1);
        assertEquals(message2, message2);
        assertEquals(message1, message2);
        assertEquals(message2, message1);

        assertEquals(message1.hashCode(), message2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {

        Message message1 = new Message();
        Message message2 = new Message();

        assertEquals(message1, message1);
        assertEquals(message2, message2);
        assertEquals(message1, message2);
        assertEquals(message2, message1);

        assertEquals(message1.hashCode(), message2.hashCode());
    }


}
