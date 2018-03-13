package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;

import java.nio.ByteBuffer;

public class ConnectRequest implements Message {

    public static final byte TYPE = (byte)0x01;

    public byte getType() {
        return TYPE;
    }

    public void encodeMessage(ByteBuffer buffer) {

    }

    public void decodeMessage(ByteBuffer buffer) {

    }
}
