package io.topiacoin.dht;

import io.topiacoin.dht.action.ConnectionAction;
import io.topiacoin.dht.action.FetchValueAction;
import io.topiacoin.dht.action.PeriodicRefreshAction;
import io.topiacoin.dht.action.RemoveValueAction;
import io.topiacoin.dht.action.StoreValueAction;
import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.content.InMemoryExpiringValueStorage;
import io.topiacoin.dht.content.InMemoryValueStorage;
import io.topiacoin.dht.intf.ValueStorage;
import io.topiacoin.dht.intf.FetchContentCallback;
import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import io.topiacoin.dht.routing.RoutingTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

/**
 * A Kademlia Distributed Hash Table.  The hash table will connect to other DHT nodes to store and retrieve content.
 */
public class DHT {

    private final Log _log = LogFactory.getLog(this.getClass());

    protected DHTComponents _dhtComponents;

    private KeyPair keyPair;
    private NodeID nodeID;
    private final Node _node;
    private final MessageDigest _sha1Hash;
    private boolean isRunning;

    private Timer refreshTimer;

    public DHT(int udpPort, KeyPair keyPair, Configuration configuration) throws SocketException, NoSuchAlgorithmException, UnknownHostException {

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        this.nodeID = nodeIDGenerator.generateNodeID();
        this.keyPair = keyPair;
        _node = new Node(this.nodeID, InetAddress.getLocalHost(), udpPort);

        MessageSigner messageSigner = new DSAMessageSigner();

        RoutingTable routingTable = new RoutingTable();
        routingTable.setNodeID(nodeID);

        MessageFactory messageFactory = new MessageFactory();

        CommunicationServer communicationServer = new CommunicationServer(udpPort, keyPair, this._node);

        int entryExpirationTime = configuration.getEntryExpirationTime();

        ValueStorage valueStorage = new InMemoryExpiringValueStorage(entryExpirationTime);

        Semaphore refreshSemaphore = new Semaphore(1);

        _dhtComponents = new DHTComponents();
        _dhtComponents.setConfiguration(configuration);
        _dhtComponents.setMessageSigner(messageSigner);
        _dhtComponents.setCommunicationServer(communicationServer);
        _dhtComponents.setRoutingTable(routingTable);
        _dhtComponents.setMessageFactory(messageFactory);
        _dhtComponents.setValueStorage(valueStorage);
        _dhtComponents.setRefreshSemaphore(refreshSemaphore);

        messageFactory.setDhtComponents(_dhtComponents);
        communicationServer.setDHTComponents(_dhtComponents);
        routingTable.setDhtComponents(_dhtComponents);

        valueStorage.initialize();
        routingTable.initialize();
        messageFactory.initialize();
        communicationServer.start();

        _sha1Hash = MessageDigest.getInstance("SHA-1");

        // Insert this node into the routing table.
        routingTable.insert(_node);

        isRunning = false;

        System.out.println("NodeID: " + this.nodeID);
    }

    private void loadState() throws IOException {

        File stateDirectory = new File(_dhtComponents.getConfiguration().getNodeDataFolder());
        File routingTableStateFile = new File(stateDirectory, "routingTable");
        File hashTableStateFile = new File(stateDirectory, "hashTable");

        NodeID nodeID;
        RoutingTable routingTable;
        ValueStorage valueStorage;

        long bytesToBeRead = routingTableStateFile.length();

        // Buffer is allocated at 512KB to insure that the entire routing table will fit
        ByteBuffer routingTableBuffer = ByteBuffer.allocate((int) bytesToBeRead);

        // Read in the NodeID and Routing Table
        FileChannel routingTableChannel = new FileInputStream(routingTableStateFile).getChannel();
        routingTableChannel.read(routingTableBuffer);
        routingTableBuffer.flip();
        nodeID = NodeID.decode(routingTableBuffer);
        routingTable = new RoutingTable(routingTableBuffer, _dhtComponents);
        routingTableChannel.close();

        this.nodeID = nodeID;
        _dhtComponents.setRoutingTable(routingTable);
    }

