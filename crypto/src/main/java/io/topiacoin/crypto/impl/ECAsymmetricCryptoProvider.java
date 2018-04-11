package io.topiacoin.crypto.impl;

import io.topiacoin.crypto.CryptographicException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class ECAsymmetricCryptoProvider implements SecrataAsymmetricCryptoProvider {

    private static ECAsymmetricCryptoProvider __instance;

    public static SecrataAsymmetricCryptoProvider getInstance() {
        if ( __instance == null ) {
            __instance = new ECAsymmetricCryptoProvider();
        }
        return __instance;
    }

    @Override
    public byte[] encryptWithPublicKey(byte[] inputData, PublicKey publicKey) throws CryptographicException {

        try {
            // Generate a message EC KeyPair
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            KeyPair keyPair = kpg.generateKeyPair();

            // Perform Key Agreement with message private key and provided public key
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(keyPair.getPrivate());
            ka.doPhase(publicKey, true);
            byte[] sharedSecretBytes = ka.generateSecret() ;
            SecretKeySpec sharedSecret = new SecretKeySpec(sharedSecretBytes, 0, 16, "AES");

            // Encrypt the inputData with the derived key
            Cipher cipher = Cipher.getInstance(sharedSecret.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, sharedSecret);
            byte[] encryptedData = cipher.doFinal(inputData);

            // Combine the encryptedData with the message public key
            byte[] dataToReturn = combinePublicKeyAndEncryptedData(keyPair.getPublic(), encryptedData);

            return dataToReturn;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Failed to encrypt data with public key", e);
        } catch (InvalidKeyException e) {
            throw new CryptographicException("Failed to encrypt data with public key", e);
        } catch (NoSuchPaddingException e) {
            throw new CryptographicException("Failed to encrypt data with public key", e);
        } catch (BadPaddingException e) {
            throw new CryptographicException("Failed to encrypt data with public key", e);
        } catch (IllegalBlockSizeException e) {
            throw new CryptographicException("Failed to encrypt data with public key", e);
        }
    }

    @Override
    public byte[] decryptWithPrivateKey(byte[] encryptedData, PrivateKey privateKey) throws CryptographicException {

        try {
            // Separate the message public key from the encrypted data
            byte[] messagePublicKey = extractPublicKeyFromMessage(encryptedData);
            byte[] dataToDecrypt = extractEncryptedDataFromMessage(encryptedData);

            KeyFactory kf = KeyFactory.getInstance("EC");
            X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(messagePublicKey);
            PublicKey publicKey = kf.generatePublic(pkSpec);

            // Perform Key agreement with message public key and provided private key
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(privateKey);
            ka.doPhase(publicKey, true);
            byte[] sharedSecretBytes = ka.generateSecret();
            SecretKeySpec sharedSecret = new SecretKeySpec(sharedSecretBytes, 0, 16, "AES");

            // Decrypt the encrypted data with the derived key
            Cipher cipher = Cipher.getInstance(sharedSecret.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, sharedSecret);
            byte[] decryptedData = cipher.doFinal(dataToDecrypt);

            return decryptedData;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (InvalidKeyException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (NoSuchPaddingException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (BadPaddingException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (InvalidKeySpecException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (IllegalBlockSizeException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        }
    }


    // ------ Private Methods --------

    private byte[] combinePublicKeyAndEncryptedData(PublicKey publicKey, byte[] encryptedData) {
        byte[] messagePublicKey = publicKey.getEncoded();

        ByteBuffer byteBuffer = ByteBuffer.allocate( messagePublicKey.length + encryptedData.length + 8) ;
        byteBuffer.put((byte)messagePublicKey.length) ;
        byteBuffer.put(messagePublicKey) ;
        byteBuffer.putInt(encryptedData.length);
        byteBuffer.put(encryptedData) ;
        byteBuffer.flip();

        byte[] dataToReturn = new byte[byteBuffer.limit()] ;
        byteBuffer.get(dataToReturn);

        return dataToReturn;
    }

    private byte[] extractPublicKeyFromMessage(byte[] encryptedData) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData) ;

        int pubKeySize = byteBuffer.get();
        byte[] messagePublicKey = new byte[pubKeySize] ;
        byteBuffer.get(messagePublicKey) ;

        return messagePublicKey;
    }

    private byte[] extractEncryptedDataFromMessage(byte[] encryptedData) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData) ;

        int pubKeySize = byteBuffer.get();
        byteBuffer.position(byteBuffer.position() + pubKeySize) ;
        int messageSize = byteBuffer.getInt();
        byte[] messageData = new byte[messageSize] ;
        byteBuffer.get(messageData);

        return messageData;
    }

}
