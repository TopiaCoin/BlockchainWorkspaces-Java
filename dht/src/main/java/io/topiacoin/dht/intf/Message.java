package io.topiacoin.dht.intf;

import io.topiacoin.dht.network.Node;

import java.nio.ByteBuffer;
import java.security.KeyPair;

public interface Message {
    byte getType();
    void encodeMessage(ByteBuffer buffer) ;
    void decodeMessage(ByteBuffer buffer) ;
}
