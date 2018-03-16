package io.topiacoin.dht.messages;

import io.topiacoin.core.util.StringUtilities;
import io.topiacoin.dht.intf.Message;

import java.nio.ByteBuffer;

public class FetchValueRequest implements Message {

    public static final byte TYPE = (byte)0x02;

    private String key ;

    public FetchValueRequest() {
    }

    public FetchValueRequest(ByteBuffer buffer) {
        this.decodeMessage(buffer);
    }

    public byte getType() {
        return TYPE;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void encodeMessage(ByteBuffer buffer) {
        byte[] keyBytes = StringUtilities.getStringBytesOrEmptyArray(this.key);
        int keyLength = keyBytes.length;

        buffer.putInt(keyLength) ;
        buffer.put(keyBytes);
    }

    public void decodeMessage(ByteBuffer buffer) {
        byte[] keyBytes ;
        int keyLength ;

        keyLength = buffer.getInt();
        if ( keyLength > 0 ) {
            keyBytes = new byte[keyLength];
            buffer.get(keyBytes);

            this.key = new String(keyBytes);
        } else {
            this.key = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FetchValueRequest that = (FetchValueRequest) o;

        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "FetchValueRequest{" +
                "key='" + key + '\'' +
                '}';
    }
}
