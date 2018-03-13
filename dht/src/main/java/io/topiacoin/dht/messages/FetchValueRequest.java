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
        keyBytes = new byte[keyLength] ;
        buffer.get(keyBytes);

        this.key = new String(keyBytes);
    }
}
