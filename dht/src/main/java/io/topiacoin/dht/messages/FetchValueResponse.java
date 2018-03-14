package io.topiacoin.dht.messages;

import io.topiacoin.core.util.StringUtilities;
import io.topiacoin.dht.intf.Message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FetchValueResponse implements Message {

    public static final byte TYPE = (byte)0x82;

    private String key;
    private Collection<String> values;

    public FetchValueResponse() {
        this.values = new ArrayList<String>();
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

    public Collection<String> getValues() {
        return values;
    }

    public void setValues(Collection<String> values) {
        this.values = values;
    }

    public void encodeMessage(ByteBuffer buffer) {
        byte[] keyBytes = StringUtilities.getStringBytesOrEmptyArray(this.key);
        int keyLength = keyBytes.length ;
        int valueCount = this.values.size();

        buffer.putInt(keyLength);
        buffer.put(keyBytes);
        buffer.putInt(valueCount);

        for ( String value : this.values ) {
            byte[] valueBytes = StringUtilities.getStringBytesOrEmptyArray(value);
            int valueLength = valueBytes.length;

            buffer.putInt(valueLength);
            buffer.put(valueBytes);
        }
    }

    public void decodeMessage(ByteBuffer buffer) {
        byte[] keyBytes;
        int keyLength;
        int valueCount ;
        byte[] valueBytes ;
        int valueLength ;

        keyLength = buffer.getInt();

        if ( keyLength > 0 ) {
            keyBytes = new byte[keyLength];
            buffer.get(keyBytes);
            this.key = new String(keyBytes);
        } else {
            this.key = null ;
        }

        valueCount = buffer.getInt();
        this.values = new ArrayList<String>(valueCount);

        for ( int i = 0 ; i < valueCount ; i++) {
            valueLength = buffer.getInt();
            valueBytes = new byte[valueLength];
            buffer.get(valueBytes);
            this.values.add(new String(valueBytes));
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FetchValueResponse response = (FetchValueResponse) o;

        if (key != null ? !key.equals(response.key) : response.key != null) return false;
        return values != null ? values.equals(response.values) : response.values == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (values != null ? values.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FetchValueResponse{" +
                "key='" + key + '\'' +
                ", values=" + values +
                '}';
    }
}
