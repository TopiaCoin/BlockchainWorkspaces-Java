package io.topiacoin.dht;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeIDGenerator;
import io.topiacoin.dht.routing.RoutingTable;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

public class DHTTest {

    @Test
    public void testStoreValue() throws Exception {

        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = 33444;
        int port2 = 33445;
        String key = "Key";
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";

        KeyPairGenerator ecGenerator = KeyPairGenerator.getInstance("EC");
        KeyPair keyPair1 = ecGenerator.generateKeyPair();
        KeyPair keyPair2 = ecGenerator.generateKeyPair();

        Random random = new Random();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        try {
            // Bootstrap our DHT against the other DHT
            dht1.bootstrap(dht2.getNode());

            Thread.sleep(2500);

            int storeCount = dht1.storeContent(key, value);

            assertTrue(storeCount > 0);
            assertEquals("Expected the value to be stored in 2 nodes", 2, storeCount);

            Thread.sleep(1000);

            Set<String> fetchedValues = dht1.fetchContent(key);

            assertNotNull("No Values Fetched", fetchedValues);
            assertTrue("Fetched Values does not contain stored value", fetchedValues.contains(value));
            assertEquals(1, fetchedValues.size());


            // Remove the value for the key
            dht1.removeContent(key, value) ;

            Thread.sleep(1000) ;


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

        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = 33444;
        int port2 = 33445;
        String key = "Key";
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";

        KeyPairGenerator ecGenerator = KeyPairGenerator.getInstance("EC");
        KeyPair keyPair1 = ecGenerator.generateKeyPair();
        KeyPair keyPair2 = ecGenerator.generateKeyPair();

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);

        try {
            // Bootstrap our DHT against the other DHT
            dht1.bootstrap(dht2.getNode());

            Thread.sleep(2500);

            int storeCount = dht1.storeContent(key, value);

            assertTrue(storeCount > 0);
            assertEquals("Expected the value to be stored in 2 nodes", 2, storeCount);

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

            Thread.sleep ( 2500 ) ;

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
    public void testStoreValueWithMultipleServers() throws Exception {

        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        int port1 = 33451;
        int port2 = 33452;
        int port3 = 33453;
        int port4 = 33454;
        int port5 = 33455;
        String key = "Key";
        String value = "HarryPotterAndOlympicCauldron";

        KeyPairGenerator ecGenerator = KeyPairGenerator.getInstance("EC");
        KeyPair keyPair1 = ecGenerator.generateKeyPair();
        KeyPair keyPair2 = ecGenerator.generateKeyPair();
        KeyPair keyPair3 = ecGenerator.generateKeyPair();
        KeyPair keyPair4 = ecGenerator.generateKeyPair();
        KeyPair keyPair5 = ecGenerator.generateKeyPair();

        Random random = new Random();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        DHT dht1 = new DHT(port1, keyPair1, configuration);
        DHT dht2 = new DHT(port2, keyPair2, configuration);
        DHT dht3 = new DHT(port3, keyPair3, configuration);
        DHT dht4 = new DHT(port4, keyPair4, configuration);
        DHT dht5 = new DHT(port5, keyPair5, configuration);

        try {
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
            dht3.removeContent(key, value) ;

            Thread.sleep(1000) ;


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
}
