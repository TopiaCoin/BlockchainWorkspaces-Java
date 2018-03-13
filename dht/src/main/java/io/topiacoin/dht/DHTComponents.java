package io.topiacoin.dht;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.content.ValueStorage;
import io.topiacoin.dht.messages.MessageFactory;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.routing.RoutingTable;

public class DHTComponents {

    private CommunicationServer _communicationServer;
    private RoutingTable _routingTable;
    private MessageSigner _messageSigner;
    private Configuration _configuration;
    private MessageFactory _messageFactory;
    private ValueStorage _valueStorage;

    public CommunicationServer getCommunicationServer() {
        return _communicationServer;
    }

    public void setCommunicationServer(CommunicationServer communicationServer) {
        _communicationServer = communicationServer;
    }

    public RoutingTable getRoutingTable() {
        return _routingTable;
    }

    public void setRoutingTable(RoutingTable routingTable) {
        _routingTable = routingTable;
    }

    public MessageSigner getMessageSigner() {
        return _messageSigner;
    }

    public void setMessageSigner(MessageSigner messageSigner) {
        _messageSigner = messageSigner;
    }

    public Configuration getConfiguration() {
        return _configuration;
    }

    public void setConfiguration(Configuration configuration) {
        _configuration = configuration;
    }

    public MessageFactory getMessageFactory() {
        return _messageFactory;
    }

    public void setMessageFactory(MessageFactory messageFactory) {
        _messageFactory = messageFactory;
    }

    public void setValueStorage(ValueStorage valueStorage) {
        _valueStorage = valueStorage;
    }

    public ValueStorage getValueStorage() {
        return _valueStorage;
    }
}
