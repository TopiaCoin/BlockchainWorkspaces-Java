package io.topiacoin.dht.config;

public interface Configuration {
    long getRestoreInterval();

    void setRestoreInterval(long restoreInterval);

    long getResponseTimeout();

    void setResponseTimeout(long responseTimeout);

    int getMaxConcurrentMessages();

    void setMaxConcurrentMessages(int maxConcurrentMessages);

    int getK();

    void setK(int k);

    int getStaleLimit();

    void setStaleLimit(int staleLimit);

    String getNodeDataFolder();

    void setNodeDataFolder(String nodeDataFolder);
}
