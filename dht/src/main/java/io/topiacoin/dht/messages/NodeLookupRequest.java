package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.network.NodeID;

import java.nio.ByteBuffer;

public class NodeLookupRequest implements Message {

    public static final int TYPE = (byte)0x03;
    private NodeID lookupID;

    public NodeLookupRequest(NodeID lookupID) {
        if ( lookupID == null ) {
            throw new IllegalArgumentException("Must specify a NodeID to lookup");
        }
        this.lookupID = lookupID ;
    }

    public NodeLookupRequest(ByteBuffer buffer) {
        decodeMessage(buffer);
    }

    public byte getType() {
        return TYPE;
    }

    public NodeID getLookupID() {
        return lookupID;
    }

    public void encodeMessage(ByteBuffer buffer) {
        byte[] nodeIDBytes = this.lookupID.getNodeID();
        buffer.putInt(nodeIDBytes.length) ;
        buffer.put(nodeIDBytes) ;
    }

    public void decodeMessage(ByteBuffer buffer) {
        int size = buffer.getInt();
        byte[] nodeIDBytes = new byte[size] ;
        buffer.get(nodeIDBytes) ;

        this.lookupID = new NodeID(nodeIDBytes, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeLookupRequest that = (NodeLookupRequest) o;

        return lookupID != null ? lookupID.equals(that.lookupID) : that.lookupID == null;
    }

    @Override
    public int hashCode() {
        return lookupID != null ? lookupID.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "NodeLookupMessage{" +
                "lookupID=" + lookupID +
                '}';
    }
}
