package io.topiacoin.dht;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.dht.config.DHTConfiguration;

public class DHTTestConfiguration extends DHTConfiguration {

    private static final String RESTORE_INTERVAL = "dht.restore.interval";
    private static final String RESPONSE_TIMEOUT = "dht.response.timeout";
    private static final String OPERATION_TIMEOUT = "dht.operation.timeout";
    private static final String MAX_CONCURRENT_MESSAGES = "dht.max.concurrent.messages";
    private static final String NODEID_C1 = "dht.nodeid.c1";
    private static final String NODEID_C2 = "dht.nodeid.c2";
    private static final String DHT_K = "dht.k";
    private static final String STALE_LIMIT = "dht.stale.limit";
    private static final String NODE_DATA_FOLDER = "dht.node.data.folder";
    private static final String ENTRY_EXPIRATION_TIME = "dht.entry.expiration.time";

    private final long defaultRestoreInterval = 60 * 1000;
    private final long defaultResponseTimeout = 2000;
    private final long defaultOperationTimeout = 2000;
    private final int defaultMaxConcurrentMessages = 10;
    private final int defaultC1 = 4;
    private final int defaultC2 = 8;
    private final int defaultK = 20;
    private final int defaultStaleLimit = 1;
    private final String defaultNodeDataFolder = "kademlia";
    private final int defaultEntryExpirationTime = 86400000; // 24 hours


    public DHTTestConfiguration() {
        super(new DefaultConfiguration());
        setEm();
    }

    public DHTTestConfiguration(Configuration c) {
        super(c);
        setEm();
    }

    private void setEm() {
        setRestoreInterval(defaultRestoreInterval);
        setResponseTimeout(defaultResponseTimeout);
        setOperationTimeout(defaultOperationTimeout);
        setMaxConcurrentMessages(defaultMaxConcurrentMessages);
        setC1(defaultC1);
        setC2(defaultC2);
        setK(defaultK);
        setStaleLimit(defaultStaleLimit);
        setNodeDataFolder(defaultNodeDataFolder);
        setEntryExpirationTime(defaultEntryExpirationTime);
    }
}
