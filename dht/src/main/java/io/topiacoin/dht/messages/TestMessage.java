package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.network.Node;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class TestMessage implements Message {

    public static final byte TYPE = 0x00;

    private String message;

    public TestMessage(String message) {
        this.message = message;
    }

    public TestMessage(ByteBuffer buffer) {
        this.decodeMessage(buffer);
    }

    public byte getType() {
        return TYPE;
    }

    public void encodeMessage(ByteBuffer buffer) {
        int strLength = 0;
        byte[] strBuffer = null;
        if (this.message != null) {
            strBuffer = this.message.getBytes();
            strLength = strBuffer.length;
        } else {
            strBuffer = new byte[0];
        }

        buffer.putInt(strLength) ;
        buffer.put(strBuffer) ;
    }

    public void decodeMessage(ByteBuffer buffer) {
        int strLength = buffer.getInt();
        if (strLength > buffer.remaining()) {
            throw new IllegalStateException("Message Size is too long");
        }

        byte[] strBuffer = new byte[strLength];
        buffer.get(strBuffer);

        try {
            String message = new String(strBuffer, "UTF-8");
            this.message = message;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("OMG! UTF-8 is no longer supported in JAVA!!");
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestMessage that = (TestMessage) o;

        return message != null ? message.equals(that.message) : that.message == null;
    }

    @Override
    public int hashCode() {
        return message != null ? message.hashCode() : 0;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
