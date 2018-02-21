package io.topiacoin.dht.config;

public class DefaultConfiguration implements Configuration {

    private long restoreInterval = 60 * 1000;
    private long responseTimeout = 2000;
    private int maxConcurrentMessages = 10;
    private int k = 5;
    private int staleLimit = 1;
    private String nodeDataFolder = "kademlia";

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

    public int getMaxConcurrentMessages() {
        return maxConcurrentMessages;
    }

    public void setMaxConcurrentMessages(int maxConcurrentMessages) {
        this.maxConcurrentMessages = maxConcurrentMessages;
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
}
