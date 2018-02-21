package io.topiacoin.dht;

import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.network.CommunicationServer;
import org.junit.Test;

public class CommunicationServerTest {

    @Test
    public void testSendingAndReceivingMessages() throws Exception {
        DefaultConfiguration config = new DefaultConfiguration();
        MessageFactory messageFactory = new MessageFactory();

        CommunicationServer communicationServer = new CommunicationServer(12345, config, messageFactory);


    }

}
