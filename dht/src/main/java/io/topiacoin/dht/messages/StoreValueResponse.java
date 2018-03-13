package io.topiacoin.dht.messages;

import io.topiacoin.core.util.StringUtilities;
import io.topiacoin.dht.intf.Message;

import java.nio.ByteBuffer;

public class StoreValueResponse implements Message {

    public static final byte TYPE = (byte)0x85;

    private String key ;
    private boolean success ;

    public StoreValueResponse() {
    }

    public StoreValueResponse(ByteBuffer buffer) {
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void encodeMessage(ByteBuffer buffer) {
        byte[] keyBytes = StringUtilities.getStringBytesOrEmptyArray(this.key);
        int keyLength = keyBytes.length;

        buffer.put(this.success ? (byte)0x01 : (byte)0x00) ;
        buffer.putInt(keyLength) ;
        buffer.put(keyBytes);
    }

    public void decodeMessage(ByteBuffer buffer) {

        byte successByte = buffer.get() ;
        int keyLength = buffer.getInt();
        byte[] keyBytes = new byte[keyLength] ;
        buffer.get(keyLength);

        this.success = (successByte > 0 ) ;
        this.key = new String(keyBytes);
    }
}
