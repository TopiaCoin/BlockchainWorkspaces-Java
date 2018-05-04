package io.topiacoin.crypto;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

public class CryptoUtilsTest {

    @Test
    public void testGenerateAESKey() throws Exception {
        SecretKey secretKey = null;

        secretKey = CryptoUtils.generateAESKey();

        assertNotNull(secretKey);

        secretKey = CryptoUtils.generateAESKey(256);

        assertNotNull(secretKey);

        try {
            secretKey = CryptoUtils.generateAESKey(64);
            fail("Expected CryptographicException Not Thrown");
        } catch (CryptographicException e) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testGenerateInitializationVector() throws Exception {

        IvParameterSpec iv = CryptoUtils.generateIV("AES");
        ;

        assertNotNull(iv);
    }

    @Test
    public void testGenerateKeyPair() throws Exception {

        KeyPair keyPair = null;

        keyPair = CryptoUtils.generateKeyPair("EC");

        assertNotNull(keyPair);
        assertEquals("EC", keyPair.getPublic().getAlgorithm());
        assertEquals("EC", keyPair.getPrivate().getAlgorithm());

        keyPair = CryptoUtils.generateKeyPair("RSA");

        assertNotNull(keyPair);
        assertEquals("RSA", keyPair.getPublic().getAlgorithm());
        assertEquals("RSA", keyPair.getPrivate().getAlgorithm());

        keyPair = CryptoUtils.generateKeyPair("DSA");

        assertNotNull(keyPair);
        assertEquals("DSA", keyPair.getPublic().getAlgorithm());
        assertEquals("DSA", keyPair.getPrivate().getAlgorithm());

        keyPair = CryptoUtils.generateKeyPair("DH");

        assertNotNull(keyPair);
        assertEquals("DH", keyPair.getPublic().getAlgorithm());
        assertEquals("DH", keyPair.getPrivate().getAlgorithm());
    }

    @Test
    public void testGenerateRSAKeyPair() throws Exception {

        KeyPair keyPair = null;

        keyPair = CryptoUtils.generateRSAKeyPair();

        assertNotNull(keyPair);
        assertEquals("RSA", keyPair.getPublic().getAlgorithm());
        assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
    }

    @Test
    public void testGenerateDSAKeyPair() throws Exception {

        KeyPair keyPair = null;

        keyPair = CryptoUtils.generateDSAKeyPair();

        assertNotNull(keyPair);
        assertEquals("DSA", keyPair.getPublic().getAlgorithm());
        assertEquals("DSA", keyPair.getPrivate().getAlgorithm());
    }

    @Test
    public void testGenerateECKeyPair() throws Exception {

        KeyPair keyPair = null;

        keyPair = CryptoUtils.generateECKeyPair();

        assertNotNull(keyPair);
        assertEquals("EC", keyPair.getPublic().getAlgorithm());
        assertEquals("EC", keyPair.getPrivate().getAlgorithm());
        byte[] pubKeyBytes = keyPair.getPublic().getEncoded();
        StringBuilder sb = new StringBuilder();
        int appendCt = 0;
        for (byte b : pubKeyBytes) {
            sb.append("(byte)0x").append(String.format("%02x", b)).append(", ");
            appendCt++;
            if (appendCt == 8) {
                sb.append(System.getProperty("line.separator"));
                appendCt = 0;
            }
        }
        System.out.println("Generated Public Key:");
        System.out.println(sb.toString());
    }

    @Test
    public void getPublicKeyFromEncodedBytes() {
        byte[] validECPublicKey = new byte[]{
                (byte) 0x30, (byte) 0x59, (byte) 0x30, (byte) 0x13, (byte) 0x06, (byte) 0x07, (byte) 0x2a, (byte) 0x86,
                (byte) 0x48, (byte) 0xce, (byte) 0x3d, (byte) 0x02, (byte) 0x01, (byte) 0x06, (byte) 0x08, (byte) 0x2a,
                (byte) 0x86, (byte) 0x48, (byte) 0xce, (byte) 0x3d, (byte) 0x03, (byte) 0x01, (byte) 0x07, (byte) 0x03,
                (byte) 0x42, (byte) 0x00, (byte) 0x04, (byte) 0x83, (byte) 0xc1, (byte) 0xfd, (byte) 0xbe, (byte) 0x05,
                (byte) 0xd5, (byte) 0xa2, (byte) 0xa5, (byte) 0x0c, (byte) 0xb7, (byte) 0x54, (byte) 0x38, (byte) 0xbc,
                (byte) 0x17, (byte) 0xc4, (byte) 0x25, (byte) 0x81, (byte) 0xb2, (byte) 0xd6, (byte) 0x4d, (byte) 0xf1,
                (byte) 0x2e, (byte) 0x90, (byte) 0xf2, (byte) 0xfa, (byte) 0x90, (byte) 0xbe, (byte) 0x55, (byte) 0x2c,
                (byte) 0x6c, (byte) 0x0e, (byte) 0x23, (byte) 0x1d, (byte) 0x39, (byte) 0x83, (byte) 0xe0, (byte) 0xf6,
                (byte) 0x93, (byte) 0xf7, (byte) 0x69, (byte) 0x4c, (byte) 0x8c, (byte) 0x5d, (byte) 0xe7, (byte) 0x82,
                (byte) 0x27, (byte) 0xc4, (byte) 0x16, (byte) 0xfe, (byte) 0xeb, (byte) 0xd5, (byte) 0xd4, (byte) 0xda,
                (byte) 0x93, (byte) 0xc0, (byte) 0x07, (byte) 0x88, (byte) 0x91, (byte) 0xb0, (byte) 0x31, (byte) 0x12,
                (byte) 0xcc, (byte) 0x19, (byte) 0x29,
        };
        try {
            PublicKey publicKey = CryptoUtils.getPublicKeyFromEncodedBytes("EC", validECPublicKey);
            assertNotNull(publicKey);
            assertEquals("EC", publicKey.getAlgorithm());
            assertTrue(Arrays.equals(validECPublicKey, publicKey.getEncoded()));
        } catch (CryptographicException e) {
            e.printStackTrace();
            fail();
        }

        //Negative
        try {
            CryptoUtils.getPublicKeyFromEncodedBytes("Potato", validECPublicKey);
            fail("Should've thrown an Exception");
        } catch (CryptographicException e) {
            //Good
        }
        try {
            CryptoUtils.getPublicKeyFromEncodedBytes("EC", "Potato".getBytes());
            fail("Should've thrown an Exception");
        } catch (CryptographicException e) {
            //Good
        }
        try {
            CryptoUtils.getPublicKeyFromEncodedBytes(null, validECPublicKey);
            fail("Should've thrown an Exception");
        } catch (CryptographicException e) {
            //Good
        }
        try {
            CryptoUtils.getPublicKeyFromEncodedBytes("EC", null);
            fail("Should've thrown an Exception");
        } catch (CryptographicException e) {
            //Good
        }
    }

    @Test
    public void testGenerateECDHSharedSecret() {
        KeyPair userAKeyPair = null;
        KeyPair userBKeyPair = null;
        try {
            userAKeyPair = CryptoUtils.generateECKeyPair();
            userBKeyPair = CryptoUtils.generateECKeyPair();
            byte[] userASharedSecret = CryptoUtils.generateECDHSharedSecret(userAKeyPair.getPrivate(), userBKeyPair.getPublic());
            byte[] userBSharedSecret = CryptoUtils.generateECDHSharedSecret(userBKeyPair.getPrivate(), userAKeyPair.getPublic());
            assertNotNull(userASharedSecret);
            assertNotNull(userBSharedSecret);
            assertTrue(Arrays.equals(userASharedSecret, userBSharedSecret));
        } catch (CryptographicException e) {
            fail();
        }
        //Negative
        try {
            CryptoUtils.generateECDHSharedSecret(null, userBKeyPair.getPublic());
            fail();
        } catch (CryptographicException e) {
            //Good
        }
        try {
            CryptoUtils.generateECDHSharedSecret(userAKeyPair.getPrivate(), null);
            fail();
        } catch (CryptographicException e) {
            //Good
        }
    }

    @Test
    public void testGenerateDHKeyPair() throws Exception {

        KeyPair keyPair = null;

        keyPair = CryptoUtils.generateDHKeyPair();

        assertNotNull(keyPair);
        assertEquals("DH", keyPair.getPublic().getAlgorithm());
        assertEquals("DH", keyPair.getPrivate().getAlgorithm());
    }


    @Test
    public void testEncryptAndDecryptDataWithSecretKey() throws Exception {
        byte[] keyBytes = new byte[16]; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        String inputString = "Four score and seven years ago";
        byte[] inputData = inputString.getBytes();
        byte[] expectedData = new byte[]{
                (byte) 0x8a, (byte) 0x57, (byte) 0x45, (byte) 0x95, (byte) 0xc8, (byte) 0xd2, (byte) 0x4f, (byte) 0x0f,
                (byte) 0xca, (byte) 0x18, (byte) 0xcc, (byte) 0xeb, (byte) 0xcd, (byte) 0xd3, (byte) 0x39, (byte) 0x1d,
                (byte) 0x1a, (byte) 0xb1, (byte) 0xc2, (byte) 0x0e, (byte) 0x92, (byte) 0x2a, (byte) 0x74, (byte) 0xc5,
                (byte) 0xd4, (byte) 0x68, (byte) 0xbd, (byte) 0xfb, (byte) 0x4f, (byte) 0xb5, (byte) 0xc7, (byte) 0x51
        };
        String expectedString = Base64.toBase64String(expectedData);

        byte[] encryptedDataBytes = CryptoUtils.encryptWithSecretKey(inputData, keySpec);

        System.out.println(Hex.toHexString(encryptedDataBytes));

        assertArrayEquals(expectedData, encryptedDataBytes);

        byte[] decryptedDataFromBytes = CryptoUtils.decryptWithSecretKey(encryptedDataBytes, keySpec);

        assertArrayEquals(inputData, decryptedDataFromBytes);
    }

    @Test
    public void testEncryptAndDecryptDataWithSecretKeyAndIV() throws Exception {
        byte[] keyBytes = new byte[16]; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16]; // IV is all 0's.
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        String inputString = "Four score and seven years ago";
        byte[] inputData = inputString.getBytes();
        byte[] expectedData = new byte[]{
                (byte) 0x8a, (byte) 0x57, (byte) 0x45, (byte) 0x95, (byte) 0xc8, (byte) 0xd2, (byte) 0x4f, (byte) 0x0f,
                (byte) 0xca, (byte) 0x18, (byte) 0xcc, (byte) 0xeb, (byte) 0xcd, (byte) 0xd3, (byte) 0x39, (byte) 0x1d,
                (byte) 0x62, (byte) 0xf2, (byte) 0x1f, (byte) 0xab, (byte) 0x91, (byte) 0xa0, (byte) 0xce, (byte) 0x6b,
                (byte) 0x94, (byte) 0xef, (byte) 0x45, (byte) 0x09, (byte) 0x48, (byte) 0x38, (byte) 0x3a, (byte) 0xf0
        };
        String expectedString = Base64.toBase64String(expectedData);

        byte[] encryptedDataBytes = CryptoUtils.encryptWithSecretKey(inputData, keySpec, ivParameterSpec);

        System.out.println(Hex.toHexString(encryptedDataBytes));

        assertArrayEquals(expectedData, encryptedDataBytes);

        byte[] decryptedDataFromBytes = CryptoUtils.decryptWithSecretKey(encryptedDataBytes, keySpec, ivParameterSpec);

        assertArrayEquals(inputData, decryptedDataFromBytes);
    }

