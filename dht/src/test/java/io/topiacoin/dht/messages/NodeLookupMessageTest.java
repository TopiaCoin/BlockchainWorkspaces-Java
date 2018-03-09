package io.topiacoin.dht.messages;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.network.NodeIDGenerator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class NodeLookupMessageTest implements MessageTest {

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

        NodeLookupMessage testMessage = new NodeLookupMessage(lookupNodeID) ;

        assertEquals(lookupNodeID, testMessage.getLookupID());
    }

    @Test
    public void testEncodingAndDecoding() {

        NodeID lookupNodeID = _baseNodeID.generateNodeIDByDistance(16) ;

        NodeLookupMessage testMessage = new NodeLookupMessage(lookupNodeID) ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        NodeLookupMessage decodedMessage = new NodeLookupMessage(buffer) ;
        assertEquals ( testMessage.getLookupID(), ((NodeLookupMessage)decodedMessage).getLookupID() );
        assertEquals ( testMessage, decodedMessage ) ;
    }
}
