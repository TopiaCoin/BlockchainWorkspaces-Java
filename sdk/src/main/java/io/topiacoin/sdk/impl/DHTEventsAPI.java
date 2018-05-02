package io.topiacoin.sdk.impl;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.EventsAPI;
import io.topiacoin.dht.DHT;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.config.DefaultDHTConfiguration;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class DHTEventsAPI implements EventsAPI {

    private DHT _dht;

    public DHTEventsAPI(Configuration configuration) {
        int udpPort = 0;
        KeyPair keyPair = null;
        try {
            _dht = new DHT(udpPort, keyPair, new DHTConfiguration(configuration)) ;
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException("Failed to Initialize the DHT" );
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the Event Fetcher. This will start the process of looking for events.
     * <p>
     * When new workspace events are detected, the Event Fetcher will post a notification to the Notification Center.
     * The event type will be based on the kinds of event that is being sent (workspace, file, etc). The classifier of
     * the notification will be the ID of the item the notification is related to (e.g. workspace ID, or file ID), or
     * null if the notification isn't related to a specific entity.
     */
    @Override
    public void startEventFetching() {

    }

    /**
     * Stops looking for events.
     */
    @Override
    public void stopEventFetching() {

    }

    /**
     * Returns true if the event fetcher is running. Otherwise, it returns false.
     *
     * @return
     */
    @Override
    public boolean isRunning() {
        return false;
    }
}
