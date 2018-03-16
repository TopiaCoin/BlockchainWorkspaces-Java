package io.topiacoin.dht.network;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class NodeTest {

    @Test
    public void testEncodeDecode() throws  Exception {
        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);
        NodeID nodeID =  nodeIDGenerator.generateNodeID();

        Node node = new Node(nodeID, "localhost", 6654);

        ByteBuffer buffer = ByteBuffer.allocate(65536);

        node.encode(buffer);
        buffer.flip();

        Node decodedNode = new Node(buffer) ;

        assertEquals ( node, decodedNode) ;
    }
}
