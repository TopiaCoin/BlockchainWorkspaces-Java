package io.topiacoin.dht.intf;

import java.nio.ByteBuffer;

public interface Message {
    byte getType();
    void encodeMessage(ByteBuffer buffer) ;
    void decodeMessage(ByteBuffer buffer) ;
}
