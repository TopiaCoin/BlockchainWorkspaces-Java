package io.topiacoin.dht.intf;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface ValueStorage {

    @PostConstruct
    void initialize();

    @PreDestroy
    void shutdown();

    void setValue(String key, String value, long timeout);

    void refreshValue(String key, String value, long timeout);

    Collection<String> getValues(String key);

    boolean containsKey(String key);

    void removeValue(String key, String value);

    Map<String, Collection<String>> getValueMap();

    long getExpirationTime(String key, String value);

    void save(File file) throws IOException;

    void load(File file) throws ClassNotFoundException, IOException;
}
