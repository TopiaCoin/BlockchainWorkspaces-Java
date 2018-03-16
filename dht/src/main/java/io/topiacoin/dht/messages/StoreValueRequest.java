package io.topiacoin.dht.messages;

import io.topiacoin.core.util.StringUtilities;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.network.Node;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

public class StoreValueRequest implements Message {

    public static final byte TYPE = (byte)0x05;

    private String key;
    private String value;

    public StoreValueRequest() {
    }

    public StoreValueRequest(ByteBuffer buffer) {
        decodeMessage(buffer);
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void encodeMessage(ByteBuffer buffer) {
        byte[] keyBytes = StringUtilities.getStringBytesOrEmptyArray(this.key);
        int keyLength = keyBytes.length;
        byte[] valueBytes = StringUtilities.getStringBytesOrEmptyArray(this.value);
        int valueLength = valueBytes.length;

        buffer.putInt(keyLength);
        buffer.put(keyBytes);
        buffer.putInt(valueLength);
        buffer.put(valueBytes);
    }

    public void decodeMessage(ByteBuffer buffer) {
        int keyLength = 0;
        int valueLength = 0;
        byte[] keyBytes = null;
        byte[] valueBytes = null;

        keyLength = buffer.getInt();
        keyBytes = new byte[keyLength];
        buffer.get(keyBytes);

        valueLength = buffer.getInt();
        valueBytes = new byte[valueLength];
        buffer.get(valueBytes);

        try {
            String keyString = new String(keyBytes, "UTF-8");
            String valueString = new String(valueBytes, "UTF-8");
            this.key = keyString;
            this.value = valueString;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("OMG! Java no longer supports UTF-8!");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StoreValueRequest that = (StoreValueRequest) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StoreValueRequest{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
