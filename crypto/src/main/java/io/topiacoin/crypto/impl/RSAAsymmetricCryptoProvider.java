package io.topiacoin.crypto.impl;

import io.topiacoin.crypto.CryptographicException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RSAAsymmetricCryptoProvider implements SecrataAsymmetricCryptoProvider {

    private static RSAAsymmetricCryptoProvider __instance;

    public static SecrataAsymmetricCryptoProvider getInstance() {
        if ( __instance == null ) {
            __instance = new RSAAsymmetricCryptoProvider();
        }
        return __instance;
    }

    @Override
    public byte[] encryptWithPublicKey(byte[] inputData, PublicKey publicKey) throws CryptographicException {
        try {
            Cipher cipher = Cipher.getInstance(publicKey.getAlgorithm());
            cipher.init(Cipher.PUBLIC_KEY, publicKey);
            return cipher.doFinal(inputData);
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
            Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
            cipher.init(Cipher.PRIVATE_KEY, privateKey);
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return decryptedData;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (InvalidKeyException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (NoSuchPaddingException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (BadPaddingException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        } catch (IllegalBlockSizeException e) {
            throw new CryptographicException("Failed to decrypt data with public key", e);
        }
    }
}
