package io.topiacoin.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class ECDSAMessageSignerTest extends AbstractMessageSignerTest {
    @Override
    protected MessageSigner getMessageSigner() {
        return new ECDSAMessageSigner();
    }

    @Override
    protected KeyPair getKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        KeyPair keyPair = kpg.generateKeyPair();
        return keyPair;
    }
}
