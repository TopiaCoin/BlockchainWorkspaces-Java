package io.topiacoin.crypto;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

import static org.junit.Assert.*;

public class MessageSignerTest {

    @Test
    public void testSigningAndVerifyingByteArrayWithRSAKey() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA") ;
        KeyPair keyPair = kpg.generateKeyPair();

        byte[] signature = MessageSigner.sign(dataToSign, keyPair) ;

        assertNotNull ( signature ) ;

        boolean verified = MessageSigner.verify(dataToSign, keyPair, signature) ;

        assertTrue ( verified) ;
    }

    @Test
    public void testSigningAndVerifyingByteBufferWithRSAKey() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        ByteBuffer bufferToSign = ByteBuffer.wrap(dataToSign) ;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA") ;
        KeyPair keyPair = kpg.generateKeyPair();

        byte[] signature = MessageSigner.sign(bufferToSign, keyPair) ;

        assertNotNull ( signature ) ;

        boolean verified = MessageSigner.verify(bufferToSign, keyPair, signature) ;

        assertTrue ( verified) ;
    }

    @Test
    public void testSigningAndVerifyingByteArrayWithECKey() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC") ;
        KeyPair keyPair = kpg.generateKeyPair();

        byte[] signature = MessageSigner.sign(dataToSign, keyPair) ;

        assertNotNull ( signature ) ;

        boolean verified = MessageSigner.verify(dataToSign, keyPair, signature) ;

        assertTrue ( verified) ;
    }

    @Test
    public void testSigningAndVerifyingByteBufferWithECKey() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        ByteBuffer bufferToSign = ByteBuffer.wrap(dataToSign) ;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC") ;
        KeyPair keyPair = kpg.generateKeyPair();

        byte[] signature = MessageSigner.sign(bufferToSign, keyPair) ;

        assertNotNull ( signature ) ;

        boolean verified = MessageSigner.verify(bufferToSign, keyPair, signature) ;

        assertTrue ( verified) ;
    }

    @Test
    public void testSigningAndVerifyingByteArrayWithDSAKey() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA") ;
        KeyPair keyPair = kpg.generateKeyPair();

        byte[] signature = MessageSigner.sign(dataToSign, keyPair) ;

        assertNotNull ( signature ) ;

        boolean verified = MessageSigner.verify(dataToSign, keyPair, signature) ;

        assertTrue ( verified) ;
    }

    @Test
    public void testSigningAndVerifyingByteBufferWithDSAKey() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        ByteBuffer bufferToSign = ByteBuffer.wrap(dataToSign) ;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA") ;
        KeyPair keyPair = kpg.generateKeyPair();

        byte[] signature = MessageSigner.sign(bufferToSign, keyPair) ;

        assertNotNull ( signature ) ;

        boolean verified = MessageSigner.verify(bufferToSign, keyPair, signature) ;

        assertTrue ( verified) ;
    }

    @Test
    public void testSigningAndVerifyingByteArrayWithUnrecognizedKey() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH") ;
        KeyPair keyPair = kpg.generateKeyPair();

        byte[] signature = null;

        try {
            signature = MessageSigner.sign(dataToSign, keyPair);
            fail ( "Expected CryptographicException Not Thrown") ;
        } catch ( CryptographicException e ) {
            // NOOP - Expected Exception
        }

        try {
            boolean verified = MessageSigner.verify(dataToSign, keyPair, signature);
            fail ( "Expected CryptographicException Not Thrown") ;
        } catch ( CryptographicException e ) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testSigningAndVerifyingByteBufferWithUnrecognizedKey() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        ByteBuffer bufferToSign = ByteBuffer.wrap(dataToSign) ;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH") ;
        KeyPair keyPair = kpg.generateKeyPair();

        byte[] signature = null ;

        try {
            signature = MessageSigner.sign(bufferToSign, keyPair);
            fail ( "Expected CryptographicException Not Thrown") ;
        } catch ( CryptographicException e ) {
            // NOOP - Expected Exception
        }

        try {
            boolean verified = MessageSigner.verify(bufferToSign, keyPair, signature);
            fail ( "Expected CryptographicException Not Thrown") ;
        } catch ( CryptographicException e ) {
            // NOOP - Expected Exception
        }
    }

}
