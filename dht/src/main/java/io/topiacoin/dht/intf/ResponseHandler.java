package io.topiacoin.dht.intf;

public interface ResponseHandler {

    void receive(Message msg, int msgID) ;
    void timeout(int msgID) ;
}
