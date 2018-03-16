package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;

import java.nio.ByteBuffer;

public class ConnectResponse implements Message {

    public static final byte TYPE = (byte)0x81;

    public ConnectResponse() {
    }

    public ConnectResponse(ByteBuffer buffer) {
        decodeMessage(buffer);
    }

    public byte getType() {
        return TYPE;
    }

    public void encodeMessage(ByteBuffer buffer) {

    }

    public void decodeMessage(ByteBuffer buffer) {

    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof ConnectResponse) ) return false ;

        ConnectResponse that = (ConnectResponse)obj;

        return this.getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return getType();
    }
}
