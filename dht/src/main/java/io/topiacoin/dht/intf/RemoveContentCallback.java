package io.topiacoin.dht.intf;

public interface RemoveContentCallback {

    void didRemoveContent(Object context);

    void failedToRemoveContent(String key, String value, Object context);

    void timeout(String key, Object context);
}
