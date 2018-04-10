package io.topiacoin.dht.content;

import io.topiacoin.dht.intf.ValueStorage;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@Deprecated
public class InMemoryValueStorage implements ValueStorage {

    private Map<String, Collection<String>> valueMap;

    public InMemoryValueStorage() {
    }

    @PostConstruct
    public void initialize() {
        this.valueMap = new TreeMap<String, Collection<String>>();
    }

    @PreDestroy
    public void shutdown() {

    }

    public void setValue(String key, String value) {
        Collection<String> valueSet = this.valueMap.get(key);
        if (valueSet == null) {
            valueSet = new TreeSet<String>();
            this.valueMap.put(key, valueSet);
        }
        valueSet.add(value);
    }

    public Collection<String> getValues(String key) {
        Collection<String> storedValues = this.valueMap.get(key);
        Collection<String> values = new TreeSet<String>();
        if (storedValues == null) {
            values = Collections.emptySet();
        } else {
            values.addAll(storedValues);
        }
        return values;
    }

    public boolean containsKey(String key) {
        return this.valueMap.containsKey(key);
    }

    public void removeValue(String key, String value) {
        Collection<String> values = this.valueMap.get(key);
        if (values != null) {
            values.remove(value);
            if (values.size() == 0) {
                // We have removed all the values, so throw away the collection
                this.valueMap.remove(key);
            }
        }
    }

    public Map<String, Collection<String>> getValueMap() {
        return valueMap;
    }

    public void save(File file) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(valueMap);
        oos.close();
    }

    public void load(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        valueMap = (Map<String, Collection<String>>) ois.readObject();
        ois.close();
    }

}
