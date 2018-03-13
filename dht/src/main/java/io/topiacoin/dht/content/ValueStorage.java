package io.topiacoin.dht.content;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

public interface ValueStorage {

    @PostConstruct
    void initialize();

    @PreDestroy
    void shutdown();

    void setValue(String key, String value) ;

    String getValue(String key) ;

    boolean containsKey(String key) ;

    void removeValue(String key, String value);

    Map<String, String> getValueMap();
}
