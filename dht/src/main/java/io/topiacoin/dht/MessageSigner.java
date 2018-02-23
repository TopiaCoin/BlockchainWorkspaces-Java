package io.topiacoin.dht;

import java.nio.ByteBuffer;
import java.security.KeyPair;

public interface MessageSigner {

    byte[] sign(ByteBuffer bufferToSign, KeyPair keyPair) ;
    boolean verify (ByteBuffer bufferToVerify, KeyPair keyPair, byte[] signature);

}
