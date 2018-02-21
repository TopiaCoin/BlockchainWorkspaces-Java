package io.topiacoin.dht.messages;

import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;

import java.nio.ByteBuffer;
import java.util.Map;

public class MessageFactory {

    private Map<Byte, Class<? extends Message>> messageMap;

    public Message createMessage(byte msgType, ByteBuffer messageData) {

        Message message = null;

        switch (msgType) {
            case TestMessage.TYPE:
                message = new TestMessage(messageData);
                break;
        }

        return message;
    }

    public ResponseHandler createReceiver(byte msgType, CommunicationServer communicationServer) {
        return null;
    }
}
