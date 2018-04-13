package io.topiacoin.dht.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Node implements Comparable<Node>{

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

    public Node(ByteBuffer buffer) throws IOException {
        decode(buffer);
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

    public void encode(ByteBuffer buffer) {
        nodeID.encode(buffer);
        byte[] addressBytes = this.address.getAddress();
        buffer.putInt(addressBytes.length);
        buffer.put(addressBytes);
        buffer.putInt(port);
    }

    private void decode(ByteBuffer buffer) throws IOException {
        nodeID = NodeID.decode(buffer);
        int addressLength = buffer.getInt();
        byte[] addressBytes = new byte[addressLength] ;
        buffer.get(addressBytes);
        address = InetAddress.getByAddress(addressBytes);
        port = buffer.getInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        return nodeID != null ? nodeID.equals(node.nodeID) : node.nodeID == null;
    }

    @Override
    public int hashCode() {
        return nodeID != null ? nodeID.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeID=" + nodeID +
                ", address=" + address.getHostAddress() +
                ", port=" + port +
                '}';
    }

    public int compareTo(Node o) {
        // Compare the NodeIDs first, then the InetAddresses, and finally the port numbers
        int nodeCompare = this.nodeID.compareTo(o.nodeID) ;
        if ( nodeCompare != 0 )
            return nodeCompare ;

        // NodeIDs match, so now we compare the InetAddresses.
        byte[] thisAddress = address.getAddress();
        byte[] thatAddress = o.address.getAddress();

        // Inet4Addresses are "less than" Inet6Addresses
        if ( thisAddress.length < thatAddress.length )
            return -1 ;
        if ( thisAddress.length > thatAddress.length )
            return 1 ;

        // Addresses are the same size, so compare them byte by byte, ordering by byte.
        for ( int i = 0 ; i < thisAddress.length ; i++ ) {
            if ( thisAddress[i] < thatAddress[i] ) return -1;
            if ( thisAddress[i] > thatAddress[i] ) return 1;
        }

        // Addresses are identical.  Compare ports

        if ( this.port < o.port ) return -1 ;
        if ( this.port > o.port ) return 1 ;

        return 0;
    }
}
