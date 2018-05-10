package io.topiacoin.dht.content;

import io.topiacoin.dht.intf.ValueStorage;
import org.junit.Test;

import java.util.Collection;

import static junit.framework.TestCase.*;

public class InMemoryExpiringValueStorageTest extends AbstractValueStorageTest {

    @Override
    protected ValueStorage getValueStorage() {

        InMemoryExpiringValueStorage valueStorage = new InMemoryExpiringValueStorage();

        valueStorage.initialize();

        return valueStorage;
    }


    @Override
    public void testSaveAndLoad() throws Exception {
        // NOOP - This test is disabled for this particular implementation
    }

    @Test
    public void testStoredDataExpires() throws Exception {

        int timeout = 250;
        String key = "Go" ;
        String value = "Fight" ;
        long expirationTime = System.currentTimeMillis() + timeout;

        InMemoryExpiringValueStorage valueStorage1 = new InMemoryExpiringValueStorage();

        valueStorage1.initialize();

        ValueStorage valueStorage = valueStorage1;

        valueStorage.setValue(key, value, expirationTime);

        Collection<String> values = valueStorage.getValues(key) ;

        assertNotNull ( values) ;
        assertTrue(values.contains(value)) ;

        Thread.sleep ( (int)(timeout * 1.1)) ;

        values = valueStorage.getValues(key) ;

        assertNotNull ( values) ;
        assertFalse(values.contains(value)) ;

    }

    @Test
    public void testStoredDataDoesNotExpireIfReAdded() throws Exception {

        int timeout = 250;
        String key = "Go" ;
        String value = "Fight" ;
        long expirationTime = System.currentTimeMillis() + timeout;

        InMemoryExpiringValueStorage valueStorage1 = new InMemoryExpiringValueStorage();

        valueStorage1.initialize();

        ValueStorage valueStorage = valueStorage1;

        // Set the value in the Value Storage
        valueStorage.setValue(key, value, expirationTime);

        // Verify that the value is still in the Value Storage
        Collection<String> values = valueStorage.getValues(key) ;

        assertNotNull ( values) ;
        assertTrue(values.contains(value)) ;

        Thread.sleep ( (int)(timeout * 0.6)) ;

        // We are just over half way through the expiration period.
        // Set the value in storage again to reset the counter.
        expirationTime = System.currentTimeMillis() + timeout;
        valueStorage.setValue(key, value, expirationTime);

        Thread.sleep ( (int)(timeout * 0.6)) ;

        // We are just over half way through the second expiration period.
        // Verify that the value is still in the Value Storage
        values = valueStorage.getValues(key) ;

        assertNotNull ( values) ;
        assertTrue(values.contains(value)) ;

        Thread.sleep ( (int)(timeout * 0.6)) ;

        // We are now beyond the second expiration period.
        // Verify that the value has been remove from the Value Storage
        values = valueStorage.getValues(key) ;

        assertNotNull ( values) ;
        assertFalse(values.contains(value)) ;
    }


    @Test
    public void testStoredDataExpiresIndependently() throws Exception {

        int timeout = 250;
        String key1 = "Go" ;
        String value1 = "Fight" ;
        String value2 = "Tonight" ;
        long expirationTime = System.currentTimeMillis() + timeout;

        InMemoryExpiringValueStorage valueStorage1 = new InMemoryExpiringValueStorage();

        valueStorage1.initialize();

        ValueStorage valueStorage = valueStorage1;

        // Set the value in the Value Storage
        valueStorage.setValue(key1, value1, expirationTime);
        valueStorage.setValue(key1, value2, expirationTime);

        // Verify that both values are still in the Value Storage
        Collection<String> values = valueStorage.getValues(key1) ;
        assertNotNull ( values) ;
        assertTrue(values.contains(value1)) ;
        assertTrue(values.contains(value2)) ;

        Thread.sleep ( (int)(timeout * 0.6)) ;

        // We are just over half way through the expiration period.
        // Set the first value in storage again to reset its counter.
        expirationTime = System.currentTimeMillis() + timeout;
        valueStorage.setValue(key1, value1, expirationTime);

        Thread.sleep ( (int)(timeout * 0.6)) ;

        // We are just over half way through the second expiration period.
        // Verify that the first value is still in the Value Storage, but the second has timed out
        values = valueStorage.getValues(key1) ;
        assertNotNull ( values) ;
        assertTrue(values.contains(value1)) ;
        assertFalse(values.contains(value2)) ;

        Thread.sleep ( (int)(timeout * 0.6)) ;

        // We are now beyond the second expiration period.
        // Verify that both values have been remove from the Value Storage
        values = valueStorage.getValues(key1) ;
        assertNotNull ( values) ;
        assertFalse(values.contains(value1)) ;
        assertFalse(values.contains(value2)) ;
    }}
