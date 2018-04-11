package io.topiacoin.crypto;

import io.topiacoin.crypto.impl.ECDSAMessageSigningProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class ECDSAMessageSignerTest extends AbstractMessageSigningProviderTest {
    @Override
    protected MessageSigningProvider getMessageSigner() {
        return new ECDSAMessageSigningProvider();
    }

    @Override
    protected KeyPair getKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        KeyPair keyPair = kpg.generateKeyPair();
        return keyPair;
    }
}
