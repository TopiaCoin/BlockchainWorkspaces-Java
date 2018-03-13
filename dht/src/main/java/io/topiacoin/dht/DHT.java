package io.topiacoin.dht;

import io.topiacoin.dht.action.ConnectionAction;
import io.topiacoin.dht.action.FetchValueAction;
import io.topiacoin.dht.action.PeriodicRefreshAction;
import io.topiacoin.dht.action.RemoveValueAction;
import io.topiacoin.dht.action.StoreValueAction;
import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.content.InMemoryValueStorage;
import io.topiacoin.dht.content.ValueStorage;
import io.topiacoin.dht.intf.FetchContentCallback;
import io.topiacoin.dht.intf.StoreContentCallback;
import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import io.topiacoin.dht.routing.RoutingTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.TreeSet;

public class DHT {

    private final Log _log = LogFactory.getLog(this.getClass());

    protected DHTComponents _dhtComponents;

    private KeyPair keyPair;
    private NodeID nodeID;
    private final Node _node;
    private final MessageDigest _sha1Hash;

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

        ValueStorage valueStorage = new InMemoryValueStorage();

        _dhtComponents = new DHTComponents();
        _dhtComponents.setConfiguration(configuration);
        _dhtComponents.setMessageSigner(messageSigner);
        _dhtComponents.setCommunicationServer(communicationServer);
        _dhtComponents.setRoutingTable(routingTable);
        _dhtComponents.setMessageFactory(messageFactory);
        _dhtComponents.setValueStorage(valueStorage);

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

        System.out.println("NodeID: " + this.nodeID);
    }

    public static DHT loadState() {
        return null;
    }

    public void saveState() {

    }

    public Node getNode() {
        return _node;
    }

    public void bootstrap(Node bootStrapNode) throws IOException {

        ConnectionAction connectionAction = new ConnectionAction(_node, bootStrapNode, _dhtComponents);
        connectionAction.execute();


     /*   // Populate the Routing Table by asking the bootstrap node for nodes that fit in each of our buckets

        CommunicationServer communicationServer = _dhtComponents.getCommunicationServer();
        RoutingTable routingTable = _dhtComponents.getRoutingTable();
        routingTable.insert(bootStrapNode);

        for (int i = 0; i < 160; i++) {
            NodeID bucketNodeID = this.nodeID.generateNodeIDByDistance(i);
            NodeLookupRequest nodeLookupMessage = new NodeLookupRequest(bucketNodeID);

            new NodeLookupAction(_node, bucketNodeID, this._dhtComponents).execute();
        }*/
    }

    public void refresh() {

        PeriodicRefreshAction periodicRefreshAction = new PeriodicRefreshAction(_node, _dhtComponents);
        periodicRefreshAction.execute();
    }

    public void shutdown(final boolean saveState) {
        _dhtComponents.getCommunicationServer().shutdown();
    }

    public int storeContent(String key, String value) {

        StoreValueAction storeValueAction = new StoreValueAction(_node, key, value, _dhtComponents) ;
        storeValueAction.execute();

        // Wait a moment to allow response to come back to the Store Action.
        try {
            Thread.sleep(10);
        } catch ( InterruptedException e ) {
            // NOOP
        }

        return storeValueAction.getStorageCount();
    }

    public void storeContent(String key, String value, StoreContentCallback callback, Object context) {
        throw new UnsupportedOperationException("This method is not yet implemented" );
    }

    public Set<String> fetchContent(String key) {

        FetchValueAction fetchValueAction = new FetchValueAction(key, this._dhtComponents);

        fetchValueAction.execute();

        Set<String> values = new TreeSet<String>() ;

        if ( fetchValueAction.isContentFound() ) {
            values.addAll(fetchValueAction.getContent());
        }

        return values;
    }

    public void fetchContent(String key, FetchContentCallback callback, Object context) {
        throw new UnsupportedOperationException("This method is not yet implemented" );
    }

    public boolean removeContent(String key, String value) {

        RemoveValueAction removeValueAction = new RemoveValueAction(_node, key, value, _dhtComponents) ;

        removeValueAction.execute();

        try {
            Thread.sleep(10) ;
        } catch ( InterruptedException e) {
            // NOOP
        }

        return ( removeValueAction.getRemoveCount() > 0 );
    }

    public void removeContent(String key, String value, Object callback, Object context) {

    }

}
