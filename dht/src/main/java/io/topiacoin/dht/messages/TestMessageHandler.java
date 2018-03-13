package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.network.Node;

public class TestMessageHandler implements ResponseHandler{
    public void receive(Node origin, Message msg, int msgID) {

    }

    public void timeout(int msgID) {

    }
}
