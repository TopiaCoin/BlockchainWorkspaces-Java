package io.topiacoin.crypto.impl;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PublicKey;

public interface MessageSigningProvider {

    byte[] sign(byte[] bufferToSign, KeyPair keyPair);

    byte[] sign(ByteBuffer bufferToSign, KeyPair keyPair);

    boolean verify(byte[] bufferToVerify, PublicKey publicKey, byte[] signature);

    boolean verify(ByteBuffer bufferToVerify, PublicKey publicKey, byte[] signature);

}
