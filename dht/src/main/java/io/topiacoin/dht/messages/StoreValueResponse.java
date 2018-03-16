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
        buffer.get(keyBytes);

        this.success = (successByte > 0 ) ;
        this.key = (keyLength > 0 ? new String(keyBytes) : null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StoreValueResponse response = (StoreValueResponse) o;

        if (success != response.success) return false;
        return key != null ? key.equals(response.key) : response.key == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (success ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StoreValueResponse{" +
                "key='" + key + '\'' +
                ", success=" + success +
                '}';
    }
}
