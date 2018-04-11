package io.topiacoin.crypto.impl;


public class RSAMessageSigningProvider extends AbstractMessageSigningProvider {

    @Override
    protected String getSignatureAlgorithm() {
        return "SHA1WithRSA";
    }
}
