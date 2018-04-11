package io.topiacoin.crypto;

import io.topiacoin.crypto.impl.RSAMessageSigningProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class RSAMessageSignerTest extends AbstractMessageSigningProviderTest {
    @Override
    protected MessageSigningProvider getMessageSigner() {
        return new RSAMessageSigningProvider();
    }

    @Override
    protected KeyPair getKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();
        return keyPair;
    }
}
