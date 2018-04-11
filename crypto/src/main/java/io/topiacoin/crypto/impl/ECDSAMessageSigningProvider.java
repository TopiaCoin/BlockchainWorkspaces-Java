package io.topiacoin.crypto.impl;

public class ECDSAMessageSigningProvider extends AbstractMessageSigningProvider {

    @Override
    protected String getSignatureAlgorithm() {
        return "SHA1WithECDSA";
    }
}
