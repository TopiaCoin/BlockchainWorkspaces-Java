package io.topiacoin.dht.messages;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class NodeLookupRequestTest implements MessageTest {

    private static NodeID _baseNodeID;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Configuration configuration = new DefaultConfiguration();

        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(configuration);

        _baseNodeID = nodeIDGenerator.generateNodeID();
    }

    @Test
    public void testConstructorAndAccessors() throws Exception {
        NodeID lookupNodeID = _baseNodeID.generateNodeIDByDistance(16) ;

        NodeLookupRequest testMessage = new NodeLookupRequest(lookupNodeID) ;

        assertEquals(lookupNodeID, testMessage.getLookupID());
    }

    @Test
    public void testEncodingAndDecoding() {

        NodeID lookupNodeID = _baseNodeID.generateNodeIDByDistance(16) ;

        NodeLookupRequest testMessage = new NodeLookupRequest(lookupNodeID) ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        NodeLookupRequest decodedMessage = new NodeLookupRequest(buffer) ;
        assertEquals ( testMessage.getLookupID(), ((NodeLookupRequest)decodedMessage).getLookupID() );
        assertEquals ( testMessage, decodedMessage ) ;
    }
}
