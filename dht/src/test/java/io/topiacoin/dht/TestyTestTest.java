package io.topiacoin.dht;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Random;

public class TestyTestTest {

    //@Test
    public void testSecureNodeIDGeneration() throws Exception {

        long overallStart = 0;
        long overallStop = 0;

        int c1 = 11;
        int c2 = 20;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        Random random = new Random();

        KeyPair keyPair = kpg.generateKeyPair(); // Prime the key generation code

        byte[] nodeID = null;
        byte[] x = null;

        boolean part1Done = false;
        boolean part2Done = false;

        overallStart = System.currentTimeMillis();
        while (!part1Done && !part2Done) {

            long start = 0;
            long stop = 0;

            start = System.currentTimeMillis();

            // Find a key that conforms to the First check: H(H(pubKey)) < 2^(160-c1)
            int part1Count = 0;
            byte[] encodedPublicKey = keyPair.getPublic().getEncoded();
            while (!part1Done) {
                keyPair = kpg.generateKeyPair();

                byte[] hash1 = sha1.digest(sha1.digest(encodedPublicKey));
                part1Done = hasSufficientZeros(hash1, c1);
                part1Count++;
            }

            stop = System.currentTimeMillis();

            nodeID = sha1.digest(encodedPublicKey);
//            System.out.println ( "Digest: " + Hex.encodeHexString(digest)) ;
//            System.out.println ( "nodeID: " + nodeID.toString(16));

            System.out.println("Found a key that meets part 1 after " + part1Count + " tries. (" + (stop - start) + "ms)");

            start = System.currentTimeMillis();
            int part2Count = 0;
            while (!part2Done) {
                x = new byte[nodeID.length] ;
                random.nextBytes(x);

//                System.out.println ( "PubKey: " + bigNodeID.toString(16)) ;
//                System.out.println ( "X: " + x.toString(16)) ;

                byte[] xoredNodeID = xorByteArrays(nodeID, x);
//                BigInteger xoredNodeID = nodeIDBigInt.xor(x);

                byte[] hash2 = sha1.digest(xoredNodeID);

                part2Done = hasSufficientZeros(hash2, c2);
                part2Count++;
            }

            stop = System.currentTimeMillis();

            System.out.println("Found a key that meets part 2 after " + part2Count + " tries. (" + (stop - start) + "ms)");
        }
        overallStop = System.currentTimeMillis();

//        System.out.println("Public Key: " + keyPair.getPublic());

        System.out.println("NodeID: " + Hex.encodeHexString(nodeID));
        System.out.println("X     : " + Hex.encodeHexString(x));

        System.out.println("Proof 1 Result: " + Hex.encodeHexString(sha1.digest(nodeID)));
        System.out.println("Proof 2 Result: " + Hex.encodeHexString(sha1.digest(xorByteArrays(nodeID,x))));

        System.out.println("Total Elapsed Time: " + (overallStop - overallStart) + "ms");
    }

    private byte[] xorByteArrays(byte[] nodeID, byte[] xBytes) {
        if ( nodeID.length != xBytes.length ) throw new RuntimeException("Arrays must be of the same length");

        byte[] result = new byte[nodeID.length] ;

        for ( int i = 0 ; i < result.length ; i++ ) {
            result[i] = (byte)(nodeID[i] ^ xBytes[i]) ;
        }

        return result;
    }

    private boolean hasSufficientZeros(byte[] data, int requiredZeros) {
        BigInteger intVersion = new BigInteger(1, data);
        BigInteger limit = BigInteger.valueOf(1L);
        limit = limit.shiftLeft((data.length * 8) - requiredZeros);

//        System.out.println ( "data : " + intVersion.toString(16)) ;
//        System.out.println ( "limit: " + limit.toString(16) ) ;

        return (intVersion.compareTo(limit) < 0);
    }
}