    @Test
    public void testEncryptAndDecryptStreamWithSecretKeyAndIV() throws Exception {
        byte[] keyBytes = new byte[16]; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16]; // IV is all 0's.
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        String inputString = "Four score and seven years ago";
        byte[] inputData = inputString.getBytes();
        byte[] expectedData = new byte[]{
                (byte) 0x8a, (byte) 0x57, (byte) 0x45, (byte) 0x95, (byte) 0xc8, (byte) 0xd2, (byte) 0x4f, (byte) 0x0f,
                (byte) 0xca, (byte) 0x18, (byte) 0xcc, (byte) 0xeb, (byte) 0xcd, (byte) 0xd3, (byte) 0x39, (byte) 0x1d,
                (byte) 0x62, (byte) 0xf2, (byte) 0x1f, (byte) 0xab, (byte) 0x91, (byte) 0xa0, (byte) 0xce, (byte) 0x6b,
                (byte) 0x94, (byte) 0xef, (byte) 0x45, (byte) 0x09, (byte) 0x48, (byte) 0x38, (byte) 0x3a, (byte) 0xf0
        };
        String expectedString = Base64.toBase64String(expectedData);

        ByteArrayInputStream clearBais = new ByteArrayInputStream(inputData);
        ByteArrayOutputStream cipherBaos = new ByteArrayOutputStream();

