package io.topiacoin.dht.routing;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;

import java.util.List;

public class RoutingTable {

    private final NodeID nodeID;
    private final Configuration configuration;
    private RouteBucket routeBuckets[] ;

    public RoutingTable (NodeID nodeID, Configuration configuration) {
        this.configuration = configuration;
        this.nodeID = nodeID ;
        routeBuckets = new RouteBucket[160] ;

        for ( int i = 0 ; i < routeBuckets.length ; i++ ) {
            routeBuckets[i] = new RouteBucket(i, configuration) ;
        }
    }

    public void insert(Node node) {
        int distance ;

        distance = this.nodeID.getDistance(node.getNodeID()) ;

        RouteBucket routeBucket = routeBuckets[distance] ;
        routeBucket.insert(node);
    }

    public int getBucketID(Node nodeID) {
        return 0;
    }

    public void setUnresponsiveNodes(List<Node> nodes) {

    }

    public void setUnresponsiveNode(Node node) {

    }
}
