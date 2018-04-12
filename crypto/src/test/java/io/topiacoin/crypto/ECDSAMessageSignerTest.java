package io.topiacoin.crypto;

import io.topiacoin.crypto.impl.ECDSAMessageSigningProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;

public class ECDSAMessageSignerTest extends AbstractMessageSigningProviderTest {
    @Override
    protected MessageSigningProvider getMessageSigner() {
        return new ECDSAMessageSigningProvider();
    }

    @Override
    protected KeyPair getKeyPair() throws Exception {
        ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("secp256k1");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(ecGenSpec, new SecureRandom());

        KeyPair keyPair = kpg.generateKeyPair();
        return keyPair;
    }


    @Test
    public void testPublicKeyRecovery() throws Exception {
        byte[] dataToSign = new byte[1024];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(dataToSign);

        ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("secp256k1");
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(ecGenSpec, new SecureRandom());

        KeyPair keyPair = g.generateKeyPair();

        ((ECPublicKey)keyPair.getPublic()).getParams() ;

        System.out.println ( "Public Key: " + Hex.toHexString(keyPair.getPublic().getEncoded())) ;

        byte[] messageHash = HashUtils.sha1(dataToSign) ;

        MessageSigningProvider messageSigner = getMessageSigner();

        byte[] signature = messageSigner.sign(dataToSign, keyPair) ;

        System.out.println ( "Signature: " + Hex.toHexString(signature));

        ByteBuffer buffer = ByteBuffer.wrap(signature) ;
        buffer.get(); // Initial 30
        buffer.get(); // Total Length
        buffer.get(); // Type
        byte rLength = buffer.get(); // Length of R
        byte[] rBytes = new byte[rLength] ;
        buffer.get(rBytes) ; // Bytes comprising R
        buffer.get();
        byte sLength = buffer.get();
        byte[] sBytes = new byte[sLength] ;
        buffer.get(sBytes);

        BigInteger r = new BigInteger(1, rBytes);
        BigInteger s = new BigInteger(1, sBytes);

        ECKey.ECDSASignature digSig = new ECKey.ECDSASignature(r, s) ;

        for ( int i = 0 ; i < 4 ; i++ ) {
            ECKey recoveredKey = ECKey.recoverFromSignature(i, digSig, messageHash, true);

            if ( recoveredKey != null ) {
                System.out.println("Recovered Key " + i + ": " + Hex.toHexString(recoveredKey.getPubKey()));
            } else {
                System.out.println ( "No Key " + i );
            }
        }
    }
}
