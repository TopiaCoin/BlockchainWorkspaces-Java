package io.topiacoin.dht.routing;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.Test;

import java.util.Random;

public class RoutingTableTest {

    @Test
    public void testInsertingNode() throws Exception {
        Random random = new Random();
        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        DHTComponents dhtComponents = new DHTComponents();
        dhtComponents.setConfiguration(configuration);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);
        NodeID thisNodeID = nodeIDGenerator.generateNodeID();
        RoutingTable routingTable = new RoutingTable();
        routingTable.setNodeID(thisNodeID);
        routingTable.setDhtComponents(dhtComponents);

        routingTable.initialize();

        for (int i = 0; i < 50; i++) {
            NodeID nodeID = nodeIDGenerator.generateNodeID();
            int port = random.nextInt(65535);
            Node node = new Node(nodeID, "localhost", port);
            routingTable.insert(node);
        }

        System.out.println(routingTable);
    }

    @Test
    public void testInsertingNodeMultipleTimes() throws Exception {
        Random random = new Random();
        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        DHTComponents dhtComponents = new DHTComponents();
        dhtComponents.setConfiguration(configuration);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);
        NodeID thisNodeID = nodeIDGenerator.generateNodeID();
        RoutingTable routingTable = new RoutingTable();
        routingTable.setNodeID(thisNodeID);
        routingTable.setDhtComponents(dhtComponents);

        routingTable.initialize();

        NodeID nodeID = nodeIDGenerator.generateNodeID();
        int port = random.nextInt(65535);
        Node node = new Node(nodeID, "localhost", port);

        for (int i = 0; i < 50; i++) {
            routingTable.insert(node);
            Thread.sleep ( 10) ;
        }

        System.out.println(routingTable);
    }
}
