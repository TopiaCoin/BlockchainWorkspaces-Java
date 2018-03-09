package io.topiacoin.dht.routing;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RouteBucketTest {

    private static Configuration _configuration;

    @BeforeClass
    public static void setUpClass() throws Exception {
        _configuration = new DefaultConfiguration();
        _configuration.setC1(4);
        _configuration.setC2(8);
        _configuration.setK(5);
    }

    @Test
    public void testAddNodeToBucket() throws Exception {
        RouteBucket routeBucket = new RouteBucket(1, _configuration);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_configuration);

        NodeID nodeID = nodeIDGenerator.generateNodeID();
        Node node = new Node(nodeID, "localhost", 8345);

        routeBucket.insert(node);

        assertTrue(routeBucket.containsNode(node));
        assertEquals(1, routeBucket.getNodeInfos().size());
    }

    @Test
    public void testAddSeveralNodesToBucket() throws Exception {
        RouteBucket routeBucket = new RouteBucket(1, _configuration);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_configuration);

        List<Node> nodes = new ArrayList<Node>();

        int k = _configuration.getK();

        for (int i = 0; i < k; i++) {
            NodeID nodeID = nodeIDGenerator.generateNodeID();
            Node node = new Node(nodeID, "localhost", 8345);
            nodes.add(node);
            routeBucket.insert(node);
        }

        assertEquals(nodes.size(), routeBucket.getNodeInfos().size());

    }

    @Test
    public void testExtraNodesAddedToReplacementCache() throws Exception {
        RouteBucket routeBucket = new RouteBucket(1, _configuration);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_configuration);

        List<Node> nodes = new ArrayList<Node>();
        List<Node> extraNodes = new ArrayList<Node>();

        int k = _configuration.getK();

        for (int i = 0; i < k; i++) {
            NodeID nodeID = nodeIDGenerator.generateNodeID();
            Node node = new Node(nodeID, "localhost", 8345);
            nodes.add(node);
            routeBucket.insert(node);
        }

        assertEquals(nodes.size(), routeBucket.getNodeInfos().size());

        for (int i = 0; i < k; i++) {
            NodeID nodeID = nodeIDGenerator.generateNodeID();
            Node node = new Node(nodeID, "localhost", 8345);
            extraNodes.add(node);
            routeBucket.insert(node);
        }

        assertEquals(nodes.size(), routeBucket.getNodeInfos().size());
        assertEquals(extraNodes.size(), routeBucket.getReplacementCache().size());
    }

    @Test
    public void testExtraNodeReplacesStaleNode() throws Exception {
        RouteBucket routeBucket = new RouteBucket(1, _configuration);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_configuration);

        List<Node> nodes = new ArrayList<Node>();
        List<Node> extraNodes = new ArrayList<Node>();

        int k = _configuration.getK();

        for (int i = 0; i < k; i++) {
            NodeID nodeID = nodeIDGenerator.generateNodeID();
            Node node = new Node(nodeID, "localhost", 8345);
            nodes.add(node);
            routeBucket.insert(node);
        }

        System.out.println ( routeBucket ) ;
        assertEquals(nodes.size(), routeBucket.getNodeInfos().size());

        // Make once of the nodes in the bucket stale
        for( int i = 0 ; i < _configuration.getStaleLimit() + 1 ; i++ ) {
            routeBucket.getNodeInfos().get(0).markAsStale();
        }

        NodeID nodeID = nodeIDGenerator.generateNodeID();
        Node node = new Node(nodeID, "localhost", 8345);
        routeBucket.insert(node);

        System.out.println ( routeBucket ) ;
        assertEquals(nodes.size(), routeBucket.getNodeInfos().size());
        assertEquals(0, routeBucket.getReplacementCache().size());
    }

    @Test
    public void testAddingExistingNodeUpdatesNodeInfo() throws Exception {
        RouteBucket routeBucket = new RouteBucket(1, _configuration);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_configuration);

        List<Node> nodes = new ArrayList<Node>();

        NodeID nodeID = nodeIDGenerator.generateNodeID();
        Node node = new Node(nodeID, "localhost", 8345);
        nodes.add(node);
        routeBucket.insert(node);

        System.out.println ( routeBucket ) ;

        // Grab the existing info about the node
        RouteBucket.NodeInfo nodeInfo = routeBucket.getNodeInfo(node) ;
        long lastSeenTime = nodeInfo.getLastContactTime() ;
        int staleCount = nodeInfo.getStaleCount();

        // Wait a moment before updating
        Thread.sleep ( 100 ) ;

        // Insert the node again.
        routeBucket.insert(node);

        // Grab the info and make sure it has changed.
        nodeInfo = routeBucket.getNodeInfo(node);

        assertNotEquals(lastSeenTime, nodeInfo.getLastContactTime());
        assertEquals(0, nodeInfo.getStaleCount());
    }
}
