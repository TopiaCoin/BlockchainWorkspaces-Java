package io.topiacoin.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class RSAMessageSignerTest extends AbstractMessageSignerTest {
    @Override
    protected MessageSigner getMessageSigner() {
        return new RSAMessageSigner();
    }

    @Override
    protected KeyPair getKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();
        return keyPair;
    }
}
