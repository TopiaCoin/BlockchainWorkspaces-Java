package io.topiacoin.crypto;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.Assert.*;

public class CryptoUtilsTest {

    @Test
    public void testGenerateAESKey() throws Exception {
        SecretKey secretKey = null ;

        secretKey = CryptoUtils.generateAESKey() ;

        assertNotNull(secretKey) ;

        secretKey = CryptoUtils.generateAESKey(256);

        assertNotNull(secretKey);

        try {
            secretKey = CryptoUtils.generateAESKey(64) ;
            fail ( "Expected CryptographicException Not Thrown" ) ;
        } catch ( CryptographicException e ) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testGenerateInitializationVector() throws Exception {

    }

    @Test
    public void testEncryptWithPublicKey() throws Exception {

    }

    @Test
    public void testEncryptAndDecryptDataWithSecretKey() throws Exception {
        byte[] keyBytes = new byte[16] ; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES") ;

        String inputString = "Four score and seven years ago" ;
        byte[] inputData = inputString.getBytes();
        byte[] expectedData = new byte[] {
                (byte)0x8a, (byte)0x57, (byte)0x45, (byte)0x95, (byte)0xc8, (byte)0xd2, (byte)0x4f, (byte)0x0f,
                (byte)0xca, (byte)0x18, (byte)0xcc, (byte)0xeb, (byte)0xcd, (byte)0xd3, (byte)0x39, (byte)0x1d,
                (byte)0x1a, (byte)0xb1, (byte)0xc2, (byte)0x0e, (byte)0x92, (byte)0x2a, (byte)0x74, (byte)0xc5,
                (byte)0xd4, (byte)0x68, (byte)0xbd, (byte)0xfb, (byte)0x4f, (byte)0xb5, (byte)0xc7, (byte)0x51
        } ;
        String expectedString = Base64.toBase64String(expectedData) ;

        byte[] encryptedDataBytes = CryptoUtils.encryptWithSecretKey(inputData, keySpec) ;

        System.out.println (Hex.toHexString(encryptedDataBytes) ) ;

        assertArrayEquals ( expectedData, encryptedDataBytes ) ;

        byte[] decryptedDataFromBytes = CryptoUtils.decryptWithSecretKey(encryptedDataBytes, keySpec) ;

        assertArrayEquals(inputData, decryptedDataFromBytes) ;
    }

    @Test
    public void testEncryptAndDecryptDataWithSecretKeyAndIV() throws Exception {
        byte[] keyBytes = new byte[16] ; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES") ;
        byte[] ivBytes = new byte[16] ; // IV is all 0's.
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes) ;

        String inputString = "Four score and seven years ago" ;
        byte[] inputData = inputString.getBytes();
        byte[] expectedData = new byte[] {
                (byte)0x8a, (byte)0x57, (byte)0x45, (byte)0x95, (byte)0xc8, (byte)0xd2, (byte)0x4f, (byte)0x0f,
                (byte)0xca, (byte)0x18, (byte)0xcc, (byte)0xeb, (byte)0xcd, (byte)0xd3, (byte)0x39, (byte)0x1d,
                (byte)0x62, (byte)0xf2, (byte)0x1f, (byte)0xab, (byte)0x91, (byte)0xa0, (byte)0xce, (byte)0x6b,
                (byte)0x94, (byte)0xef, (byte)0x45, (byte)0x09, (byte)0x48, (byte)0x38, (byte)0x3a, (byte)0xf0
        } ;
        String expectedString = Base64.toBase64String(expectedData) ;

        byte[] encryptedDataBytes = CryptoUtils.encryptWithSecretKey(inputData, keySpec, ivParameterSpec) ;

        System.out.println (Hex.toHexString(encryptedDataBytes) ) ;

        assertArrayEquals ( expectedData, encryptedDataBytes ) ;

        byte[] decryptedDataFromBytes = CryptoUtils.decryptWithSecretKey(encryptedDataBytes, keySpec, ivParameterSpec) ;

        assertArrayEquals(inputData, decryptedDataFromBytes) ;
    }

    @Test
    public void testEncryptAndDecryptStringWithSecretKey() throws Exception {
        byte[] keyBytes = new byte[16] ; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES") ;

        String inputString = "Four score and seven years ago" ;

        // Encrypt the string
        String encryptedStringString = CryptoUtils.encryptStringWithSecretKey(inputString, keySpec) ;

        System.out.println ( encryptedStringString);

        // Decrypt the String
        String decryptedDataFromString = CryptoUtils.decryptStringWithSecretKey(encryptedStringString, keySpec) ;

        // Verify the Decrypted String matches the Original one.
        assertEquals(inputString, decryptedDataFromString) ;

    }

