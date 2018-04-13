package io.topiacoin.dht.routing;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class RoutingTable {

    private NodeID nodeID;
    private DHTComponents _dhtComponents;
    private RouteBucket routeBuckets[] ;

    public RoutingTable () {
    }

    public RoutingTable(ByteBuffer buffer, DHTComponents dhtComponents) throws IOException {
        this._dhtComponents = dhtComponents;
        decode(buffer);
    }

    public void initialize() {

        Configuration configuration = _dhtComponents.getConfiguration();

        routeBuckets = new RouteBucket[160] ;

        for ( int i = 0 ; i < routeBuckets.length ; i++ ) {
            routeBuckets[i] = new RouteBucket(i, configuration) ;
        }
    }

    public NodeID getNodeID() {
        return nodeID;
    }

    public void setNodeID(NodeID nodeID) {
        this.nodeID = nodeID;
    }

    public DHTComponents getDhtComponents() {
        return _dhtComponents;
    }

    public void setDhtComponents(DHTComponents dhtComponents) {
        _dhtComponents = dhtComponents;
    }

    public void insert(Node node) {
        int distance ;

        distance = this.nodeID.getDistance(node.getNodeID()) - 1;

        if ( distance < 0 ) distance = 0;

        RouteBucket routeBucket = routeBuckets[distance] ;
        routeBucket.insert(node);
    }

    public List<Node> getAllNodes() {
        List<Node> allNodes = new ArrayList<Node>();

        for ( RouteBucket bucket : routeBuckets ) {
            List<RouteBucket.NodeInfo> nodeInfos = bucket.getNodeInfos();
            for (RouteBucket.NodeInfo nodeInfo : nodeInfos) {
                allNodes.add(nodeInfo.getNode()) ;
            }
        }

        return allNodes ;
    }

    public List<Node> findClosest(NodeID targetID, int numNodes) {
        TreeSet<Node> sortedNodes = new TreeSet<Node>(new NodeIDComparator(targetID));
        sortedNodes.addAll(this.getAllNodes()) ;

        List<Node> nodes = new ArrayList<Node>(numNodes) ;

        Iterator<Node> iterator = sortedNodes.iterator();
        while ( iterator.hasNext() && nodes.size() < numNodes ) {
            nodes.add(iterator.next()) ;
        }

        return nodes;
    }

    public int getBucketID(Node nodeID) {
        return 0;
    }

    public void setUnresponsiveNodes(List<Node> nodes) {

    }

    public void setUnresponsiveNode(Node node) {

    }

    public void encode (ByteBuffer buffer) {
        nodeID.encode(buffer);
        buffer.putInt(routeBuckets.length);
        for ( RouteBucket bucket : routeBuckets) {
            bucket.encode(buffer);
        }
    }

    private void decode (ByteBuffer buffer) throws IOException {
        nodeID = NodeID.decode(buffer);
        int bucketCount = buffer.getInt();
        routeBuckets = new RouteBucket[bucketCount];
        for ( int i = 0 ; i < bucketCount ; i++ ){
            RouteBucket routeBucket = new RouteBucket(buffer, _dhtComponents.getConfiguration());
            routeBuckets[i] = routeBucket;
        }
    }

    @Override
    public String toString() {
        return "RoutingTable{" +
                "nodeID=" + nodeID +
                ", configuration=" + _dhtComponents.getConfiguration() +
                ", routeBuckets=" + Arrays.toString(routeBuckets) +
                '}';
    }
}
