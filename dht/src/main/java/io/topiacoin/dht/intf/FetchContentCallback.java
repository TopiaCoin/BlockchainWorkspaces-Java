package io.topiacoin.dht.intf;

import java.util.List;

public interface FetchContentCallback {

    void didFetchValues(String key, List<String> fetchedValues, Object context);

    void failedToFetchValues(String key, Object context);

    void timeout(String key, Object context);
}
