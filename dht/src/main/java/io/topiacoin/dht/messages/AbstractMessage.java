package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.network.Node;

import java.nio.ByteBuffer;

public abstract class AbstractMessage implements Message {

    public void encodeMessage(ByteBuffer buffer) {

    }

    public void decodeMessage(ByteBuffer buffer) {

    }
}
