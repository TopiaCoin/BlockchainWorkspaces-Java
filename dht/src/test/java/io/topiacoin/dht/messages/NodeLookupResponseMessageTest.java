package io.topiacoin.dht.messages;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NodeLookupResponseMessageTest implements MessageTest {

    private static List<NodeID> _nodeIDs;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Configuration configuration = new DefaultConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        _nodeIDs = new ArrayList<NodeID>();
        for ( int i = 0 ; i < 6 ; i++ ) {
            _nodeIDs.add(nodeIDGenerator.generateNodeID()) ;
        }
    }

    @Test
    public void testConstructorAndAccessors() throws Exception {

        NodeLookupResponseMessage testMessage = new NodeLookupResponseMessage(_nodeIDs) ;

        assertEquals(_nodeIDs, testMessage.getNodeIDs());
    }

    @Test
    public void testEncodingAndDecoding() throws Exception {

        NodeLookupResponseMessage testMessage = new NodeLookupResponseMessage(_nodeIDs) ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        NodeLookupResponseMessage decodedMessage = new NodeLookupResponseMessage(buffer) ;
        assertEquals ( testMessage.getNodeIDs(), ((NodeLookupResponseMessage)decodedMessage).getNodeIDs() );
        assertEquals ( testMessage, decodedMessage ) ;
    }
}
