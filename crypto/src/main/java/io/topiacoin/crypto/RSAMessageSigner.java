package io.topiacoin.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;


public class RSAMessageSigner  extends AbstractMessageSigner {

    @Override
    protected String getSignatureAlgorithm() {
        return "SHA1WithRSA";
    }
}
