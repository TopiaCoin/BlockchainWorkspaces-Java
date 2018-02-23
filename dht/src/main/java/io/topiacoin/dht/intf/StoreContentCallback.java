package io.topiacoin.dht.intf;

public interface StoreContentCallback {

    void didStoreValue(String key, int storageCount, Object context);

    void failedToStoreValue(String key, String content, Object context);

    void timeout(String key, Object context);
}
