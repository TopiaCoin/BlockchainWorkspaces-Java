package io.topiacoin.dht.config;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import org.junit.Test;

import static org.junit.Assert.*;

public class DHTTConfigurationTest {

    @Test
    public void testDHTConfigurationDefaults() {
        Configuration coreConfiguration = new DefaultConfiguration() ;
        DHTConfiguration dhtConfiguration = new DHTConfiguration(coreConfiguration) ;

        int k = dhtConfiguration.getK() ;

        assertTrue (k > 0 );

        assertEquals( 20, k);
    }
}
