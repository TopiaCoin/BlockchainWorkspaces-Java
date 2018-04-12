package io.topiacoin.dht;

import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import io.topiacoin.dht.routing.RoutingTable;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyPair;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class DHTBootstrapTest {

    public static int port = 37000;

    public static synchronized int getPortNumber() {
        return 0 ;
    }


    @Test
    public void testBootstrapNode() throws Exception {

        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);
        configuration.setResponseTimeout(250);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";
        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        Random random = new Random();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        try {
            // Prepopulate the dht1 Routing Table with Nodes
            RoutingTable routingTable1 = dht1._dhtComponents.getRoutingTable();
            RoutingTable routingTable2 = dht2._dhtComponents.getRoutingTable();

            Node node1 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
            Node node2 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
            Node node3 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
            Node node4 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
            routingTable1.insert(node1);
            routingTable1.insert(node2);
            routingTable1.insert(node3);
            routingTable1.insert(node4);

            // Assert that DHT1 contains itself and the 4 nodes that we just added
            List<Node> dht1Nodes = routingTable1.getAllNodes();
            assertEquals(5, dht1Nodes.size());
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(node1));
            assertTrue(dht1Nodes.contains(node2));
            assertTrue(dht1Nodes.contains(node3));
            assertTrue(dht1Nodes.contains(node4));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht2Nodes = routingTable2.getAllNodes();
            assertEquals(1, dht2Nodes.size());
            assertTrue(dht2Nodes.contains(dht2.getNode()));

            // Bootstrap DHT2 off of DHT1, then wait a moment for it to finish the bootstrap process
            dht2.bootstrap(dht1.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);

            // Assert that DHT2 now contains itself and all the nodes from DHT1
            dht2Nodes = routingTable2.getAllNodes();
            assertEquals(2, dht2Nodes.size());
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));

            // Assert that DHT1 now contains DHT2
            dht1Nodes = routingTable1.getAllNodes();
            assertEquals(6, dht1Nodes.size());
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }

    }


    @Test
    public void testBootstrapMultipleNodeInAChain() throws Exception {

        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        int port3 = getPortNumber();
        int port4 = getPortNumber();
        int port5 = getPortNumber();
        String key = "Key";
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";
        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair3 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair4 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair5 = CryptoUtils.generateECKeyPair();

        Random random = new Random();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);
        DHT dht3 = new DHT(port3, keyPair3, configuration);
        DHT dht4 = new DHT(port4, keyPair4, configuration);
        DHT dht5 = new DHT(port5, keyPair5, configuration);

        try {
            // Prepopulate the dht1 Routing Table with Nodes
            RoutingTable routingTable1 = dht1._dhtComponents.getRoutingTable();
            RoutingTable routingTable2 = dht2._dhtComponents.getRoutingTable();
            RoutingTable routingTable3 = dht3._dhtComponents.getRoutingTable();
            RoutingTable routingTable4 = dht4._dhtComponents.getRoutingTable();
            RoutingTable routingTable5 = dht5._dhtComponents.getRoutingTable();

//        Node node1 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
//        Node node2 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
//        Node node3 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
//        Node node4 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
//        routingTable1.insert(node1);
//        routingTable1.insert(node2);
//        routingTable1.insert(node3);
//        routingTable1.insert(node4);

            // Assert that DHT1 contains itself and the 4 nodes that we just added
            List<Node> dht1Nodes = routingTable1.getAllNodes();
            assertEquals(1, dht1Nodes.size());
            assertTrue(dht1Nodes.contains(dht1.getNode()));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht2Nodes = routingTable2.getAllNodes();
            assertEquals(1, dht2Nodes.size());
            assertTrue(dht2Nodes.contains(dht2.getNode()));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht3Nodes = routingTable3.getAllNodes();
            assertEquals(1, dht3Nodes.size());
            assertTrue(dht3Nodes.contains(dht3.getNode()));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht4Nodes = routingTable4.getAllNodes();
            assertEquals(1, dht4Nodes.size());
            assertTrue(dht4Nodes.contains(dht4.getNode()));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht5Nodes = routingTable5.getAllNodes();
            assertEquals(1, dht5Nodes.size());
            assertTrue(dht5Nodes.contains(dht5.getNode()));

            // Bootstrap DHT2 off of DHT1, then wait a moment for it to finish the bootstrap process
            dht2.bootstrap(dht1.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);

            // Assert that DHT2 now contains itself and all the nodes from DHT1
            dht2Nodes = routingTable2.getAllNodes();
            assertEquals(2, dht2Nodes.size());
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));

            // Assert that DHT1 now contains DHT2
            dht1Nodes = routingTable1.getAllNodes();
            assertEquals(2, dht1Nodes.size());
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));

            // Bootstrap DHT3 off of DHT2, then wait a moment for it to finish the bootstrap process
            dht3.bootstrap(dht2.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);
            System.out.println("DHT3: " + routingTable3);

            // Assert that DHT3 now contains itself, DHT2, and DHT1
            dht3Nodes = routingTable3.getAllNodes();
            assertTrue(dht3Nodes.contains(dht1.getNode()));
            assertTrue(dht3Nodes.contains(dht2.getNode()));
            assertTrue(dht3Nodes.contains(dht3.getNode()));
            assertEquals(3, dht3Nodes.size());

            // Assert that DHT2 now contains itself, DHT1, and DHT3
            dht2Nodes = routingTable2.getAllNodes();
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));
            assertTrue(dht2Nodes.contains(dht3.getNode()));
            assertEquals(3, dht2Nodes.size());

            // Assert that DHT1 now contains itself, DHT2, but not DHT3
            dht1Nodes = routingTable1.getAllNodes();
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));
            assertTrue(dht1Nodes.contains(dht3.getNode()));
            assertEquals(3, dht1Nodes.size());

            // Bootstrap DHT4 off of DHT3, then wait a moment for it to finish the bootstrap process
            dht4.bootstrap(dht3.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);
            System.out.println("DHT3: " + routingTable3);
            System.out.println("DHT4: " + routingTable4);

            // Assert that DHT4 now contains itself, DHT3, DHT2, and DHT1
            dht4Nodes = routingTable4.getAllNodes();
            assertTrue(dht4Nodes.contains(dht1.getNode()));
            assertTrue(dht4Nodes.contains(dht2.getNode()));
            assertTrue(dht4Nodes.contains(dht3.getNode()));
            assertTrue(dht4Nodes.contains(dht4.getNode()));
            assertEquals(4, dht4Nodes.size());

            // Assert that DHT3 now contains itself, DHT3, DHT2, and DHT1
            dht3Nodes = routingTable3.getAllNodes();
            assertTrue(dht3Nodes.contains(dht1.getNode()));
            assertTrue(dht3Nodes.contains(dht2.getNode()));
            assertTrue(dht3Nodes.contains(dht3.getNode()));
            assertTrue(dht3Nodes.contains(dht4.getNode()));
            assertEquals(4, dht3Nodes.size());

            // Assert that DHT2 now contains itself, DHT1, DHT3, but not DHT4
            dht2Nodes = routingTable2.getAllNodes();
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));
            assertTrue(dht2Nodes.contains(dht3.getNode()));
            assertTrue(dht2Nodes.contains(dht4.getNode()));
            assertEquals(4, dht2Nodes.size());

            // Assert that DHT1 now contains itself, DHT2, but not DHT3 or DHT4
            dht1Nodes = routingTable1.getAllNodes();
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));
            assertTrue(dht1Nodes.contains(dht3.getNode()));
            assertTrue(dht1Nodes.contains(dht4.getNode()));
            assertEquals(4, dht1Nodes.size());

            // Bootstrap DHT5 off of DHT4, then wait a moment for it to finish the bootstrap process
            dht5.bootstrap(dht4.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);
            System.out.println("DHT3: " + routingTable3);
            System.out.println("DHT4: " + routingTable4);
            System.out.println("DHT5: " + routingTable5);

            // Assert that DHT5 now contains itself, DHT4, DHT3, DHT2, and DHT1
            dht5Nodes = routingTable5.getAllNodes();
            assertTrue(dht5Nodes.contains(dht1.getNode()));
            assertTrue(dht5Nodes.contains(dht2.getNode()));
            assertTrue(dht5Nodes.contains(dht3.getNode()));
            assertTrue(dht5Nodes.contains(dht4.getNode()));
            assertTrue(dht5Nodes.contains(dht5.getNode()));
            assertEquals(5, dht5Nodes.size());

            // Assert that DHT4 now contains itself, DHT4, DHT3, DHT2, and DHT1
            dht4Nodes = routingTable4.getAllNodes();
            assertTrue(dht4Nodes.contains(dht1.getNode()));
            assertTrue(dht4Nodes.contains(dht2.getNode()));
            assertTrue(dht4Nodes.contains(dht3.getNode()));
            assertTrue(dht4Nodes.contains(dht4.getNode()));
            assertTrue(dht4Nodes.contains(dht5.getNode()));
            assertEquals(5, dht4Nodes.size());

            // Assert that DHT3 now contains itself, DHT3, DHT2, DHT1, but not DHT5
            dht3Nodes = routingTable3.getAllNodes();
            assertTrue(dht3Nodes.contains(dht1.getNode()));
            assertTrue(dht3Nodes.contains(dht2.getNode()));
            assertTrue(dht3Nodes.contains(dht3.getNode()));
            assertTrue(dht3Nodes.contains(dht4.getNode()));
            assertTrue(dht3Nodes.contains(dht5.getNode()));
            assertEquals(5, dht3Nodes.size());

            // Assert that DHT2 now contains itself, DHT1, DHT3, but not DHT4, or DHT5
            dht2Nodes = routingTable2.getAllNodes();
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));
            assertTrue(dht2Nodes.contains(dht3.getNode()));
            assertTrue(dht2Nodes.contains(dht4.getNode()));
            assertTrue(dht2Nodes.contains(dht5.getNode()));
            assertEquals(5, dht2Nodes.size());

            // Assert that DHT1 now contains itself, DHT2, but not DHT3, DHT4, or DHT5
            dht1Nodes = routingTable1.getAllNodes();
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));
            assertTrue(dht1Nodes.contains(dht3.getNode()));
            assertTrue(dht1Nodes.contains(dht4.getNode()));
            assertTrue(dht1Nodes.contains(dht5.getNode()));
            assertEquals(5, dht1Nodes.size());
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
            dht3.shutdown(false);
            dht4.shutdown(false);
            dht5.shutdown(false);
        }
    }

    @Test
    public void testBootstrapMultipleNodeFromSingleNode() throws Exception {

        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);
        configuration.setK(160);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        int port3 = getPortNumber();
        int port4 = getPortNumber();
        int port5 = getPortNumber();
        String key = "Key";
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";
        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair3 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair4 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair5 = CryptoUtils.generateECKeyPair();

        Random random = new Random();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);
        DHT dht3 = new DHT(port3, keyPair3, configuration);
        DHT dht4 = new DHT(port4, keyPair4, configuration);
        DHT dht5 = new DHT(port5, keyPair5, configuration);

        try {
            // Prepopulate the dht1 Routing Table with Nodes
            RoutingTable routingTable1 = dht1._dhtComponents.getRoutingTable();
            RoutingTable routingTable2 = dht2._dhtComponents.getRoutingTable();
            RoutingTable routingTable3 = dht3._dhtComponents.getRoutingTable();
            RoutingTable routingTable4 = dht4._dhtComponents.getRoutingTable();
            RoutingTable routingTable5 = dht5._dhtComponents.getRoutingTable();

//        Node node1 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
//        Node node2 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
//        Node node3 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
//        Node node4 = new Node(nodeIDGenerator.generateNodeID(), "localhost", random.nextInt(65535));
//        routingTable1.insert(node1);
//        routingTable1.insert(node2);
//        routingTable1.insert(node3);
//        routingTable1.insert(node4);

            // Assert that DHT1 contains itself and the 4 nodes that we just added
            List<Node> dht1Nodes = routingTable1.getAllNodes();
            assertEquals(1, dht1Nodes.size());
            assertTrue(dht1Nodes.contains(dht1.getNode()));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht2Nodes = routingTable2.getAllNodes();
            assertEquals(1, dht2Nodes.size());
            assertTrue(dht2Nodes.contains(dht2.getNode()));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht3Nodes = routingTable3.getAllNodes();
            assertEquals(1, dht3Nodes.size());
            assertTrue(dht3Nodes.contains(dht3.getNode()));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht4Nodes = routingTable4.getAllNodes();
            assertEquals(1, dht4Nodes.size());
            assertTrue(dht4Nodes.contains(dht4.getNode()));

            // Assert that DHT2 only contains one node, itself
            List<Node> dht5Nodes = routingTable5.getAllNodes();
            assertEquals(1, dht5Nodes.size());
            assertTrue(dht5Nodes.contains(dht5.getNode()));

            // Bootstrap DHT2 off of DHT1, then wait a moment for it to finish the bootstrap process
            dht2.bootstrap(dht1.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);

            // Assert that DHT2 now contains itself and all the nodes from DHT1
            dht2Nodes = routingTable2.getAllNodes();
            assertEquals(2, dht2Nodes.size());
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));

            // Assert that DHT1 now contains DHT2
            dht1Nodes = routingTable1.getAllNodes();
            assertEquals(2, dht1Nodes.size());
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));

            // Bootstrap DHT3 off of DHT1, then wait a moment for it to finish the bootstrap process
            dht3.bootstrap(dht1.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);
            System.out.println("DHT3: " + routingTable3);

            // Assert that DHT3 now contains itself, DHT2, and DHT1
            dht3Nodes = routingTable3.getAllNodes();
            assertTrue(dht3Nodes.contains(dht1.getNode()));
            assertTrue(dht3Nodes.contains(dht2.getNode()));
            assertTrue(dht3Nodes.contains(dht3.getNode()));
            assertEquals(3, dht3Nodes.size());

            // Assert that DHT2 now contains itself, DHT1, and DHT3
            dht2Nodes = routingTable2.getAllNodes();
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));
            assertTrue(dht2Nodes.contains(dht3.getNode()));
            assertEquals(3, dht2Nodes.size());

            // Assert that DHT1 now contains itself, DHT2, but not DHT3
            dht1Nodes = routingTable1.getAllNodes();
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));
            assertTrue(dht1Nodes.contains(dht3.getNode()));
            assertEquals(3, dht1Nodes.size());

            // Bootstrap DHT4 off of DHT1, then wait a moment for it to finish the bootstrap process
            dht4.bootstrap(dht1.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);
            System.out.println("DHT3: " + routingTable3);
            System.out.println("DHT4: " + routingTable4);

            // Assert that DHT4 now contains itself, DHT3, DHT2, and DHT1
            dht4Nodes = routingTable4.getAllNodes();
            assertTrue(dht4Nodes.contains(dht1.getNode()));
            assertTrue(dht4Nodes.contains(dht2.getNode()));
            assertTrue(dht4Nodes.contains(dht3.getNode()));
            assertTrue(dht4Nodes.contains(dht4.getNode()));
            assertEquals(4, dht4Nodes.size());

            // Assert that DHT3 now contains itself, DHT3, DHT2, and DHT1
            dht3Nodes = routingTable3.getAllNodes();
            assertTrue(dht3Nodes.contains(dht1.getNode()));
            assertTrue(dht3Nodes.contains(dht2.getNode()));
            assertTrue(dht3Nodes.contains(dht3.getNode()));
            assertTrue(dht3Nodes.contains(dht4.getNode()));
            assertEquals(4, dht3Nodes.size());

            // Assert that DHT2 now contains itself, DHT1, DHT3, but not DHT4
            dht2Nodes = routingTable2.getAllNodes();
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));
            assertTrue(dht2Nodes.contains(dht3.getNode()));
            assertTrue(dht2Nodes.contains(dht4.getNode()));
            assertEquals(4, dht2Nodes.size());

            // Assert that DHT1 now contains itself, DHT2, but not DHT3 or DHT4
            dht1Nodes = routingTable1.getAllNodes();
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));
            assertTrue(dht1Nodes.contains(dht3.getNode()));
            assertTrue(dht1Nodes.contains(dht4.getNode()));
            assertEquals(4, dht1Nodes.size());

            // Bootstrap DHT5 off of DHT1, then wait a moment for it to finish the bootstrap process
            dht5.bootstrap(dht1.getNode());
            Thread.sleep(2500);

            System.out.println("DHT1: " + routingTable1);
            System.out.println("DHT2: " + routingTable2);
            System.out.println("DHT3: " + routingTable3);
            System.out.println("DHT4: " + routingTable4);
            System.out.println("DHT5: " + routingTable5);

            // Assert that DHT5 now contains itself, DHT4, DHT3, DHT2, and DHT1
            dht5Nodes = routingTable5.getAllNodes();
            assertTrue(dht5Nodes.contains(dht1.getNode()));
            assertTrue(dht5Nodes.contains(dht2.getNode()));
            assertTrue(dht5Nodes.contains(dht3.getNode()));
            assertTrue(dht5Nodes.contains(dht4.getNode()));
            assertTrue(dht5Nodes.contains(dht5.getNode()));
            assertEquals(5, dht5Nodes.size());

            // Assert that DHT4 now contains itself, DHT4, DHT3, DHT2, and DHT1
            dht4Nodes = routingTable4.getAllNodes();
            assertTrue(dht4Nodes.contains(dht1.getNode()));
            assertTrue(dht4Nodes.contains(dht2.getNode()));
            assertTrue(dht4Nodes.contains(dht3.getNode()));
            assertTrue(dht4Nodes.contains(dht4.getNode()));
            assertTrue(dht4Nodes.contains(dht5.getNode()));
            assertEquals(5, dht4Nodes.size());

            // Assert that DHT3 now contains itself, DHT3, DHT2, DHT1, but not DHT5
            dht3Nodes = routingTable3.getAllNodes();
            assertTrue(dht3Nodes.contains(dht1.getNode()));
            assertTrue(dht3Nodes.contains(dht2.getNode()));
            assertTrue(dht3Nodes.contains(dht3.getNode()));
            assertTrue(dht3Nodes.contains(dht4.getNode()));
            assertTrue(dht3Nodes.contains(dht5.getNode()));
            assertEquals(5, dht3Nodes.size());

            // Assert that DHT2 now contains itself, DHT1, DHT3, but not DHT4, or DHT5
            dht2Nodes = routingTable2.getAllNodes();
            assertTrue(dht2Nodes.contains(dht1.getNode()));
            assertTrue(dht2Nodes.contains(dht2.getNode()));
            assertTrue(dht2Nodes.contains(dht3.getNode()));
            assertTrue(dht2Nodes.contains(dht4.getNode()));
            assertTrue(dht2Nodes.contains(dht5.getNode()));
            assertEquals(5, dht2Nodes.size());

            // Assert that DHT1 now contains itself, DHT2, but not DHT3, DHT4, or DHT5
            dht1Nodes = routingTable1.getAllNodes();
            assertTrue(dht1Nodes.contains(dht1.getNode()));
            assertTrue(dht1Nodes.contains(dht2.getNode()));
            assertTrue(dht1Nodes.contains(dht3.getNode()));
            assertTrue(dht1Nodes.contains(dht4.getNode()));
            assertTrue(dht1Nodes.contains(dht5.getNode()));
            assertEquals(5, dht1Nodes.size());
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
            dht3.shutdown(false);
            dht4.shutdown(false);
            dht5.shutdown(false);
        }
    }


    @Test
    public void testBootstrapAndRefreshNode() throws Exception {

        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";
        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        Random random = new Random();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        try {

            dht2.bootstrap(dht1.getNode());

            Thread.sleep(2500);

            dht2.refresh();
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }

    }

    @Test
    public void testBootstrapWithNonExistentNode() throws Exception {
        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);

        NodeID randomNode = nodeIDGenerator.generateNodeID();
        Node nonexistentNode = new Node(randomNode, "localhost", 40000);

        try {
            dht1.bootstrap(nonexistentNode);
            fail ( "Expected IOException to be thrown" ) ;
        } catch ( IOException e) {
            // NOOP - Expected Exception
        }

        assertFalse ( dht1.isRunning() );
    }

    @Test
    public void testStartAlreadyRunningDHT() throws Exception {
        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);

        NodeID randomNode = nodeIDGenerator.generateNodeID();
        Node nonexistentNode = new Node(randomNode, "localhost", 40000);

        try {
            dht1.start(false);

            Thread.sleep ( 250) ;
            assertTrue ( dht1.isRunning() ) ;

            try {
                dht1.start(false);
                fail("Expected IllegalStateException to be thrown");
            } catch (IllegalStateException e) {
                // NOOP - Expected Exception
            }
        } finally {
            dht1.shutdown(false);
        }

        assertFalse ( dht1.isRunning() );
    }
}
