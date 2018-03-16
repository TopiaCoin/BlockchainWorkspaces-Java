package io.topiacoin.dht.content;

import io.topiacoin.dht.intf.ValueStorage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class InMemoryExpiringValueStorage implements ValueStorage {

    private final int _entryExpirationTime;
    Map<String, Collection<ExpiringStringValue>> _valueMap;

    public InMemoryExpiringValueStorage(int entryExpirationTime) {
        _entryExpirationTime = entryExpirationTime;
    }

    @Override
    public void initialize() {
        _valueMap = new HashMap<String, Collection<ExpiringStringValue>>();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void setValue(String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new NullPointerException("Key not specified");
        }

        Collection<ExpiringStringValue> values = _valueMap.get(key);
        if (values == null) {
            values = new TreeSet<ExpiringStringValue>();
            _valueMap.put(key, values);
        }

        long expirationTime = System.currentTimeMillis() + _entryExpirationTime;
        ExpiringStringValue expiringStringValue = new ExpiringStringValue(value, expirationTime);

        // Remove the entry for the value, then readd it.
        values.remove(expiringStringValue);
        values.add(expiringStringValue);
    }

    @Override
    public Collection<String> getValues(String key) {
        Collection<String> retValues = new TreeSet<String>();
        Collection<ExpiringStringValue> values = _valueMap.get(key);

        if (values != null) {
            Iterator<ExpiringStringValue> iterator = values.iterator();
            while (iterator.hasNext()) {
                ExpiringStringValue expiringStringValue = iterator.next();
                if (expiringStringValue.isExpired()) {
                    // This entry has expired, so remove it from the collection
                    iterator.remove();
                } else {
                    // This entry isn't expired, so add it to the return set.
                    retValues.add(expiringStringValue.getValue());
                }
            }
        } else {
            retValues = Collections.emptySet();
        }

        return retValues;
    }

    @Override
    public boolean containsKey(String key) {
        return _valueMap.containsKey(key);
    }

    @Override
    public void removeValue(String key, String value) {
        Collection<ExpiringStringValue> values = _valueMap.get(key);
        if (values != null) {

            // Iterate through all the values for the key.  If the value matches, remove it.
            Iterator<ExpiringStringValue> iterator = values.iterator();
            while (iterator.hasNext()) {
                ExpiringStringValue expiringStringValue = iterator.next();
                if (value.equals(expiringStringValue.getValue())) {
                    iterator.remove();
                    break;
                } else if ( expiringStringValue.isExpired() ){
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public Map<String, Collection<String>> getValueMap() {

        Map<String, Collection<String>> retValueMap = new HashMap<String, Collection<String>>();
        Map<String, Collection<ExpiringStringValue>> valueMap = _valueMap;

        // Convert the values in the collections to strings.
        for (String key : _valueMap.keySet()) {
            Collection<String> values = getValues(key);
            if (values != null && !values.isEmpty()) {
                retValueMap.put(key, values);
            }
        }

        return retValueMap;
    }

    @Override
    public void save(File file) throws IOException {
        // NOOP - Saving of this Value Storage class is not supported.
    }

    @Override
    public void load(File file) throws ClassNotFoundException, IOException {
        // NOOP - Loading of this Value Storage class is not supported.
    }


    // -------- Private Methods --------

    private void purgeExpiredValues(Collection<ExpiringStringValue> values) {

    }

    // -------- Internal Classes --------

    static class ExpiringStringValue implements Comparable<ExpiringStringValue> {
        private String value;
        private long expirationTime;

        public ExpiringStringValue(String value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        public String getValue() {
            return value;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expirationTime;
        }

        public void setExpirationDate(long expirationTime) {
            this.expirationTime = expirationTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExpiringStringValue that = (ExpiringStringValue) o;

            return value != null ? value.equals(that.value) : that.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @Override
        public int compareTo(ExpiringStringValue o) {
            return this.getValue().compareTo(o.getValue());
        }
    }
}
