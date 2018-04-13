package io.topiacoin.dht.content;

import io.topiacoin.dht.intf.ValueStorage;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.*;

public abstract class AbstractValueStorageTest {

    protected abstract ValueStorage getValueStorage();

    @Test
    public void testSanity() throws Exception {
        ValueStorage valueStorage = getValueStorage();

        assertNotNull(valueStorage);
    }

    @Test
    public void testStoringAndGettingSingleValue() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Firefly";
        String value = "Serenity";

        valueStorage.setValue(key, value);

        Collection<String> values = valueStorage.getValues(key);

        assertEquals(1, values.size());
        assertTrue(values.contains(value));
    }

    @Test
    public void testGetNonexistentKey() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Firefly";

        Collection<String> values = valueStorage.getValues(key);

        assertEquals(0, values.size());
    }

    @Test
    public void testStoringAndGettingMultipleValues() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Firefly";
        List<String> values = new ArrayList<String>();
        values.add("Malcolm");
        values.add("Wash");
        values.add("Zoe");
        values.add("Inara");
        values.add("Jayne");
        values.add("Simon");
        values.add("River");
        values.add("Kaylee");

        for (String value : values) {
            valueStorage.setValue(key, value);
        }

        Collection<String> fetchedValues = valueStorage.getValues(key);

        assertEquals(values.size(), fetchedValues.size());
        assertTrue(values.containsAll(fetchedValues));
    }


    @Test
    public void testRemovingWithMultipleValue() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Firefly";
        List<String> values = new ArrayList<String>();
        values.add("Malcolm");
        values.add("Wash");
        values.add("Zoe");
        values.add("Inara");
        values.add("Jayne");
        values.add("Simon");
        values.add("River");
        values.add("Kaylee");

        for (String value : values) {
            valueStorage.setValue(key, value);
        }

        valueStorage.removeValue(key, values.get(1));
        values.remove(1);

        Collection<String> fetchedValues = valueStorage.getValues(key);

        assertEquals(values.size(), fetchedValues.size());
        assertTrue(values.containsAll(fetchedValues));
    }

    @Test
    public void testRemovingAllMultipleValue() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Firefly";
        List<String> values = new ArrayList<String>();
        values.add("Malcolm");
        values.add("Wash");
        values.add("Zoe");
        values.add("Inara");
        values.add("Jayne");
        values.add("Simon");
        values.add("River");
        values.add("Kaylee");

        for (String value : values) {
            valueStorage.setValue(key, value);
        }

        for (String value : values) {
            valueStorage.removeValue(key, value);
        }
        values.clear();

        Collection<String> fetchedValues = valueStorage.getValues(key);

        assertEquals(values.size(), fetchedValues.size());
        assertTrue(values.containsAll(fetchedValues));
    }

    @Test
    public void testRemovingNonexistentValue() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Firefly";
        List<String> values = new ArrayList<String>();
        values.add("Malcolm");
        values.add("Wash");
        values.add("Zoe");
        values.add("Inara");
        values.add("Jayne");
        values.add("Simon");
        values.add("River");
        values.add("Kaylee");

        for (String value : values) {
            valueStorage.setValue(key, value);
        }

        valueStorage.removeValue(key, "CaptKirk");

        Collection<String> fetchedValues = valueStorage.getValues(key);

        assertEquals(values.size(), fetchedValues.size());
        assertTrue(values.containsAll(fetchedValues));

    }

    @Test
    public void testStoringDuplicateValue() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Firefly";
        List<String> values = new ArrayList<String>();
        values.add("Malcolm");
        values.add("Wash");
        values.add("Zoe");
        values.add("Inara");
        values.add("Jayne");
        values.add("Simon");
        values.add("River");
        values.add("Kaylee");

        for (String value : values) {
            valueStorage.setValue(key, value);
        }

        // Add them all again
        for (String value : values) {
            valueStorage.setValue(key, value);
        }

        Collection<String> fetchedValues = valueStorage.getValues(key);

        assertEquals(values.size(), fetchedValues.size());
        assertTrue(values.containsAll(fetchedValues));
    }

    @Test
    public void testStoringNullValue() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Iamnull";

        try {
            valueStorage.setValue(key, null);
            fail("Expected InvalidArgumentException to tbe thrown");
        } catch (NullPointerException e) {
            // NOOP
        }
    }

    @Test
    public void testStoringNullKey() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String value = "Iamnull";

        try {
            valueStorage.setValue(null, value);
            fail("Expected InvalidArgumentException to tbe thrown");
        } catch (NullPointerException e) {
            // NOOP
        }
    }

    @Test
    public void testSaveAndLoad() throws Exception {

        ValueStorage valueStorage = getValueStorage();

        String key = "Firefly";
        List<String> values = new ArrayList<String>();
        values.add("Malcolm");
        values.add("Wash");
        values.add("Zoe");
        values.add("Inara");
        values.add("Jayne");
        values.add("Simon");
        values.add("River");
        values.add("Kaylee");

        // Put in the forward Mapping
        for (String value : values) {
            valueStorage.setValue(key, value);
        }

        // Put in the Reverse Mapping
        for (String value : values) {
            valueStorage.setValue(value, key);
        }

        File tempFile = File.createTempFile("dht", null);
        try {

            valueStorage.save(tempFile);

            ValueStorage loadedStorage = getValueStorage();
            loadedStorage.load(tempFile);

            Map<String, Collection<String>> originalValueMap = valueStorage.getValueMap();
            Map<String, Collection<String>> loadedValueMap = loadedStorage.getValueMap();

            assertEquals(originalValueMap, loadedValueMap);
        } finally {
            tempFile.delete();
        }
    }
}
