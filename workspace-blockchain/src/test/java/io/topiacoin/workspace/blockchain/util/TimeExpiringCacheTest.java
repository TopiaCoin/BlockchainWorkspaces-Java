package io.topiacoin.workspace.blockchain.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class TimeExpiringCacheTest {

    @Test
    public void testEntryExpiration() throws Exception {
        TimeExpiringCache<String, String> expiringCache = new TimeExpiringCache<>(75);

        String key = "foo";
        String value = "bar";
        String fetchedValue = null ;

        fetchedValue = expiringCache.get(key);
        assertNull ( fetchedValue) ;

        expiringCache.put(key, value);

        fetchedValue = expiringCache.get(key);
        assertNotNull ( fetchedValue) ;

        Thread.sleep(25) ;

        fetchedValue = expiringCache.get(key);
        assertNotNull ( fetchedValue) ;

        Thread.sleep(75) ;

        fetchedValue = expiringCache.get(key);
        assertNull ( fetchedValue) ;

        expiringCache.put(key, value, 10) ;

        fetchedValue = expiringCache.get(key);
        assertNotNull ( fetchedValue) ;

        Thread.sleep(25) ;

        fetchedValue = expiringCache.get(key);
        assertNull ( fetchedValue) ;
    }
}
