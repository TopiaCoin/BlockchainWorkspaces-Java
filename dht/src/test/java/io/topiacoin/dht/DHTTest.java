package io.topiacoin.dht;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class DHTTest {

    @Test
    public void testStoreValue() throws Exception {

        Configuration configuration = new DefaultConfiguration();

        int port = 33456;
        String key = "Key" ;
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";
        KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();

        DHT dht = new DHT(port, keyPair, configuration) ;

        dht.storeContent(key, value) ;
    }

}
