package io.topiacoin.dht.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Node {

    private NodeID nodeID;
    private InetAddress address;
    private int port;

    public Node(NodeID nodeID, InetAddress address, int port) {
        this.nodeID = nodeID;
        this.address = address;
        this.port = port;
    }

    public Node(NodeID nodeID, String address, int port) throws UnknownHostException {
        this.nodeID = nodeID;
        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    public NodeID getNodeID() {
        return nodeID;
    }

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
