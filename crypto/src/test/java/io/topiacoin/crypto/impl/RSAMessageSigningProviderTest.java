package io.topiacoin.crypto.impl;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class RSAMessageSigningProviderTest extends AbstractMessageSigningProviderTest {
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