    @Test
    public void testDecryptStringWithSecretKeyWhenStringIsNotEncrypted() throws Exception {
        byte[] keyBytes = new byte[16] ; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES") ;

        String inputString = "Four score and seven years ago" ;

        // Decrypt the String
        String decryptedDataFromString = CryptoUtils.decryptStringWithSecretKey(inputString, keySpec) ;

        // Verify the Decrypted String matches the Original one.
        assertEquals(inputString, decryptedDataFromString) ;

    }

    @Test
    public void testEncryptAndDecryptStringWithSecretKeyWhenSecretKeyIsNull() throws Exception {
        SecretKeySpec keySpec = null ;

        String inputString = "Four score and seven years ago" ;
        String encInputString = "ENC:Four score and seven years ago" ;

        // Encrypt the string
        String encryptedStringString = CryptoUtils.encryptStringWithSecretKey(inputString, keySpec);

        assertEquals ( inputString, encryptedStringString) ;

        // Decrypt the String
        String decryptedDataFromString = CryptoUtils.decryptStringWithSecretKey(encInputString, keySpec);

        assertNull ( decryptedDataFromString ) ;
    }

    @Test
    public void testEncryptAndDecryptWithPublicKeyWithECKeyPair() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair() ;

        String inputString = "It was a dark and stormy night!";
        byte[] inputData = inputString.getBytes();

        // Encrypt with the Public Key
        byte[] encryptedDataBytes = CryptoUtils.encryptWithPublicKey(inputData, keyPair.getPublic()) ;
        byte[] encryptedStringBytes = CryptoUtils.encryptWithPublicKey(inputString, keyPair.getPublic()) ;
        String encryptedDataString = CryptoUtils.encryptWithPublicKeyToString(inputData, keyPair.getPublic()) ;
        String encryptedStringString = CryptoUtils.encryptWithPublicKeyToString(inputString, keyPair.getPublic()) ;

        System.out.println ( encryptedStringString);

        // Decrypt with the Private Key
        byte[] decryptDataBytes = CryptoUtils.decryptWithPrivateKey(encryptedDataBytes, keyPair.getPrivate() ) ;
        byte[] decryptStringBytes = CryptoUtils.decryptWithPrivateKey(encryptedStringBytes, keyPair.getPrivate() ) ;
        byte[] decryptDataString = CryptoUtils.decryptWithPrivateKey(encryptedDataString, keyPair.getPrivate() ) ;
        byte[] decryptStringString = CryptoUtils.decryptWithPrivateKey(encryptedStringString, keyPair.getPrivate() ) ;

        // Verify the Results Match
        assertArrayEquals(inputData, decryptDataBytes);
        assertArrayEquals(inputData, decryptStringBytes);
        assertArrayEquals(inputData, decryptDataString);
        assertArrayEquals(inputData, decryptStringString);
    }

    @Test
    public void testEncryptAndDecryptwithPublicKeyWithRSAKeyPair() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair() ;

        String inputString = "It was a dark and stormy night!";
        byte[] inputData = inputString.getBytes();

        // Encrypt with the Public Key
        byte[] encryptedDataBytes = CryptoUtils.encryptWithPublicKey(inputData, keyPair.getPublic()) ;
        byte[] encryptedStringBytes = CryptoUtils.encryptWithPublicKey(inputString, keyPair.getPublic()) ;
        String encryptedDataString = CryptoUtils.encryptWithPublicKeyToString(inputData, keyPair.getPublic()) ;
        String encryptedStringString = CryptoUtils.encryptWithPublicKeyToString(inputString, keyPair.getPublic()) ;

        System.out.println ( encryptedStringString);

        // Decrypt with the Private Key
        byte[] decryptDataBytes = CryptoUtils.decryptWithPrivateKey(encryptedDataBytes, keyPair.getPrivate() ) ;
        byte[] decryptStringBytes = CryptoUtils.decryptWithPrivateKey(encryptedStringBytes, keyPair.getPrivate() ) ;
        byte[] decryptDataString = CryptoUtils.decryptWithPrivateKey(encryptedDataString, keyPair.getPrivate() ) ;
        byte[] decryptStringString = CryptoUtils.decryptWithPrivateKey(encryptedStringString, keyPair.getPrivate() ) ;

        // Verify the Results Match
        assertArrayEquals(inputData, decryptDataBytes);
        assertArrayEquals(inputData, decryptStringBytes);
        assertArrayEquals(inputData, decryptDataString);
        assertArrayEquals(inputData, decryptStringString);
    }
}
