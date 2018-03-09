package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;

import java.nio.ByteBuffer;

public class NodeLookupMessage implements Message {

    private static final int TYPE = 0;
    private NodeID lookupID;

    public NodeLookupMessage(NodeID lookupID) {
        if ( lookupID == null ) {
            throw new IllegalArgumentException("Must specify a NodeID to lookup");
        }
        this.lookupID = lookupID ;
    }

    public NodeLookupMessage(ByteBuffer buffer) {
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

        NodeLookupMessage that = (NodeLookupMessage) o;

        return lookupID != null ? lookupID.equals(that.lookupID) : that.lookupID == null;
    }

    @Override
    public int hashCode() {
        return lookupID != null ? lookupID.hashCode() : 0;
    }
}
