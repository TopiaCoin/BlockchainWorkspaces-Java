package io.topiacoin.dht;

public class DHT {


    public DHT(int udpPort, Object keyPair) {

    }

    public static DHT loadState() {
        return null ;
    }

    public void saveState() {

    }

    public void bootstrap(Object node) {

    }

    public void shutdown(final boolean saveState) {

    }

    public int storeValue(String key, String value) {
        return 0;
    }

    public void storeValue(String key, String value, Object callback) {
    }

    public String fetchValue(String key) {
        return null ;
    }

    public void fetchValue(String key, Object callback) {

    }

    public boolean removeValue(String key, String value) {
        return false;
    }

    public void removeValue(String key, String value, Object callback) {

    }

}
