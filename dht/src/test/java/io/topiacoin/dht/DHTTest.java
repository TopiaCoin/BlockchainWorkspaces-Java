package io.topiacoin.dht;

import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class DHTTest {

    @Test
    public void testStoreValue() throws Exception {

        int port = 33456;
        String key = "Key" ;
        String value = "HarryPotterAndTheGuyWhoLooksLikeASnake";
        KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();

        DHT dht = new DHT(port, keyPair) ;

        dht.storeContent(key, value) ;
    }

}
