package io.topiacoin.dht.messages;

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

import static org.junit.Assert.*;

public class NodeLookupResponseTest implements MessageTest {

    private static List<NodeID> _nodeIDs;
    private static List<Node> _nodes;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Configuration configuration = new DefaultConfiguration();
        configuration.setC1(4);
        configuration.setC2(8);

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        _nodeIDs = new ArrayList<NodeID>();
        _nodes = new ArrayList<Node>();
        for ( int i = 0 ; i < 6 ; i++ ) {
            NodeID nodeID = nodeIDGenerator.generateNodeID();
            _nodeIDs.add(nodeID) ;
            Node node = new Node(nodeID, "localhost", 12345);
            _nodes.add(node);
        }
    }

    @Test
    public void testConstructorAndAccessors() throws Exception {

        NodeLookupResponse testMessage = new NodeLookupResponse(_nodes) ;

        assertEquals(_nodes, testMessage.getNodes());
    }

    @Test
    public void testEncodingAndDecoding() throws Exception {

        NodeLookupResponse testMessage = new NodeLookupResponse(_nodes) ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        NodeLookupResponse decodedMessage = new NodeLookupResponse(buffer) ;
        assertEquals ( testMessage.getNodes(), ((NodeLookupResponse)decodedMessage).getNodes() );
        assertEquals ( testMessage, decodedMessage ) ;
    }
}
