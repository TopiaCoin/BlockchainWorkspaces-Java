package io.topiacoin.dht.content;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

public class InMemoryValueStorage implements ValueStorage {

    private Map<String, String> valueMap;

    public InMemoryValueStorage() {
    }

    @PostConstruct
    public void initialize() {
        this.valueMap = new HashMap<String, String>();
    }

    @PreDestroy
    public void shutdown() {

    }

    public void setValue(String key, String value) {
        this.valueMap.put(key, value) ;
    }

    public String getValue(String key) {
        return this.valueMap.get(key);
    }

    public boolean containsKey(String key) {
        return this.valueMap.containsKey(key);
    }

    public void removeValue(String key, String value) {
        this.valueMap.remove(key);
    }

    public Map<String, String> getValueMap() {
        return valueMap;
    }
}
