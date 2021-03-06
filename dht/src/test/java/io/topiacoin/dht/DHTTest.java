package io.topiacoin.dht;

import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.Ignore;
import org.junit.Test;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

public class DHTTest {

    public static int port = 33000;

    public static synchronized int getPortNumber() {
        return 0 ;
    }

    @Test
    public void testStoreValue() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();

        Random random = new Random();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        dht1.start(false);

        try {
            int storeCount = dht1.storeContent(key, value);

            assertTrue(storeCount > 0);
            assertEquals("Expected the value to be stored in 2 nodes", 1, storeCount);

            Thread.sleep(1000);

            Set<String> fetchedValues = dht1.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertTrue("Fetched Values does not contain stored value", fetchedValues.contains(value));
            assertEquals(1, fetchedValues.size());


            // Remove the value for the key
            dht1.removeContent(key, value);

            Thread.sleep(1000);


            // Fetch the key again and check that the value is not returned
            fetchedValues = dht1.fetchContent(key);

            System.out.println("fetchedValues: " + fetchedValues.size() + " -- " + fetchedValues);

            assertNotNull("No Values Fetched", fetchedValues);
            assertFalse("Stored Values should have been removed", fetchedValues.contains(value));
            assertEquals(0, fetchedValues.size());

        } finally {
            dht1.shutdown(false);
        }
    }

    @Test
    public void testStoreMultipleValues() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        String key = "Key";
        List<String> values = new ArrayList<String>();
        values.add( "HarryPotterAndMagicalRock");
        values.add( "HarryPotterAndSecretCave");
        values.add( "HarryPotterAndTheGuyWhoLooksLikeASnake");

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        dht1.start(false);

        try {
            for ( String value: values) {
                dht1.storeContent(key, value);
            }

            Thread.sleep(1000);

            Set<String> fetchedValues = dht1.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertEquals("Wrong number of values fetched", values.size(), fetchedValues.size());
            assertTrue("Fetched Values does not contain stored values", fetchedValues.containsAll(values));
        } finally {
            dht1.shutdown(false);
        }
    }

    @Test
    public void testStoredValuesExpire() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);
        configuration.setEntryExpirationTime(250);

        int port1 = getPortNumber();
        String key = "Key";
        List<String> values = new ArrayList<String>();
        values.add( "HarryPotterAndMagicalRock");
        values.add( "HarryPotterAndSecretCave");
        values.add( "HarryPotterAndTheGuyWhoLooksLikeASnake");

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        dht1.start(false);

        try {
            for ( String value: values) {
                dht1.storeContent(key, value);
            }

            Thread.sleep(1000);

            Set<String> fetchedValues = dht1.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertEquals("Wrong number of values fetched", 0, fetchedValues.size());
        } finally {
            dht1.shutdown(false);
        }
    }

    @Test
    public void testStoredValuesExpireUnlessStoredAgain() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);
        configuration.setEntryExpirationTime(1500);

        int port1 = getPortNumber();
        String key = "Key";
        List<String> values = new ArrayList<String>();
        values.add( "HarryPotterAndMagicalRock");
        values.add( "HarryPotterAndSecretCave");
        values.add( "HarryPotterAndTheGuyWhoLooksLikeASnake");

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        dht1.start(false);

        try {
            for ( String value: values) {
                dht1.storeContent(key, value);
            }

            Thread.sleep(1000);

            // Store the first value again
            dht1.storeContent(key, values.get(0));

            Thread.sleep(1000);

            Set<String> fetchedValues = dht1.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertEquals("Wrong number of values fetched", 1, fetchedValues.size());
            assertEquals("Expected Value not Found", values.get(0), fetchedValues.iterator().next());
        } finally {
            dht1.shutdown(false);
        }
    }

    @Test
    public void testStoreMultipleValuesFromMultipleDHTs() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        List<String> values1 = new ArrayList<String>();
        values1.add( "HarryPotterAndMagicalRock");
        values1.add( "HarryPotterAndSecretCave");
        values1.add( "HarryPotterAndTheGuyWhoLooksLikeASnake");

        List<String> values2 = new ArrayList<String>();
        values2.add( "HarryPotterAndEscapedPrisoner");
        values2.add( "HarryPotterAndStuffThatWillKillYou");
        values2.add( "HarryPotterAndPrinceWhoIsn'tAPrince");

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        dht1.start(false);
        dht2.bootstrap(dht1.getNode());

        try {
            for ( String value: values1) {
                dht1.storeContent(key, value);
            }

            for ( String value: values2) {
                dht2.storeContent(key, value);
            }

            Thread.sleep(1000);

            Set<String> fetchedValues = dht1.fetchContent(key);

            List<String> allValues = new ArrayList<String>();
            allValues.addAll(values1);
            allValues.addAll(values2) ;

            assertNotNull("No Values Fetched", fetchedValues);
            assertEquals("Wrong number of values fetched", allValues.size(), fetchedValues.size());
            assertTrue("Fetched Values does not contain stored values", fetchedValues.containsAll(allValues));
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }
    }

    @Test
    public void testStoreMultipleValuesFromMultipleDHTsExpire() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);
        configuration.setEntryExpirationTime(500);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        List<String> values1 = new ArrayList<String>();
        values1.add( "HarryPotterAndMagicalRock");
        values1.add( "HarryPotterAndSecretCave");
        values1.add( "HarryPotterAndTheGuyWhoLooksLikeASnake");

        List<String> values2 = new ArrayList<String>();
        values2.add( "HarryPotterAndEscapedPrisoner");
        values2.add( "HarryPotterAndStuffThatWillKillYou");
        values2.add( "HarryPotterAndPrinceWhoIsn'tAPrince");

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        dht1.start(false);
        dht2.bootstrap(dht1.getNode());

        try {
            for ( String value: values1) {
                dht1.storeContent(key, value);
            }

            for ( String value: values2) {
                dht2.storeContent(key, value);
            }

            Thread.sleep(1000);

            Set<String> fetchedValues = dht1.fetchContent(key);

            List<String> allValues = new ArrayList<String>();
            allValues.addAll(values1);
            allValues.addAll(values2) ;

            assertNotNull("No Values Fetched", fetchedValues);
            assertEquals("Wrong number of values fetched", 0, fetchedValues.size());
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }
    }

    @Test
    public void testStoreMultipleValuesFromMultipleDHTsExpiresUnlessStoredAgain() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);
        configuration.setEntryExpirationTime(3000);
        configuration.setRestoreInterval(500);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        List<String> values1 = new ArrayList<String>();
        values1.add( "HarryPotterAndMagicalRock");
        values1.add( "HarryPotterAndSecretCave");
        values1.add( "HarryPotterAndTheGuyWhoLooksLikeASnake");

        List<String> values2 = new ArrayList<String>();
        values2.add( "HarryPotterAndEscapedPrisoner");
        values2.add( "HarryPotterAndStuffThatWillKillYou");
        values2.add( "HarryPotterAndPrinceWhoIsn'tAPrince");

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        dht1.start(false);
        dht2.bootstrap(dht1.getNode());

        System.out.println ( "Storing Values" ) ;

        try {
            for ( String value: values1) {
                dht1.storeContent(key, value);
            }

            for ( String value: values2) {
                dht2.storeContent(key, value);
            }

            Thread.sleep(2000);

            System.out.println ( "Storing Some Values Again" ) ;

            dht1.storeContent(key, values1.get(0));
            dht2.storeContent(key, values2.get(0));

            Thread.sleep ( 2000 );

            System.out.println ( "Fetching Values" ) ;

            Set<String> fetchedValues = dht1.fetchContent(key);

            List<String> allValues = new ArrayList<String>();
            allValues.addAll(values1);
            allValues.addAll(values2) ;

            assertNotNull("No Values Fetched", fetchedValues);
            assertEquals("Wrong number of values fetched", 2, fetchedValues.size());
            assertTrue("Fetched Values does not contain stored values", fetchedValues.contains(values1.get(0)));
            assertTrue("Fetched Values does not contain stored values", fetchedValues.contains(values2.get(0)));
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }
    }

    @Test
    public void testStoreMultipleValuesThenAddNewDHT() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        List<String> values1 = new ArrayList<String>();
        values1.add( "HarryPotterAndMagicalRock");
        values1.add( "HarryPotterAndSecretCave");
        values1.add( "HarryPotterAndTheGuyWhoLooksLikeASnake");

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        dht1.start(false);

        try {
            for ( String value: values1) {
                dht1.storeContent(key, value);
            }

            dht2.bootstrap(dht1.getNode());

            Thread.sleep(1000);

            Set<String> fetchedValues = dht2.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertEquals("Wrong number of values fetched", values1.size(), fetchedValues.size());
            assertTrue("Fetched Values does not contain stored values", fetchedValues.containsAll(values1));
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }
    }


    @Test
    public void testStoreValueWithSecondDHT() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
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
            dht2.start(false);

            // Bootstrap our DHT against the other DHT
            dht1.bootstrap(dht2.getNode());

            Thread.sleep(2500);

            int storeCount = dht1.storeContent(key, value);

            assertTrue(storeCount > 0);

            Thread.sleep(1000);

            Set<String> fetchedValues = dht1.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertTrue("Fetched Values does not contain stored value", fetchedValues.contains(value));
            assertEquals(1, fetchedValues.size());


            // Remove the value for the key
            dht1.removeContent(key, value);

            Thread.sleep(1000);


            // Fetch the key again and check that the value is not returned
            fetchedValues = dht1.fetchContent(key);

            System.out.println("fetchedValues: " + fetchedValues.size() + " -- " + fetchedValues);

            assertNotNull("No Values Fetched", fetchedValues);
            assertFalse("Stored Values should have been removed", fetchedValues.contains(value));
            assertEquals(0, fetchedValues.size());

        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }
    }

    @Test
    public void testContentRefresh() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        try {
            dht2.start(false);

            // Bootstrap our DHT against the other DHT
            dht1.bootstrap(dht2.getNode());

            Thread.sleep(2500);

            int storeCount = dht1.storeContent(key, value);

            assertTrue(storeCount > 0);

            Thread.sleep(1000);

            // Make sure the content is present in DHT1
            Set<String> fetchedValues = dht1.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertTrue("Fetched Values does not contain stored value", fetchedValues.contains(value));
            assertEquals(1, fetchedValues.size());

            // Make sure the content is present in DHT2
            fetchedValues = dht2.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertTrue("Fetched Values does not contain stored value", fetchedValues.contains(value));
            assertEquals(1, fetchedValues.size());


            // Initiate a Refresh of the DHT
            dht1.refresh();

            Thread.sleep(2500);

            // Make sure the content is still present in DHT1
            fetchedValues = dht1.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertTrue("Fetched Values does not contain stored value", fetchedValues.contains(value));
            assertEquals(1, fetchedValues.size());

            // Make sure the content is still present in DHT2
            fetchedValues = dht2.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertTrue("Fetched Values does not contain stored value", fetchedValues.contains(value));
            assertEquals(1, fetchedValues.size());
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }
    }

    @Test
    public void testContentRefreshWithMultipleValues() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        String key = "Key";
        List<String> values1 = new ArrayList<String>();
        values1.add( "HarryPotterAndMagicalRock");
        values1.add( "HarryPotterAndSecretCave");
        values1.add( "HarryPotterAndTheGuyWhoLooksLikeASnake");

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        dht1.start(false);

        try {
            for ( String value: values1) {
                dht1.storeContent(key, value);
            }

            dht2.bootstrap(dht1.getNode());

            Thread.sleep(1000);

            dht1.refresh();

            Thread.sleep(1000);

            Set<String> fetchedValues = dht2.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertEquals("Wrong number of values fetched", values1.size(), fetchedValues.size());
            assertTrue("Fetched Values does not contain stored values", fetchedValues.containsAll(values1));
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }
    }

    @Test
    public void testStoreValueWithMultipleServers() throws Exception {

        DHTConfiguration configuration = new DHTTestConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = getPortNumber();
        int port2 = getPortNumber();
        int port3 = getPortNumber();
        int port4 = getPortNumber();
        int port5 = getPortNumber();
        String key = "Key";
        String value = "HarryPotterAndOlympicCauldron";

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
            dht1.start(false);

            // Bootstrap our DHT against the other DHT
            dht2.bootstrap(dht1.getNode());
            dht3.bootstrap(dht1.getNode());
            dht4.bootstrap(dht1.getNode());
            dht5.bootstrap(dht1.getNode());

            Thread.sleep(2500);

            dht1.refresh();
            dht2.refresh();
            dht3.refresh();
            dht4.refresh();
            dht5.refresh();

            Thread.sleep(2500);

            int storeCount = dht1.storeContent(key, value);

            assertTrue(storeCount > 0);

            Thread.sleep(1000);

            Set<String> fetchedValues = dht2.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertTrue("Fetched Values does not contain stored value", fetchedValues.contains(value));
            assertEquals(1, fetchedValues.size());

            // Remove the value for the key
            dht3.removeContent(key, value);

            Thread.sleep(1000);


            // Fetch the key again and check that the value is not returned
            fetchedValues = dht4.fetchContent(key);

            System.out.println("fetchedValues: " + fetchedValues.size() + " -- " + fetchedValues);

            assertNotNull("No Values Fetched", fetchedValues);
            assertFalse("Stored Values should have been removed", fetchedValues.contains(value));
            assertEquals(0, fetchedValues.size());
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
            dht3.shutdown(false);
            dht4.shutdown(false);
            dht5.shutdown(false);
        }
    }


    @Ignore
    @Test
    public void testSaveAndRestoreStateWithFullRoutingTable() throws Exception {
        DHTConfiguration configuration1 = new DHTTestConfiguration();
        configuration1.setC1(4);
        configuration1.setC2(8);
        configuration1.setNodeDataFolder("./target/node1");

        int port1 = getPortNumber();
        int port2 = getPortNumber();

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration1);
        DHT dht2 = new DHT(port2, keyPair1, configuration1);
        try {

//        dht1.start(false);

            // Load the DHT up with a bunch of routing info (Note that these NodeIDs are not actually valid for Routing)
            for (int i = 0; i < 160 * 40; i++) {
                NodeID nodeID = dht1.getNode().getNodeID().generateNodeIDByDistance(i % 161);
                int port = 33000 + i;

                Node node = new Node(nodeID, "::1", port);
                dht1._dhtComponents.getRoutingTable().insert(node);
            }
            List<Node> insertedNodes = dht1._dhtComponents.getRoutingTable().getAllNodes();

            System.out.println(dht1._dhtComponents.getRoutingTable());

            // Shutdown the DHT and save its state
            dht1.shutdown(true);

            // Instantiate a new DHT and load its state from the previous DHT's saved state
            dht2.start();

            System.out.println(dht2._dhtComponents.getRoutingTable());

            // Verify that DHT2 is able to fetch the stored content
            List<Node> restoredNodes = dht2._dhtComponents.getRoutingTable().getAllNodes();

            assertEquals(insertedNodes.size(), restoredNodes.size());
            assertTrue(restoredNodes.containsAll(insertedNodes));
        } finally {
            dht1.shutdown(false) ;
            dht2.shutdown(false);
        }
    }

    @Test
    public void testRefreshTimer() throws Exception {
        // NOTE:  This test has no assertions as it is not currently possible to verify that the /
        // periodic refresh has actually executed.
        DHTConfiguration configuration1 = new DHTTestConfiguration();
        configuration1.setC1(4);
        configuration1.setC2(8);
        configuration1.setRestoreInterval(500);

        int port1 = getPortNumber();

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration1);

        try {
            dht1.start(false);

            Thread.sleep(1500);
        } finally {
            dht1.shutdown(false);
        }
    }

    @Test
    public void testRefreshTimerWithMultipleNodes() throws Exception {
        // NOTE:  This test has no assertions as it is not currently possible to verify that the /
        // periodic refresh has actually executed.
        DHTConfiguration configuration1 = new DHTTestConfiguration();
        configuration1.setC1(4);
        configuration1.setC2(8);
        configuration1.setRestoreInterval(500);

        int port1 = getPortNumber();
        int port2 = getPortNumber();

        KeyPair keyPair1 = CryptoUtils.generateECKeyPair();
        KeyPair keyPair2 = CryptoUtils.generateECKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration1);
        DHT dht2 = new DHT(port2, keyPair2, configuration1);

        try {
            dht1.start(false);
            dht2.bootstrap(dht1.getNode());

            Thread.sleep(1500);
        } finally {
            dht1.shutdown(false);
            dht2.shutdown(false);
        }
    }
}
