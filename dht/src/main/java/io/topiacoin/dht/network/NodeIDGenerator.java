package io.topiacoin.dht.network;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class NodeIDGenerator {

    private Log _log = LogFactory.getLog(this.getClass());

    private Configuration configuration;

    public NodeIDGenerator(Configuration configuration) {
        this.configuration = configuration;
    }

    public NodeID generateNodeID() {

        try {
            KeyPair keyPair = null;
            byte[] nodeID = null;
            byte[] validation = null;

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            Random random = new Random();

            boolean part1Done = false;
            boolean part2Done = false;

            // Find a key that conforms to the First check: H(H(pubKey)) < 2^(160-c1)
            while (!part1Done) {
                keyPair = kpg.generateKeyPair();

                byte[] encodedPublicKey = keyPair.getPublic().getEncoded();
                nodeID = sha1.digest(encodedPublicKey);
                part1Done = isValidSolution(configuration.getC1(), sha1, nodeID);
            }

            validation = new byte[nodeID.length];
            while (!part2Done) {
                random.nextBytes(validation);

                byte[] xoredNodeID = Utilities.xorByteArrays(nodeID, validation);
                part2Done = isValidSolution(configuration.getC2(), sha1, xoredNodeID);
            }

            return new NodeID(keyPair, validation);
        } catch (NoSuchAlgorithmException e) {
            _log.fatal("Unable to find required cryptographic Algorithms", e);
            throw new RuntimeException("Unable to find the required cryptographic algorithms", e);
        }

    }

    public boolean validateNodeID(NodeID nodeID) {
        int c1 = configuration.getC1();
        int c2 = configuration.getC2();

        boolean valid = (nodeID.getNodeID() != null) && (nodeID.getValidation() != null);

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

            valid &= isValidSolution(c1, sha1, nodeID.getNodeID());

            if (valid) {
                byte[] xoredNodeID = Utilities.xorByteArrays(nodeID.getNodeID(), nodeID.getValidation());
                valid &= isValidSolution(c2, sha1, xoredNodeID);
            }
        } catch (NoSuchAlgorithmException e) {
            _log.fatal("Unable to find required cryptographic Algorithms", e);
            throw new RuntimeException("Unable to find the required cryptographic algorithms", e);
        }

        return valid;
    }

    /**
     * Tests whether the proposed value meets the requirements of having a hash with the required number of leading
     * zeros.
     *
     * @param c1     The number of leading zeros the cryptographic hash must have.
     * @param digest The message digest used to validate the value.
     * @param value  The value to be validated.
     *
     * @return True if the value's hash has the required number of leading zeros.  False if the value's hash does not
     * have the required number of leading zeros.
     */
    private boolean isValidSolution(int c1, MessageDigest digest, byte[] value) {
        byte[] hash1 = digest.digest(value);
        return Utilities.hasSufficientZeros(hash1, c1);
    }


}
