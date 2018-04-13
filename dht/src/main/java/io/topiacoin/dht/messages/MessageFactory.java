package io.topiacoin.dht.messages;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.handlers.ConnectRequestHandler;
import io.topiacoin.dht.handlers.FetchValueRequestHandler;
import io.topiacoin.dht.handlers.NodeLookupRequestHandler;
import io.topiacoin.dht.handlers.RemoveValueRequestHandler;
import io.topiacoin.dht.handlers.StoreValueRequestHandler;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;

import java.nio.ByteBuffer;

public class MessageFactory {

    private DHTComponents _dhtComponents ;

    public MessageFactory() {
    }

    public void initialize() {

    }

    public DHTComponents getDhtComponents() {
        return _dhtComponents;
    }

    public void setDhtComponents(DHTComponents dhtComponents) {
        _dhtComponents = dhtComponents;
    }

    public Message createMessage(byte msgType, ByteBuffer messageData) {

        Message message = null;

        switch (msgType) {
            case ConnectRequest.TYPE:
                message = new ConnectRequest();
                break;
            case ConnectResponse.TYPE:
                message = new ConnectResponse();
                break;
            case NodeLookupRequest.TYPE:
                message = new NodeLookupRequest(messageData);
                break;
            case NodeLookupResponse.TYPE:
                message = new NodeLookupResponse(messageData);
                break;
            case StoreValueRequest.TYPE :
                message = new StoreValueRequest(messageData);
                break;
            case StoreValueResponse.TYPE:
                message = new StoreValueResponse(messageData);
                break;
            case FetchValueRequest.TYPE :
                message = new FetchValueRequest(messageData);
                break;
            case FetchValueResponse.TYPE:
                message = new FetchValueResponse(messageData);
                break;
            case RemoveValueRequest.TYPE:
                message = new RemoveValueRequest(messageData);
                break ;
            case RemoveValueResponse.TYPE:
                message = new RemoveValueResponse(messageData);
                break;
            default:
                System.err.println("Unable to create message - Unrecognized msgType: " + msgType);
        }

        return message;
    }

    public ResponseHandler createReceiver(byte msgType) {
        ResponseHandler responseHandler = null;

        switch (msgType) {
            case ConnectRequest.TYPE:
                responseHandler = new ConnectRequestHandler(_dhtComponents);
                break;
            case NodeLookupRequest.TYPE:
                responseHandler = new NodeLookupRequestHandler(_dhtComponents);
                break;
            case StoreValueRequest.TYPE:
                responseHandler = new StoreValueRequestHandler(_dhtComponents);
                break;
            case FetchValueRequest.TYPE:
                responseHandler = new FetchValueRequestHandler(_dhtComponents);
                break;
            case RemoveValueRequest.TYPE:
                responseHandler = new RemoveValueRequestHandler(_dhtComponents);
            default:
                System.err.println("Unable to create handler - Unrecognized msgType: " + msgType);
        }

        return responseHandler;
    }
}