    private void saveState() throws IOException {

        File stateDirectory = new File(_dhtComponents.getConfiguration().getNodeDataFolder());
        File routingTableStateFile = new File(stateDirectory, "routingTable");
        File hashTableStateFile = new File(stateDirectory, "hashTable");

        RoutingTable routingTable = _dhtComponents.getRoutingTable();
        ValueStorage valueStorage = _dhtComponents.getValueStorage();

        try {
            // Make sure the state directory exists
            stateDirectory.mkdirs();

            FileChannel routingTableChannel = new FileOutputStream(routingTableStateFile).getChannel();

            // Buffer is allocated at 512KB to insure that the entire routing table will fit
            ByteBuffer routingTableBuffer = ByteBuffer.allocate(512 * 1024);

            // Save the NodeID and the Routing Table
            nodeID.encode(routingTableBuffer);
            routingTable.encode(routingTableBuffer);
            routingTableBuffer.flip();
            routingTableChannel.write(routingTableBuffer);
            routingTableChannel.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to Save State", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to Save State", e);
        } finally {

        }

    }

    public Node getNode() {
        return _node;
    }

    public void bootstrap(Node bootStrapNode) throws IOException {
        isRunning = true;

        ConnectionAction connectionAction = new ConnectionAction(_node, bootStrapNode, _dhtComponents);
        try {
            connectionAction.execute();
            startRefreshTimer();
        } catch (IOException e) {
            isRunning = false;
            throw e;
        }
    }

    public void start() throws IllegalStateException, IOException {
        start(true);
    }

    public void start(boolean loadState) throws IllegalStateException, IOException {
        if (isRunning) {
            throw new IllegalStateException("The DHT is already running");
        }

        // Load the current state
        isRunning = true;
        if (loadState) {
            loadState();
        }

        refresh();

        startRefreshTimer();
    }

    public void shutdown(final boolean saveState) {
        stopRefreshTimer();
        _dhtComponents.getCommunicationServer().shutdown();
        if (saveState) {
            try {
                saveState();
            } catch (IOException e) {
                _log.warn("Unable to Save DHT State", e);
            }
        }
        isRunning = false;
    }

    private void startRefreshTimer() {
        long refreshInterval = _dhtComponents.getConfiguration().getRestoreInterval();

        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        refreshTimer = new Timer(nodeID + " Periodic Refresh Timer");
        refreshTimer.schedule(new PeriodicRefreshTask(), refreshInterval, refreshInterval);
    }

    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    public synchronized void refresh() {

        PeriodicRefreshAction periodicRefreshAction = new PeriodicRefreshAction(_node, _dhtComponents);
        periodicRefreshAction.execute();
    }

    public int storeContent(String key, String value) {

        StoreValueAction storeValueAction = new StoreValueAction(_node, key, value, _dhtComponents);
        storeValueAction.execute();

        if (storeValueAction.getStorageCount() == 0) {
            // If no one has stored the content yet, wait a moment to allow response to come back to the Store Action.
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // NOOP
            }
        }

        return storeValueAction.getStorageCount();
    }

    public Set<String> fetchContent(String key) {

        Set<String> values = new TreeSet<String>();

        if (!this._dhtComponents.getValueStorage().containsKey(key)) {
            FetchValueAction fetchValueAction = new FetchValueAction(key, this._dhtComponents);

            fetchValueAction.execute();

            if (fetchValueAction.isContentFound()) {
                values.addAll(fetchValueAction.getContent());
            }
        } else {
            Collection<String> localValue = this._dhtComponents.getValueStorage().getValues(key);
            values.addAll(localValue);
        }

        return values;
    }

    public void fetchContent(String key, FetchContentCallback callback, Object context) {
        throw new UnsupportedOperationException("This method is not yet implemented");
    }

    public int removeContent(String key, String value) {

        RemoveValueAction removeValueAction = new RemoveValueAction(_node, key, value, _dhtComponents);

        removeValueAction.execute();

        if (removeValueAction.getRemoveCount() == 0) {
            // If no one has removed the content yet, wait a moment to allow response to come back to the Remove Action.
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // NOOP
            }
        }

        return removeValueAction.getRemoveCount();
    }

    public void removeContent(String key, String value, Object callback, Object context) {

    }

    public boolean isRunning() {
        return isRunning;
    }

    // -------- Timer Task that Performs the Periodic Refresh --------

    class PeriodicRefreshTask extends TimerTask {

        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            System.out.println("Refreshing the DHT");
            refresh();
        }
    }
}
