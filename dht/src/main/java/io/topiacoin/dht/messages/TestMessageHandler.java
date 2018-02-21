package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;

public class TestMessageHandler implements ResponseHandler{
    public void receive(Message msg, int msgID) {

    }

    public void timeout(int msgID) {

    }
}
