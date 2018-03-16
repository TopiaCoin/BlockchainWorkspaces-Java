package io.topiacoin.dht.intf;

import io.topiacoin.dht.network.Node;

public interface ResponseHandler {

    void receive(Node origin, Message msg, int msgID) ;
    void timeout(int msgID) ;
}
