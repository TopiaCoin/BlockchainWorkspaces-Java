import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.messages.TestMessage;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class MessageFactoryTest {

    @Test
    public void testMessageFactory() {
        MessageFactory messageFactory = new MessageFactory() ;
        messageFactory.initialize();

        TestMessage testMessage = new TestMessage("arrivederci") ;

        ByteBuffer buffer = ByteBuffer.allocate(64000) ;
        testMessage.encodeMessage(buffer);
        buffer.flip();

        Message decodedMessage = messageFactory.createMessage(TestMessage.TYPE, buffer) ;

        assertEquals ( TestMessage.class, decodedMessage.getClass()) ;
        assertEquals ( testMessage.getMessage(), ((TestMessage)decodedMessage).getMessage() );
    }

}


