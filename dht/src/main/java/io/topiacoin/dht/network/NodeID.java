package io.topiacoin.dht.network;

import io.topiacoin.dht.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

/**
 * An ID representing a Node in the DHT network.  Node IDs are generated using the algorithm specified in the S/Kademlia
 * whitepaper. This is done to slow the creation of NodeIDs to prevent Sybil and Eclipse attacks on the DHT network.
 * <p>
 * A new NodeID is created by generating a Elliptic Curve cryptographic keypair and hashing it.  A NodeID is considered
 * valid if a hash of the nodeID has a minimum number of leading zeros.  If a generated NodeID isn't valid, a new
 * cryptographic keypair is generated and the process is repeated.
 * <p>
 * Once a valid NodeID has been found, a random validation value is generated that, when XORed with the NodeID and
 * hashed, is checked to see if the hash has a minimum number of leading zeros.  If the hash has the necessary number of
 * leading zeros, the validation value is considered good and is stored along with the NodeID.
 * <p>
 * NodeIDs can also be created using existing raw nodeID and validation values, or from an existing KeyPair and
 * validation value.  In either case the class will validate the the keyPair represents a valid NodeID, and that the
 * validation value properly validates the nodeID.
 */
public class NodeID {

    private Log _log = LogFactory.getLog(this.getClass());

    private byte[] nodeID;
    private byte[] validation;
    private KeyPair _keyPair;

    private int c1 = 11;
    private int c2 = 20;

    /**
     * Creates a new NodeID from the specified nodeID and Validation value.  If the nodeID is not a valid value, or if
     * the validation value does not properly validate the nodeID, an IllegalArgumentException is thrown.
     *
     * @param nodeID     The nodeID value that is being used to create this NodeID.
     * @param validation The validation value that confirms that the necessary work has been done to generate this ID.
     *
     * @throws IllegalArgumentException If the keyPair doesn't generate a valid NodeID, or if the validation value
     *                                  doesn't validate the NodeID generated from the keyPair.
     */
    public NodeID(byte[] nodeID, byte[] validation) throws IllegalArgumentException {
        this.nodeID = nodeID;
        this.validation = validation;
    }

    /**
     * Creates a new NodeID from the specified keyPair and Validation value.  If the KeyPair does not generate a valid
     * NodeID, or if the validation value does not properly validate the NodeID created from the KeyPair, an
     * IllegalArgumentException is thrown.
     *
     * @param keyPair    The KeyPair from which the NodeID is to be generated.
     * @param validation The validation value that confirms that the necessary work has been done to generate this ID.
     *
     * @throws IllegalArgumentException If the keyPair doesn't generate a valid NodeID, or if the validation value
     *                                  doesn't validate the NodeID generated from the keyPair.
     */
    public NodeID(KeyPair keyPair, byte[] validation) throws IllegalArgumentException {
        _keyPair = keyPair;
        this.validation = validation;

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            this.nodeID = sha1.digest(_keyPair.getPublic().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            _log.fatal("Unable to find required cryptographic Algorithms", e);
            throw new RuntimeException("Unable to find the required cryptographic algorithms", e);
        }
    }

    private NodeID(byte[] nodeID) {
        this.nodeID = nodeID;
    }

    /**
     * Returns the nodeID value encapsulated in this NodeID object.
     *
     * @return the byte array containing the raw nodeID of this object.
     */
    public byte[] getNodeID() {
        return nodeID;
    }

    /**
     * Returns the validation value encapsulared in this NodeID object.
     *
     * @return the byte array containing the validation value of this Node ID.
     */
    public byte[] getValidation() {
        return validation;
    }

    /**
     * Returns the cryptographic keypair used to generate this Node ID, if available.
     *
     * @return The cryptographic keypair used to generate this NodeID, or null if no keypair was used to create this
     * object.
     */
    public KeyPair getKeyPair() {
        return _keyPair;
    }


    /**
     * Returns the distance between this nodeID and another nodeID.  Distance is calculated as being the number of
     * leading bits the node IDs have in common.
     *
     * @param other The nodeID whose distance from this nodeID is being calculated
     *
     * @return The distance between this nodeID and the other nodeID.  The value will be between 0 and the length of the
     * nodeIDs in bits.
     */
    public int getDistance(NodeID other) {

        byte[] thisNodeID = this.getNodeID();
        byte[] thatNodeID = other.getNodeID();

        // Assume the nodes are as far apart as possible.
        int distance = thisNodeID.length * 8;

        for (int i = 0; i < thisNodeID.length; i++) {
            if (thisNodeID[i] == thatNodeID[i]) {
                // This byte matches between the two NodeIDs, so subtract 8 from the distance.
                distance -= 8;
            } else {
                byte thisByte = thisNodeID[i];
                byte thatByte = thatNodeID[i];
                byte xorByte = (byte) (thisByte ^ thatByte);

                for (i = 7; i >= 0; i--) {
                    if ((xorByte & (1 << i)) == 0) {
                        distance--;
                    } else {
                        // These bytes do not match.  We have found the end!
                        break;
                    }
                }

                // This branch must have found the end of the matching prefix, so break out.
                break;
            }
        }

        return distance;
    }


    /**
     * Returns a NodeID object that is the specified distance away from this NodeID.  Note that this NodeID is
     * <b>note</b> a valid NodeID as it does not meet the criteria for being valid.  It is intended to be used in search
     * operations where a random node ID a certain distance away is required.
     *
     * @param distance The distance from this node that the new NodeID should be.
     *
     * @return A NodeID object that is the requested distance away from this NodeID.
     */
    public NodeID generateNodeIDByDistance(int distance) {
        byte[] result = new byte[this.nodeID.length];

        // Generate a random Node ID value
        Random random = new Random();
        random.nextBytes(result);

        // Mask the new value with the appropriate prefix from this node.
        int msbBitsToMatch = result.length * 8 - distance;

        for (int i = 0; i < result.length; i++) {
            if (msbBitsToMatch >= 8) {
                result[i] = this.nodeID[i];
                msbBitsToMatch -= 8;
            } else {
                // Set the this octet to match the first set of bits to the nodeID, and the remaining
                // should be inverted to prevent random ID length mismatches.
                byte mask = (byte) (0xff << (8 - msbBitsToMatch));
                byte inverseMask = (byte) (~mask);
                byte prefix = (byte) (this.nodeID[i] & mask);
                byte suffix = (byte) (~this.nodeID[i] & inverseMask);
                result[i] = (byte) (prefix | suffix);
                break;
            }
        }

        //  TODO Generate the node id value

        NodeID nodeID = new NodeID(result);

        return nodeID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeID nodeID1 = (NodeID) o;

        if (!Arrays.equals(nodeID, nodeID1.nodeID)) return false;
        return Arrays.equals(validation, nodeID1.validation);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(nodeID);
        result = 31 * result + Arrays.hashCode(validation);
        return result;
    }


    // -------- Package Scope Methods - Mostly for Testing --------

    static NodeID createNodeID(byte[] nodeID, byte[] validation) throws IllegalArgumentException {
        NodeID nID = new NodeID(nodeID);
        nID.validation = validation;

        return nID;
    }


    // -------- Private Methods --------

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
