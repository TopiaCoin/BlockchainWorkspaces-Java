package io.topiacoin.dht.messages;

import io.topiacoin.core.util.StringUtilities;
import io.topiacoin.dht.intf.Message;
import org.apache.commons.codec.binary.StringUtils;
import sun.jvm.hotspot.utilities.CStringUtilities;

import java.nio.ByteBuffer;

public class FetchValueResponse implements Message {

    public static final byte TYPE = (byte)0x82;

    private String key;
    private String value;

    public FetchValueResponse() {
    }

    public FetchValueResponse(ByteBuffer buffer) {
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void encodeMessage(ByteBuffer buffer) {
        byte[] keyBytes = StringUtilities.getStringBytesOrEmptyArray(this.key);
        int keyLength = keyBytes.length ;
        byte[] valueBytes = StringUtilities.getStringBytesOrEmptyArray(this.value);
        int valueLength = valueBytes.length;

        buffer.putInt(keyLength);
        buffer.put(keyBytes);
        buffer.putInt(valueLength);
        buffer.put(valueBytes);
    }

    public void decodeMessage(ByteBuffer buffer) {
        byte[] keyBytes;
        int keyLength;
        byte[] valueBytes ;
        int valueLength ;

        keyLength = buffer.getInt();
        keyBytes = new byte[keyLength];
        buffer.get(keyBytes);
        valueLength = buffer.getInt();
        valueBytes = new byte[valueLength];
        buffer.get(valueBytes);

        this.key = new String(keyBytes);
        this.value = new String(valueBytes);
    }
}
