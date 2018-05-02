package io.topiacoin.dht.network;

import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.DHTTestConfiguration;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static org.junit.Assert.*;

public class NodeIDTest {



    @Test
    public void testNodeIDCreation() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(10);
        configuration.setC2(20);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        NodeID nodeID = nodeIDGenerator.generateNodeID();

        assertTrue("The generated Node ID does not pass the static validation check", isValid(10, nodeID.getNodeID()));
        byte[] xoredNodeID = xorArrays(nodeID.getNodeID(), nodeID.getValidation());
        assertTrue("The generated Node ID and validation do not pass the dynamic validation check", isValid(20, xoredNodeID));

        assertNotNull("The NodeID did not return a generated KeyPair", nodeID.getKeyPair());

        assertTrue ( "Newly Created NodeID should be valid", nodeIDGenerator.validateNodeID(nodeID)) ;
    }



    @Test
    public void testNodeIDCreationFromNodeIDAndValidation() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        byte[] nodeIDBytes = new byte[]{61, 22, -2, -47, 86, 84, 112, 9, 120, 73, -24, 73, 123, 17, 66, 60, -100, 3, 115, 42};
        byte[] validationBytes = new byte[]{-7, 74, -89, -46, -36, 101, -26, 34, 44, 11, 113, -12, 119, 77, 32, -40, -1, 107, 18, -41};

        NodeID nodeID = new NodeID(nodeIDBytes, validationBytes);

        assertTrue("The generated Node ID does not pass the static validation check", isValid(10, nodeID.getNodeID()));
        byte[] xoredNodeID = xorArrays(nodeID.getNodeID(), nodeID.getValidation());
        assertTrue("The generated Node ID and validation do not pass the dynamic validation check", isValid(20, xoredNodeID));

        assertTrue ( "Newly Created NodeID should be valid", nodeIDGenerator.validateNodeID(nodeID)) ;
    }



    @Test
    public void testNodeIDCreationFromKeyPairAndValidation() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        byte[] publicBytes = new byte[]{48, 89, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -122, 72, -50, 61, 3, 1, 7, 3, 66, 0, 4, -29, -18, 39, -101, -109, -100, -39, 79, 121, -126, 42, -2, -57, 120, -123, 55, -115, 38, -107, 81, 42, -27, 106, 5, -41, -64, 90, 65, -34, -119, 108, 51, 27, 42, -102, 69, 71, -54, 9, -124, 117, 78, 78, 23, -87, 101, -19, -30, -126, 20, -119, 13, -19, 107, 96, 106, 62, 118, -115, 119, 104, -122, -113, -14};
        byte[] privateBytes = new byte[]{48, 65, 2, 1, 0, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -122, 72, -50, 61, 3, 1, 7, 4, 39, 48, 37, 2, 1, 1, 4, 32, 26, -108, -105, -29, 80, -33, -61, -20, -85, 48, 92, 92, 32, -30, -41, -113, 22, -47, 64, -51, -51, -11, -25, -51, 6, -62, 31, -115, -44, -56, 125, -21};
        PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(publicBytes));
        PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        byte[] validationBytes = new byte[]{-99, 56, 114, -64, -39, 121, -41, -123, 121, -111, -118, 35, 50, -83, -18, 102, 53, -73, -81, 50};

        NodeID nodeID = new NodeID(keyPair, validationBytes);

        assertTrue("The generated Node ID does not pass the static validation check", isValid(10, nodeID.getNodeID()));
        byte[] xoredNodeID = xorArrays(nodeID.getNodeID(), nodeID.getValidation());
        assertTrue("The generated Node ID and validation do not pass the dynamic validation check", isValid(20, xoredNodeID));

        KeyPair retrievedKeyPair = nodeID.getKeyPair();
        assertEquals("The keypair retrieved from the Node ID does not match the KeyPair used to generate it", keyPair, retrievedKeyPair);

        assertTrue ( "Newly Created NodeID should be valid", nodeIDGenerator.validateNodeID(nodeID)) ;
    }



    @Test
    public void testNodeIDCreationFromNodeIDAndValidationWhenNodeIDIsInvalid() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        byte[] nodeIDBytes = new byte[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, -10, -20, -128, -30, -40, -50, -60};
        byte[] validationBytes = new byte[]{-7, 74, -89, -46, -36, 101, -26, 34, 44, 11, 113, -12, 119, 77, 32, -40, -1, 107, 18, -41};

        NodeID nodeID = new NodeID(nodeIDBytes, validationBytes);

        assertTrue("The generated Node ID does not pass the static validation check", isValid(10, nodeID.getNodeID()));
        byte[] xoredNodeID = xorArrays(nodeID.getNodeID(), nodeID.getValidation());
        assertTrue("The generated Node ID and validation do not pass the dynamic validation check", isValid(20, xoredNodeID));
    }



    @Test
    public void testNodeIDCreationFromNodeIDAndValidationWhenValidationIsInvalid() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        byte[] nodeIDBytes = new byte[]{61, 22, -2, -47, 86, 84, 112, 9, 120, 73, -24, 73, 123, 17, 66, 60, -100, 3, 115, 42};
        byte[] validationBytes = new byte[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, -10, -20, -128, -30, -40, -50, -60};

        NodeID nodeID = new NodeID(nodeIDBytes, validationBytes);

        assertTrue("The generated Node ID does not pass the static validation check", isValid(10, nodeID.getNodeID()));
        byte[] xoredNodeID = xorArrays(nodeID.getNodeID(), nodeID.getValidation());
        assertTrue("The generated Node ID and validation do not pass the dynamic validation check", isValid(20, xoredNodeID));
    }



    @Test
    public void testNodeIDCreationFromKeyPairAndValidationWhenKeyPairIsInvalid() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        // Generate a random keypair.  Odds are that it won't meet the requirements and will cause the
        // constructor to throw.  If it does happen to match, then the validation bytes will almost
        // certainly cause it to throw as the likelihood of the key validating and then the validation
        // string validating the generated node ID are astronomical.
        KeyPair keyPair = CryptoUtils.generateECKeyPair();

        byte[] validationBytes = new byte[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, -10, -20, -128, -30, -40, -50, -60};

        NodeID nodeID = new NodeID(keyPair, validationBytes);

        assertFalse ( nodeIDGenerator.validateNodeID(nodeID )) ;
    }



    @Test
    public void testNodeIDCreationFromKeyPairAndValidationWhenValidationIsInvalid() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        byte[] publicBytes = new byte[]{48, 89, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -122, 72, -50, 61, 3, 1, 7, 3, 66, 0, 4, -29, -18, 39, -101, -109, -100, -39, 79, 121, -126, 42, -2, -57, 120, -123, 55, -115, 38, -107, 81, 42, -27, 106, 5, -41, -64, 90, 65, -34, -119, 108, 51, 27, 42, -102, 69, 71, -54, 9, -124, 117, 78, 78, 23, -87, 101, -19, -30, -126, 20, -119, 13, -19, 107, 96, 106, 62, 118, -115, 119, 104, -122, -113, -14};
        byte[] privateBytes = new byte[]{48, 65, 2, 1, 0, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -122, 72, -50, 61, 3, 1, 7, 4, 39, 48, 37, 2, 1, 1, 4, 32, 26, -108, -105, -29, 80, -33, -61, -20, -85, 48, 92, 92, 32, -30, -41, -113, 22, -47, 64, -51, -51, -11, -25, -51, 6, -62, 31, -115, -44, -56, 125, -21};
        PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(publicBytes));
        PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        byte[] validationBytes = new byte[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, -10, -20, -128, -30, -40, -50, -60};

        NodeID nodeID = new NodeID(keyPair, validationBytes);

        assertFalse ( nodeIDGenerator.validateNodeID(nodeID )) ;
    }



    @Test
    public void testDistanceWithSelf() throws Exception {
        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        NodeID nodeID = nodeIDGenerator.generateNodeID();

        assertEquals ( "Expected the node have distance 0 to itself.", 0, nodeID.getDistance(nodeID) ) ;
    }



    @Test
    public void testDistanceCalculation() throws Exception {
        byte[] nodeID1Bytes = new byte[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, -10, -20, -128, -30, -40, -50, -60};
        byte[] validation1Bytes = new byte[]{-7, 74, -89, -46, -36, 101, -26, 34, 44, 11, 113, -12, 119, 77, 32, -40, -1, 107, 18, -41};

        byte[] nodeID2Bytes = new byte[]{0, 10, 20, 30, 48, 50, 60, 70, 80, 90, 100, 110, 120, -10, -20, -128, -30, -40, -50, -60};
        byte[] validation2Bytes = new byte[]{-7, 74, -89, -46, -36, 101, -26, 34, 44, 11, 113, -12, 119, 77, 32, -40, -1, 107, 18, -41};

        byte[] xorBytes = xorArrays(nodeID1Bytes, nodeID2Bytes) ;

        int zeroCount = countLeadingZeros(xorBytes) ;
        int distance = nodeID1Bytes.length * 8 - zeroCount ;

        NodeID nodeID1 = NodeID.createNodeID(nodeID1Bytes, validation1Bytes);
        NodeID nodeID2 = NodeID.createNodeID(nodeID2Bytes, validation2Bytes);

        assertEquals ( "Node Distance not as expected",  distance, nodeID1.getDistance(nodeID2)) ;
    }



    @Test
    public void testDistanceBetweenNodes() throws Exception {
        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        NodeID nodeID1 = nodeIDGenerator.generateNodeID();
        NodeID nodeID2 = nodeIDGenerator.generateNodeID();

        byte[] node1Bytes = nodeID1.getNodeID();
        byte[] node2Bytes = nodeID2.getNodeID();
        byte[] xorBytes = xorArrays(node1Bytes, node2Bytes) ;

        int zeroCount = countLeadingZeros(xorBytes) ;
        int distance = node1Bytes.length * 8 - zeroCount ;

        assertEquals ( "Node Distance not as expected", distance, nodeID1.getDistance(nodeID2)) ;
    }



    @Test
    public void testDistanceBetweenNodesIsSymmetric() throws Exception {
        byte[] nodeID1Bytes = new byte[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, -10, -20, -128, -30, -40, -50, -60};
        byte[] validation1Bytes = new byte[]{-7, 74, -89, -46, -36, 101, -26, 34, 44, 11, 113, -12, 119, 77, 32, -40, -1, 107, 18, -41};

        byte[] nodeID2Bytes = new byte[]{0, 10, 20, 30, 40, 50, 64, 70, 80, 90, 100, 110, 120, -10, -20, -128, -30, -40, -50, -60};
        byte[] validation2Bytes = new byte[]{-7, 74, -89, -46, -36, 101, -26, 34, 44, 11, 113, -12, 119, 77, 32, -40, -1, 107, 18, -41};

        NodeID nodeID1 = NodeID.createNodeID(nodeID1Bytes, validation1Bytes);
        NodeID nodeID2 = NodeID.createNodeID(nodeID2Bytes, validation2Bytes);

        assertEquals ( "Node Distance not as expected",  nodeID1.getDistance(nodeID2), nodeID2.getDistance(nodeID1)) ;
    }



    @Test
    public void testCreateNodeWithDistance() throws Exception {
        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        NodeID nodeID = nodeIDGenerator.generateNodeID();

        // Verify that we can successfully create node IDs that are every possible distance from this node (except for distance 0).
        for ( int distance = 1 ; distance < 160 ; distance++ ) {
            NodeID distantNodeID1 = nodeID.generateNodeIDByDistance(distance);
            NodeID distantNodeID2 = nodeID.generateNodeIDByDistance(distance);
            NodeID distantNodeID3 = nodeID.generateNodeIDByDistance(distance);
            NodeID distantNodeID4 = nodeID.generateNodeIDByDistance(distance);

            assertEquals("Calculated Node ID is not the correct distance away", distance, nodeID.getDistance(distantNodeID1));
            assertEquals("Calculated Node ID is not the correct distance away", distance, nodeID.getDistance(distantNodeID2));
            assertEquals("Calculated Node ID is not the correct distance away", distance, nodeID.getDistance(distantNodeID3));
            assertEquals("Calculated Node ID is not the correct distance away", distance, nodeID.getDistance(distantNodeID4));
        }
    }



    @Test
    public void testCreatedDistantNodeNotValid() throws Exception {
        DHTConfiguration configuration = new DHTTestConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        NodeID nodeID = nodeIDGenerator.generateNodeID();

        // Verify that we can successfully create node IDs that are every possible distance from this node (except for distance 0).
        for ( int distance = 1 ; distance < 160 ; distance++ ) {
            NodeID distantNodeID1 = nodeID.generateNodeIDByDistance(distance);
            assertFalse("Calculated Node ID should not have been valid", nodeIDGenerator.validateNodeID(distantNodeID1));
        }
    }


    @Test
    public void testEncodingAndDecoding() throws  Exception {
        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        NodeID nodeID = nodeIDGenerator.generateNodeID();

        ByteBuffer buffer = ByteBuffer.allocate(65535) ;

        nodeID.encode(buffer);
        buffer.flip();

        NodeID decodedNodeID = NodeID.decode(buffer) ;

        assertEquals ( nodeID, decodedNodeID) ;
    }

    // -------- Private validation methods --------



    private boolean isValid(int leadingZeros, byte[] value) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] result = sha1.digest(value);

        boolean valid = true;
        int bitIndex = 0;
        while (valid && bitIndex < leadingZeros) {
            byte mask = (byte) (1 << (7 - bitIndex % 8));
            valid = ((result[bitIndex / 8] ^ mask) != 0);
            bitIndex++;
        }

        return valid;
    }



    private byte[] xorArrays(byte[] first, byte[] second) {
        byte[] result = new byte[first.length];
        System.arraycopy(first, 0, result, 0, result.length);

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (result[i] ^ second[i]);
        }

        return result;
    }


    private int countLeadingZeros(byte[] value) {
        int count = 0 ;

        for ( int i = 0 ; i < value.length ; i++ ){
            if ( value[i] == 0 ) {
                count += 8 ;
            } else {
                for ( int j = 7 ; j >= 0 ; j-- ) {
                    int mask = 1 << j;
                    if ( (value[i] & mask) == 0 ) {
                        count++ ;
                    } else {
                        break ;
                    }
                }
                break ;
            }
        }

        return count ;
    }
}