        CryptoUtils.encryptWithSecretKey(clearBais, cipherBaos, keySpec, ivParameterSpec);

        byte[] encryptedDataBytes = cipherBaos.toByteArray();

        System.out.println(Hex.toHexString(encryptedDataBytes));

        assertArrayEquals(expectedData, encryptedDataBytes);

        ByteArrayInputStream cipherBais = new ByteArrayInputStream(encryptedDataBytes);
        ByteArrayOutputStream clearBaos = new ByteArrayOutputStream();

        CryptoUtils.decryptWithSecretKey(cipherBais, clearBaos, keySpec, ivParameterSpec);

        byte[] decryptedDataFromBytes = clearBaos.toByteArray();

        assertArrayEquals(inputData, decryptedDataFromBytes);
    }

    @Test
    public void testEncryptAndDecryptStringWithSecretKey() throws Exception {
        byte[] keyBytes = new byte[16]; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        String inputString = "Four score and seven years ago";

        // Encrypt the string
        String encryptedStringString = CryptoUtils.encryptStringWithSecretKey(inputString, keySpec);

        System.out.println(encryptedStringString);

        // Decrypt the String
        String decryptedDataFromString = CryptoUtils.decryptStringWithSecretKey(encryptedStringString, keySpec);

        // Verify the Decrypted String matches the Original one.
        assertEquals(inputString, decryptedDataFromString);

    }

    @Test
    public void testDecryptStringWithSecretKeyWhenStringIsNotEncrypted() throws Exception {
        byte[] keyBytes = new byte[16]; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        String inputString = "Four score and seven years ago";

        // Decrypt the String
        String decryptedDataFromString = CryptoUtils.decryptStringWithSecretKey(inputString, keySpec);

        // Verify the Decrypted String matches the Original one.
        assertEquals(inputString, decryptedDataFromString);

    }

    @Test
    public void testEncryptAndDecryptStringWithSecretKeyWhenSecretKeyIsNull() throws Exception {
        SecretKeySpec keySpec = null;

        String inputString = "Four score and seven years ago";
        String encInputString = "ENC:Four score and seven years ago";

        // Encrypt the string
        String encryptedStringString = CryptoUtils.encryptStringWithSecretKey(inputString, keySpec);

        assertEquals(inputString, encryptedStringString);

        // Decrypt the String
        String decryptedDataFromString = CryptoUtils.decryptStringWithSecretKey(encInputString, keySpec);

        assertNull(decryptedDataFromString);
    }

    @Test
    public void testEncryptAndDecryptWithPublicKeyWithECKeyPair() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();

        String inputString = "It was a dark and stormy night!";
        byte[] inputData = inputString.getBytes();

        // Encrypt with the Public Key
        byte[] encryptedDataBytes = CryptoUtils.encryptWithPublicKey(inputData, keyPair.getPublic());
        byte[] encryptedStringBytes = CryptoUtils.encryptWithPublicKey(inputString, keyPair.getPublic());
        String encryptedDataString = CryptoUtils.encryptWithPublicKeyToString(inputData, keyPair.getPublic());
        String encryptedStringString = CryptoUtils.encryptWithPublicKeyToString(inputString, keyPair.getPublic());

        System.out.println(encryptedStringString);

        // Decrypt with the Private Key
        byte[] decryptDataBytes = CryptoUtils.decryptWithPrivateKey(encryptedDataBytes, keyPair.getPrivate());
        byte[] decryptStringBytes = CryptoUtils.decryptWithPrivateKey(encryptedStringBytes, keyPair.getPrivate());
        byte[] decryptDataString = CryptoUtils.decryptWithPrivateKey(encryptedDataString, keyPair.getPrivate());
        byte[] decryptStringString = CryptoUtils.decryptWithPrivateKey(encryptedStringString, keyPair.getPrivate());

        // Verify the Results Match
        assertArrayEquals(inputData, decryptDataBytes);
        assertArrayEquals(inputData, decryptStringBytes);
        assertArrayEquals(inputData, decryptDataString);
        assertArrayEquals(inputData, decryptStringString);
    }

    @Test
    public void testEncryptAndDecryptwithPublicKeyWithRSAKeyPair() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        String inputString = "It was a dark and stormy night!";
        byte[] inputData = inputString.getBytes();

        // Encrypt with the Public Key
        byte[] encryptedDataBytes = CryptoUtils.encryptWithPublicKey(inputData, keyPair.getPublic());
        byte[] encryptedStringBytes = CryptoUtils.encryptWithPublicKey(inputString, keyPair.getPublic());
        String encryptedDataString = CryptoUtils.encryptWithPublicKeyToString(inputData, keyPair.getPublic());
        String encryptedStringString = CryptoUtils.encryptWithPublicKeyToString(inputString, keyPair.getPublic());

        System.out.println(encryptedStringString);

        // Decrypt with the Private Key
        byte[] decryptDataBytes = CryptoUtils.decryptWithPrivateKey(encryptedDataBytes, keyPair.getPrivate());
        byte[] decryptStringBytes = CryptoUtils.decryptWithPrivateKey(encryptedStringBytes, keyPair.getPrivate());
        byte[] decryptDataString = CryptoUtils.decryptWithPrivateKey(encryptedDataString, keyPair.getPrivate());
        byte[] decryptStringString = CryptoUtils.decryptWithPrivateKey(encryptedStringString, keyPair.getPrivate());

        // Verify the Results Match
        assertArrayEquals(inputData, decryptDataBytes);
        assertArrayEquals(inputData, decryptStringBytes);
        assertArrayEquals(inputData, decryptDataString);
        assertArrayEquals(inputData, decryptStringString);
    }

    @Test
    public void testEncryptDecryptStreams() throws Exception {
        byte[] keyBytes = new byte[16]; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] inData = new byte[16384];
        Random random = new Random();
        random.nextBytes(inData);

        System.out.println("inData.length: " + inData.length);
        System.out.println("inData: " + Arrays.toString(inData));

        ByteArrayInputStream encBais = new ByteArrayInputStream(inData);
        ByteArrayOutputStream encBaos = new ByteArrayOutputStream();

        CryptoUtils.encryptWithSecretKey(encBais, encBaos, keySpec);

        byte[] encData = encBaos.toByteArray();

        System.out.println("encData.length: " + encData.length);
        System.out.println("encData: " + Arrays.toString(encData));

        assertNotNull(encData);
        assertTrue(encData.length >= inData.length);

        ByteArrayInputStream decBais = new ByteArrayInputStream(encData);
        ByteArrayOutputStream decBaos = new ByteArrayOutputStream();

        CryptoUtils.decryptWithSecretKey(decBais, decBaos, keySpec);

        byte[] decData = decBaos.toByteArray();

        System.out.println("decData.length: " + decData.length);
        System.out.println("decData: " + Arrays.toString(decData));

        assertNotNull(decData);
        assertTrue(decData.length <= encData.length);
        assertArrayEquals(inData, decData);
    }

    @Test
    public void testEncryptDecryptStreamsWithIV() throws Exception {
        byte[] keyBytes = new byte[16]; // Key is all 0's.
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16]; // IV is all 0's.
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        byte[] inData = new byte[16];
        Random random = new Random();
        random.nextBytes(inData);

        System.out.println("inData.length: " + inData.length);
        System.out.println("inData: " + Arrays.toString(inData));

        ByteArrayInputStream encBais = new ByteArrayInputStream(inData);
        ByteArrayOutputStream encBaos = new ByteArrayOutputStream();

        CryptoUtils.encryptWithSecretKey(encBais, encBaos, keySpec, ivParameterSpec);

        byte[] encData = encBaos.toByteArray();

        System.out.println("encData.length: " + encData.length);
        System.out.println("encData: " + Arrays.toString(encData));

        assertNotNull(encData);
        assertTrue(encData.length >= inData.length);

        ByteArrayInputStream decBais = new ByteArrayInputStream(encData);
        ByteArrayOutputStream decBaos = new ByteArrayOutputStream();

        CryptoUtils.decryptWithSecretKey(decBais, decBaos, keySpec, ivParameterSpec);

        byte[] decData = decBaos.toByteArray();

        System.out.println("decData.length: " + decData.length);
        System.out.println("decData: " + Arrays.toString(decData));

        assertNotNull(decData);
        assertTrue(decData.length <= encData.length);
        assertArrayEquals(inData, decData);
    }
}
