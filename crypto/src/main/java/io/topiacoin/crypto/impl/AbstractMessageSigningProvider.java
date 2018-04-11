package io.topiacoin.crypto.impl;

import io.topiacoin.crypto.MessageSigningProvider;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

public abstract class AbstractMessageSigningProvider implements MessageSigningProvider {

    protected abstract String getSignatureAlgorithm() ;

    @Override
    public byte[] sign(byte[] bufferToSign, KeyPair keyPair) {
        byte[] signature = null;

        try {
            Signature dsa = Signature.getInstance(getSignatureAlgorithm());
            dsa.initSign(keyPair.getPrivate());
            dsa.update(bufferToSign);
            signature = dsa.sign();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return signature;
    }

    public byte[] sign(ByteBuffer bufferToSign, KeyPair keyPair) {
        byte[] signature = null;

        try {
            Signature dsa = Signature.getInstance(getSignatureAlgorithm());
            dsa.initSign(keyPair.getPrivate());
            dsa.update(bufferToSign);
            signature = dsa.sign();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return signature;
    }

    @Override
    public boolean verify(byte[] bufferToVerify, KeyPair keyPair, byte[] signature) {

        boolean signatureIsValid = false;

        try {
            Signature dsa = Signature.getInstance(getSignatureAlgorithm());
            dsa.initVerify(keyPair.getPublic());
            dsa.update(bufferToVerify);
            dsa.verify(signature);
            signatureIsValid = true;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return signatureIsValid;
    }

    public boolean verify(ByteBuffer bufferToVerify, KeyPair keyPair, byte[] signature) {

        boolean signatureIsValid = false;

        try {
            Signature dsa = Signature.getInstance(getSignatureAlgorithm());
            dsa.initVerify(keyPair.getPublic());
            dsa.update(bufferToVerify);
            dsa.verify(signature);
            signatureIsValid = true;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return signatureIsValid;
    }
}
