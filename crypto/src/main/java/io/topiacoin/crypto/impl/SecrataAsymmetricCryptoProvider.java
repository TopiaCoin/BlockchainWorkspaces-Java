package io.topiacoin.crypto.impl;

import io.topiacoin.crypto.CryptographicException;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface SecrataAsymmetricCryptoProvider {

    byte[] encryptWithPublicKey(byte[] inputData, PublicKey publicKey) throws CryptographicException;

    byte[] decryptWithPrivateKey(byte[] encryptedData, PrivateKey privateKey) throws CryptographicException;
}
