package io.topiacoin.workspace.blockchain.util;

import java.util.HashMap;
import java.util.Map;

public class TimeExpiringCache<K,V>{


    private final long defaultTimeToLive;
    private Map<K, ExpiringEntry<V>> expiringMap;

    public TimeExpiringCache(long defaultTimeToLive) {
        this.defaultTimeToLive = defaultTimeToLive;
        expiringMap = new HashMap<>() ;
    }

    public void put(K key, V value) {
        long expirationTime = System.currentTimeMillis() + defaultTimeToLive ;
        ExpiringEntry<V> entry = new ExpiringEntry<>(value, expirationTime);
        expiringMap.put(key, entry);
    }

    public void put(K key, V value, long timeToLive) {
        long expirationTime = System.currentTimeMillis() + timeToLive ;
        ExpiringEntry<V> entry = new ExpiringEntry<>(value, expirationTime);
        expiringMap.put(key, entry);
    }

    public V get(K key){
        V value = null ;
        ExpiringEntry<V> expiringEntry = expiringMap.get(key);
        if ( expiringEntry != null) {
            if (!expiringEntry.isExpired()) {
                value = expiringEntry.value;
            } else {
                expiringMap.remove(key, expiringEntry);
            }
        }
        return value;
    }

    public V remove(K key){
        V value = null ;
        ExpiringEntry<V> expiringEntry = expiringMap.remove(key);
        if ( expiringEntry != null && !expiringEntry.isExpired()) {
            value = expiringEntry.value ;
        }
        return value;
    }


    public void expireAll() {
        expiringMap.clear();
    }


    private static class ExpiringEntry<V> {
        private V value ;
        private long expirationTime;

        public ExpiringEntry(V value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        public V getValue() {
            return value;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expirationTime;
        }
    }
}
