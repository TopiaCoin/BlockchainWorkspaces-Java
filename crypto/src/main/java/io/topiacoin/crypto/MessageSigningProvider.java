package io.topiacoin.crypto;

import java.nio.ByteBuffer;
import java.security.KeyPair;

public interface MessageSigningProvider {

    byte[] sign(byte[] bufferToSign, KeyPair keyPair);

    byte[] sign(ByteBuffer bufferToSign, KeyPair keyPair);

    boolean verify(byte[] bufferToVerify, KeyPair keyPair, byte[] signature);

    boolean verify(ByteBuffer bufferToVerify, KeyPair keyPair, byte[] signature);

}
