package io.topiacoin.crypto.impl;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class DSAMessageSigningProviderTest extends AbstractMessageSigningProviderTest {
    @Override
    protected MessageSigningProvider getMessageSigner() {
        return new DSAMessageSigningProvider();
    }

    @Override
    protected KeyPair getKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA");
        KeyPair keyPair = kpg.generateKeyPair();
        return keyPair;
    }
}
