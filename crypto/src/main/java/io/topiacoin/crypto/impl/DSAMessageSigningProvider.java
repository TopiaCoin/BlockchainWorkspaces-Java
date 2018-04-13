package io.topiacoin.crypto.impl;


public class DSAMessageSigningProvider extends AbstractMessageSigningProvider {

    @Override
    protected String getSignatureAlgorithm() {
        return "SHA1WithDSA";
    }
}
