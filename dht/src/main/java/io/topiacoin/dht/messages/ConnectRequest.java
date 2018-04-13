package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;

import java.nio.ByteBuffer;

public class ConnectRequest implements Message {

    public static final byte TYPE = (byte)0x01;

    public byte getType() {
        return TYPE;
    }

    public ConnectRequest() {
    }

    public ConnectRequest(ByteBuffer buffer) {
        decodeMessage(buffer);
    }

    public void encodeMessage(ByteBuffer buffer) {

    }

    public void decodeMessage(ByteBuffer buffer) {

    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof ConnectRequest) ) return false ;

        ConnectRequest that = (ConnectRequest)obj;

        return this.getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return getType();
    }
}
