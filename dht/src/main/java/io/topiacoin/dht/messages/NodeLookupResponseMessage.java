package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.network.NodeID;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class NodeLookupResponseMessage implements Message {

    private static final int TYPE = 2;

    private List<NodeID> nodeIDs;

    public NodeLookupResponseMessage(List<NodeID> nodeIDs) {
        if ( nodeIDs == null ) {
            throw new IllegalArgumentException("Must specify a list of NodeIDs");
        }
        this.nodeIDs = nodeIDs;
    }

    public NodeLookupResponseMessage(ByteBuffer buffer) {
        this.nodeIDs = new ArrayList<NodeID>();
        decodeMessage(buffer);
    }

    public byte getType() {
        return TYPE;
    }

    public List<NodeID> getNodeIDs() {
        return nodeIDs;
    }

    public void encodeMessage(ByteBuffer buffer) {

        buffer.putInt(nodeIDs.size());

        for ( NodeID nodeID : this.nodeIDs ) {
            byte[] nodeIDBytes = nodeID.getNodeID();
            byte[] validationBytes = nodeID.getValidation();

            buffer.putInt(nodeIDBytes.length) ;
            buffer.put(nodeIDBytes) ;
            buffer.putInt(validationBytes.length) ;
            buffer.put(validationBytes) ;
        }
    }

    public void decodeMessage(ByteBuffer buffer) {

        int count = buffer.getInt() ;

        for ( int i = 0 ; i < count ; i++ ){
            int nodeIDSize = buffer.getInt();
            byte[] nodeIDBytes = new byte[nodeIDSize] ;
            buffer.get(nodeIDBytes) ;
            int validationSize = buffer.getInt();
            byte[] validationBytes = new byte[validationSize] ;
            buffer.get(validationBytes) ;

            this.nodeIDs.add(new NodeID(nodeIDBytes, validationBytes)) ;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeLookupResponseMessage that = (NodeLookupResponseMessage) o;

        return nodeIDs.equals(that.nodeIDs);
    }

    @Override
    public int hashCode() {
        return nodeIDs.hashCode();
    }
}
