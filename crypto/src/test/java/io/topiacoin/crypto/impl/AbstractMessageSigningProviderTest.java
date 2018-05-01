package io.topiacoin.crypto.impl;

import io.topiacoin.crypto.impl.MessageSigningProvider;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.SecureRandom;

import static org.junit.Assert.*;

public abstract class AbstractMessageSigningProviderTest {

    protected abstract MessageSigningProvider getMessageSigner();
    protected abstract KeyPair getKeyPair() throws Exception;

    @Test
    public void testSigningAndVerifyingByteArray() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        KeyPair keyPair = getKeyPair() ;

        MessageSigningProvider messageSigner = getMessageSigner();

        byte[] signature = messageSigner.sign(dataToSign, keyPair) ;

        assertNotNull ( signature ) ;

        boolean verified = messageSigner.verify(dataToSign, keyPair.getPublic(), signature) ;

        assertTrue ( verified) ;
    }

    @Test
    public void testSigningAndVerifyingByteBuffer() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        ByteBuffer bufferToSign = ByteBuffer.wrap(dataToSign) ;
        bufferToSign.mark();

        KeyPair keyPair = getKeyPair() ;

        MessageSigningProvider messageSigner = getMessageSigner();

        byte[] signature = messageSigner.sign(bufferToSign, keyPair) ;

        assertNotNull ( signature ) ;

        bufferToSign.reset();
        boolean verified = messageSigner.verify(bufferToSign, keyPair.getPublic(), signature) ;

        assertTrue ( verified) ;
    }
}
