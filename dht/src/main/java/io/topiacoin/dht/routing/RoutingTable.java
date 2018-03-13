package io.topiacoin.dht.routing;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;

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
            for (RouteBucket.NodeInfo nodeInfo : bucket.getNodeInfos() ) {
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

    @Override
    public String toString() {
        return "RoutingTable{" +
                "nodeID=" + nodeID +
                ", configuration=" + _dhtComponents.getConfiguration() +
                ", routeBuckets=" + Arrays.toString(routeBuckets) +
                '}';
    }
}
