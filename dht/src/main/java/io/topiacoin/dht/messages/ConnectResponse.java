package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;

import java.nio.ByteBuffer;

public class ConnectResponse implements Message {

    public static final byte TYPE = (byte)0x81;

    public byte getType() {
        return TYPE;
    }

    public void encodeMessage(ByteBuffer buffer) {

    }

    public void decodeMessage(ByteBuffer buffer) {

    }
}
