package io.topiacoin.dht.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Node {

    private InetAddress address;
    private int port;

    public Node(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public Node(String address, int port) throws UnknownHostException {
        this.address = InetAddress.getByName(address);
        this.port = port;
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
