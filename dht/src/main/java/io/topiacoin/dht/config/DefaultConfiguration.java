package io.topiacoin.dht.config;

public class DefaultConfiguration implements Configuration {

    private long restoreInterval = 60 * 1000;
    private long responseTimeout = 2000;
    private long operationTimeout = 2000;
    private int maxConcurrentMessages = 10;
    private int c1 = 10 ;
    private int c2 = 20 ;
    private int k = 20;
    private int staleLimit = 1;
    private String nodeDataFolder = "kademlia";
    private int entryExpirationTime = 86400000; // 24 hours

    public DefaultConfiguration() {
        // NOOP
    }

    public long getRestoreInterval() {
        return restoreInterval;
    }

    public void setRestoreInterval(long restoreInterval) {
        this.restoreInterval = restoreInterval;
    }

    public long getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(long responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public long getOperationTimeout() {
        return operationTimeout;
    }

    public void setOperationTimeout(long operationTimeout) {
        this.operationTimeout = operationTimeout;
    }

    public int getMaxConcurrentMessages() {
        return maxConcurrentMessages;
    }

    public void setMaxConcurrentMessages(int maxConcurrentMessages) {
        this.maxConcurrentMessages = maxConcurrentMessages;
    }

    public int getC1() {
        return c1;
    }

    public void setC1(int c1) {
        this.c1 = c1;
    }

    public int getC2() {
        return c2;
    }

    public void setC2(int c2) {
        this.c2 = c2;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getStaleLimit() {
        return staleLimit;
    }

    public void setStaleLimit(int staleLimit) {
        this.staleLimit = staleLimit;
    }

    public String getNodeDataFolder() {
        return nodeDataFolder;
    }

    public void setNodeDataFolder(String nodeDataFolder) {
        this.nodeDataFolder = nodeDataFolder;
    }

    @Override
    public int getEntryExpirationTime() {
        return entryExpirationTime;
    }

    @Override
    public void setEntryExpirationTime(int entryExpirationTime) {
        this.entryExpirationTime = entryExpirationTime;
    }
}
