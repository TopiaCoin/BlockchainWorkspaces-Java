package io.topiacoin.dht.routing;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    @Test
    public void testGetAllNodes() throws  Exception {
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
    }

    @Test
    public void testNodeInfoSetHandling() throws Exception {
        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_configuration);

        NodeID nodeID1 = nodeIDGenerator.generateNodeID();
        NodeID nodeID2 = nodeIDGenerator.generateNodeID();

        Node node1 = new Node(nodeID1, "localhost", 34567) ;
        Node node1a = new Node(nodeID1, "localhost", 34567) ;
        Node node2 = new Node(nodeID2, "localhost", 34568) ;

        RouteBucket.NodeInfo info1 = new RouteBucket.NodeInfo(node1) ;
        Thread.sleep ( 10) ;
        RouteBucket.NodeInfo info1a = new RouteBucket.NodeInfo(node1) ;
        Thread.sleep ( 10) ;
        RouteBucket.NodeInfo info2 = new RouteBucket.NodeInfo(node2) ;

        Set<RouteBucket.NodeInfo> sortedSet = new TreeSet<RouteBucket.NodeInfo>();

        // Add Order -- 1 -> 2 -> 1a
        assertEquals(0, sortedSet.size());
        sortedSet.add(info1) ;
        assertEquals(1, sortedSet.size());
        sortedSet.add(info2);
        assertEquals(2, sortedSet.size());
        sortedSet.add(info1a) ;
        assertEquals(2, sortedSet.size()) ;

        System.out.println ( sortedSet ) ;


        // Add Order -- 1 -> 1a -> 2
        sortedSet.clear();
        assertEquals(0, sortedSet.size());
        sortedSet.add(info1) ;
        assertEquals(1, sortedSet.size());
        sortedSet.add(info1a) ;
        assertEquals(1, sortedSet.size()) ;
        sortedSet.add(info2);
        assertEquals(2, sortedSet.size());

        System.out.println ( sortedSet ) ;


        // Add Order -- 2 -> 1 -> 1a
        sortedSet.clear();
        assertEquals(0, sortedSet.size());
        sortedSet.add(info2);
        assertEquals(1, sortedSet.size());
        sortedSet.add(info1) ;
        assertEquals(2, sortedSet.size()) ;
        sortedSet.add(info1a) ;
        assertEquals(2, sortedSet.size());

        System.out.println ( sortedSet ) ;

    }

    @Test
    public void testInsertingNodes() throws Exception {
        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_configuration);

        NodeID nodeID1 = nodeIDGenerator.generateNodeID();
        NodeID nodeID2 = nodeIDGenerator.generateNodeID();

        System.out.println("Node 1: " + nodeID1);
        System.out.println("Node 2: " + nodeID2);

        Node node1 = new Node(nodeID1, "localhost", 34567) ;
        Node node1a = new Node(nodeID1, "localhost", 34567) ;
        Node node2 = new Node(nodeID2, "localhost", 34568) ;


        RouteBucket routeBucket = new RouteBucket(1, _configuration) ;

        // Add Order -- 1 -> 2 -> 1a
        assertEquals(0, routeBucket.numNodes());
        routeBucket.insert(node1);
        assertEquals(1, routeBucket.numNodes());
        routeBucket.insert(node2);
        assertEquals(2, routeBucket.numNodes());
        routeBucket.insert(node1a);
        assertEquals(2, routeBucket.numNodes()) ;

        System.out.println ( routeBucket ) ;


        // Add Order -- 1 -> 1a -> 2
        routeBucket = new RouteBucket(1, _configuration) ;
        assertEquals(0, routeBucket.numNodes());
        routeBucket.insert(node1);
        assertEquals(1, routeBucket.numNodes());
        routeBucket.insert(node1a);
        assertEquals(1, routeBucket.numNodes());
        routeBucket.insert(node2);
        assertEquals(2, routeBucket.numNodes()) ;

        System.out.println ( routeBucket ) ;


        // Add Order -- 2 -> 1 -> 1a
        routeBucket = new RouteBucket(1, _configuration) ;
        assertEquals(0, routeBucket.numNodes());
        routeBucket.insert(node2);
        assertEquals(1, routeBucket.numNodes());
        routeBucket.insert(node1);
        assertEquals(2, routeBucket.numNodes());
        routeBucket.insert(node1a);
        assertEquals(2, routeBucket.numNodes()) ;

        System.out.println ( routeBucket ) ;

    }

    @Test
    public void testEncodeDecode() throws Exception {
        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_configuration);

        NodeID nodeID1 = nodeIDGenerator.generateNodeID();
        NodeID nodeID2 = nodeIDGenerator.generateNodeID();

        System.out.println("Node 1: " + nodeID1);
        System.out.println("Node 2: " + nodeID2);

        Node node1 = new Node(nodeID1, "localhost", 34567) ;
        Node node2 = new Node(nodeID2, "localhost", 34568) ;

        RouteBucket routeBucket = new RouteBucket(1, _configuration);

        routeBucket.insert(node1);
        routeBucket.insert(node2);

        ByteBuffer buffer = ByteBuffer.allocate(65536);

        routeBucket.encode(buffer);

        buffer.flip();

        RouteBucket decodedBucket = new RouteBucket(buffer, _configuration) ;

        assertTrue ("Decoded Bucket does not contain Node 1", decodedBucket.containsNode(node1));
        assertTrue ("Decoded Bucket does not contain Node 2", decodedBucket.containsNode(node2));
        assertEquals("Wrong number of Nodes in Decoded Bucket", routeBucket.numNodes(), decodedBucket.numNodes()) ;
    }
}
