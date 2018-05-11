package io.topiacoin.sdk.impl;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.EventsAPI;
import io.topiacoin.dht.DHT;
import io.topiacoin.dht.SDFSDHTAccessor;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.model.DHTWorkspaceEntry;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.util.NotificationCenter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DHTEventsAPI implements EventsAPI {
    private static final Log _log = LogFactory.getLog(DHTEventsAPI.class);
    private final SDFSDHTAccessor _dhtAccessor;
    private Thread _DHTEventFetchThread;
    private DHTEventFetchRunnable _DHTEventFetchRunnable;
    private final long _DHTEventFetchInterval = 1000;
    private DataModel _model;
    private NotificationCenter _notificationCenter = NotificationCenter.defaultCenter();

    public DHTEventsAPI(Configuration configuration, DataModel model) {
        _model = model;
        _dhtAccessor = SDFSDHTAccessor.getInstance(configuration, model);
        _DHTEventFetchRunnable = new DHTEventFetchRunnable();
    }

    /**
     * Starts the Event Fetcher. This will start the process of looking for events.
     * <p>
     * When new workspace events are detected, the Event Fetcher will post a notification to the Notification Center.
     * The event type will be based on the kinds of event that is being sent (workspace, file, etc). The classifier of
     * the notification will be the ID of the item the notification is related to (e.g. workspace ID, or file ID), or
     * null if the notification isn't related to a specific entity.
     * @throws NoSuchUserException If the current user cannot be ascertained (read: not logged in)
     */
    @Override
    public void startEventFetching() throws NoSuchUserException {
        _model.getCurrentUser();
        if(!isRunning()) {
            _DHTEventFetchThread = new Thread(_DHTEventFetchRunnable, "DHTEventFetch");
            _DHTEventFetchThread.setDaemon(true);
            _DHTEventFetchThread.start();
            for(int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    //nop
                }
                if(isRunning()) {
                    return;
                }
            }
            throw new RuntimeException("Failed to start Event Fetching");
        }
    }

    /**
     * Stops looking for events.
     */
    @Override
    public void stopEventFetching() {
        if(isRunning()) {
            if (_DHTEventFetchRunnable != null) {
                _DHTEventFetchRunnable.stop();
            }
            if (_DHTEventFetchThread != null) {
                try {
                    _DHTEventFetchThread.join(3000);
                    if (_DHTEventFetchThread.isAlive()) {
                        _DHTEventFetchThread.interrupt();
                    }
                } catch (InterruptedException e) {
                    //NOP
                }
            }
            _DHTEventFetchThread = null;
        }
    }

    /**
     * Returns true if the event fetcher is running. Otherwise, it returns false.
     *
     * @return true if the event fetcher is running. Otherwise, it returns false.
     */
    @Override
    public boolean isRunning() {
        return _DHTEventFetchRunnable != null && _DHTEventFetchRunnable.isRunning();
    }

    private class DHTEventFetchRunnable implements Runnable {
        private boolean _isRunning = false;
        private List<DHTWorkspaceEntry> _cachedEntries = new ArrayList<>();
        @Override public void run() {
            _isRunning = true;
            try {
                while (_isRunning) {
                    List<DHTWorkspaceEntry> theseEntries = _dhtAccessor.fetchMyDHTWorkspaces();
                    if(!_cachedEntries.containsAll(theseEntries)) {
                        //One or more workspaces have been added - figure out which ones.
                        List<DHTWorkspaceEntry> addedWorkspaces = new ArrayList<>(theseEntries);
                        addedWorkspaces.removeAll(_cachedEntries);
                        for(DHTWorkspaceEntry addedWorkspace : addedWorkspaces) {
                            _notificationCenter.postNotification("newWorkspace", addedWorkspace.getWorkspaceID(), null);
                        }
                    }
                    if(!theseEntries.containsAll(_cachedEntries)) {
                        //One or more workspaces have been removed - figure out which ones.
                        List<DHTWorkspaceEntry> removedWorkspaces = new ArrayList<>(_cachedEntries);
                        removedWorkspaces.removeAll(theseEntries);
                        for(DHTWorkspaceEntry removedWorkspace : removedWorkspaces) {
                            _notificationCenter.postNotification("removedFromWorkspace", removedWorkspace.getWorkspaceID(), null);
                        }
                    }
                    _cachedEntries = theseEntries;
                    Thread.sleep(_DHTEventFetchInterval);
                }
            } catch (InterruptedException e) {
                //NOP
            } catch (NoSuchUserException e) {
                _log.warn("Stopping DHTEventFetch non-clean - it appears the User has logged out", e);
            }
            _isRunning = false;
        }

        void stop() {
            _isRunning = false;
        }

        boolean isRunning() {
            return _isRunning;
        }
    }
}
