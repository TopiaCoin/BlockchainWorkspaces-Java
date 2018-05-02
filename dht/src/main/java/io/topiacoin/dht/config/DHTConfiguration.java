package io.topiacoin.dht.config;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;

public class DHTConfiguration {

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

    private static final long defaultRestoreInterval = 60 * 1000;
    private static final long defaultResponseTimeout = 2000;
    private static final long defaultOperationTimeout = 2000;
    private static final int defaultMaxConcurrentMessages = 10;
    private static final int defaultC1 = 10 ;
    private static final int defaultC2 = 20 ;
    private static final int defaultK = 20;
    private static final int defaultStaleLimit = 1;
    private static final String defaultNodeDataFolder = "kademlia";
    private static final int defaultEntryExpirationTime = 86400000; // 24 hours


    private Configuration _coreConfiguration ;

    public DHTConfiguration(Configuration coreConfiguration) {
        this._coreConfiguration = coreConfiguration;
    }

    public long getRestoreInterval() {
        return _coreConfiguration.getConfigurationOption(RESTORE_INTERVAL, defaultRestoreInterval) ;
    }

    public void setRestoreInterval(long restoreInterval) {
        _coreConfiguration.setConfigurationOption(RESTORE_INTERVAL, Long.toString(restoreInterval));
    }

    public long getResponseTimeout() {
        return _coreConfiguration.getConfigurationOption(RESPONSE_TIMEOUT, defaultResponseTimeout) ;
    }

    public void setResponseTimeout(long responseTimeout) {
        _coreConfiguration.setConfigurationOption(RESPONSE_TIMEOUT, Long.toString(responseTimeout));
    }

    public long getOperationTimeout() {
        return _coreConfiguration.getConfigurationOption(OPERATION_TIMEOUT, defaultOperationTimeout) ;
    }

    public void setOperationTimeout(long operationTimeout) {
        _coreConfiguration.setConfigurationOption(OPERATION_TIMEOUT, Long.toString(operationTimeout));
    }

    public int getMaxConcurrentMessages() {
        return _coreConfiguration.getConfigurationOption(MAX_CONCURRENT_MESSAGES, defaultMaxConcurrentMessages) ;
    }

    public void setMaxConcurrentMessages(int maxConcurrentMessages) {
        _coreConfiguration.setConfigurationOption(MAX_CONCURRENT_MESSAGES, Integer.toString(maxConcurrentMessages));
    }

    public int getC1() {
        return _coreConfiguration.getConfigurationOption(NODEID_C1, defaultC1) ;
    }

    public void setC1(int c1) {
        _coreConfiguration.setConfigurationOption(NODEID_C1, Integer.toString(c1));
    }

    public int getC2() {
        return _coreConfiguration.getConfigurationOption(NODEID_C2, defaultC2) ;
    }

    public void setC2(int c2) {
        _coreConfiguration.setConfigurationOption(NODEID_C2, Integer.toString(c2));
    }

    public int getK() {
        return _coreConfiguration.getConfigurationOption(DHT_K, defaultK) ;
    }

    public void setK(int k) {
        _coreConfiguration.setConfigurationOption(DHT_K, Integer.toString(k));
    }

    public int getStaleLimit() {
        return _coreConfiguration.getConfigurationOption(STALE_LIMIT, defaultStaleLimit) ;
    }

    public void setStaleLimit(int staleLimit) {
        _coreConfiguration.setConfigurationOption(STALE_LIMIT, Integer.toString(staleLimit));
    }

    public String getNodeDataFolder() {
        return _coreConfiguration.getConfigurationOption(NODE_DATA_FOLDER, defaultNodeDataFolder) ;
    }

    public void setNodeDataFolder(String nodeDataFolder) {
        _coreConfiguration.setConfigurationOption(NODE_DATA_FOLDER, nodeDataFolder);
    }

    public int getEntryExpirationTime() {
        return _coreConfiguration.getConfigurationOption(ENTRY_EXPIRATION_TIME, defaultEntryExpirationTime) ;
    }

    public void setEntryExpirationTime(int entryExpirationTime) {
        _coreConfiguration.setConfigurationOption(ENTRY_EXPIRATION_TIME, Integer.toString(entryExpirationTime));
    }
}
