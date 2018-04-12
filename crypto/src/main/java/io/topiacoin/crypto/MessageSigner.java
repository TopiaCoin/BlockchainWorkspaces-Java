package io.topiacoin.crypto;

import io.topiacoin.crypto.impl.DSAMessageSigningProvider;
import io.topiacoin.crypto.impl.ECDSAMessageSigningProvider;
import io.topiacoin.crypto.impl.MessageSigningProvider;
import io.topiacoin.crypto.impl.RSAMessageSigningProvider;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

public class MessageSigner {

    private static Map<String, MessageSigningProvider> _signingProviderCache = new HashMap<>();

    public static byte[] sign(byte[] bufferToSign, KeyPair keyPair) throws CryptographicException {
        MessageSigningProvider messageSigner = getMessageSigningProviderForKeyPair(keyPair);

        return messageSigner.sign(bufferToSign, keyPair);
    }

    public static byte[] sign(ByteBuffer bufferToSign, KeyPair keyPair) throws CryptographicException {
        MessageSigningProvider messageSigner = getMessageSigningProviderForKeyPair(keyPair);

        return messageSigner.sign(bufferToSign, keyPair);
    }

    public static boolean verify(byte[] bufferToVerify, KeyPair keyPair, byte[] signature) throws CryptographicException {
        MessageSigningProvider messageSigner = getMessageSigningProviderForKeyPair(keyPair);

        return messageSigner.verify(bufferToVerify, keyPair, signature);
    }

    public static boolean verify(ByteBuffer bufferToVerify, KeyPair keyPair, byte[] signature) throws CryptographicException {
        MessageSigningProvider messageSigner = getMessageSigningProviderForKeyPair(keyPair);

        return messageSigner.verify(bufferToVerify, keyPair, signature);
    }

    private static MessageSigningProvider getMessageSigningProviderForKeyPair(KeyPair keyPair) throws CryptographicException {
        MessageSigningProvider messageSigningProvider = null;

        String algorithm = keyPair.getPublic().getAlgorithm();

        messageSigningProvider = _signingProviderCache.get(algorithm);
        if (messageSigningProvider == null) {
            if (algorithm.equals("RSA")) {
                messageSigningProvider = new RSAMessageSigningProvider();
            } else if (algorithm.equals("EC")) {
                messageSigningProvider = new ECDSAMessageSigningProvider();
            } else if (algorithm.equals("DSA")) {
                messageSigningProvider = new DSAMessageSigningProvider();
            } else {
                throw new CryptographicException("Signing Error: Unrecognized Key Type: " + algorithm);
            }

            _signingProviderCache.put(algorithm, messageSigningProvider);
        }

        return messageSigningProvider;
    }

}
