package io.topiacoin.dht.messages;

import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodeLookupResponse implements Message {

    private final Log _log = LogFactory.getLog(this.getClass());

    public static final int TYPE = (byte)0x83;

    private List<Node> nodes;

    public NodeLookupResponse(List<Node> nodeIDs) {
        if ( nodeIDs == null ) {
            throw new IllegalArgumentException("Must specify a list of NodeIDs");
        }
        this.nodes = nodeIDs;
    }

    public NodeLookupResponse(ByteBuffer buffer) {
        this.nodes = new ArrayList<Node>();
        decodeMessage(buffer);
    }

    public byte getType() {
        return TYPE;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void encodeMessage(ByteBuffer buffer) {

        buffer.putInt(nodes.size());

        for ( Node node : this.nodes) {
            node.getNodeID().encode(buffer);

            byte[] address = node.getAddress().getAddress();
            int addressLength = address.length;
            int port = node.getPort();

            buffer.putInt(addressLength) ;
            buffer.put(address);
            buffer.putInt(port);
        }
    }

    public void decodeMessage(ByteBuffer buffer) {

        int count = buffer.getInt() ;

        for ( int i = 0 ; i < count ; i++ ){
            NodeID nodeID = NodeID.decode(buffer);

            byte[] address;
            int addressLength;
            int port ;

            addressLength = buffer.getInt();
            address = new byte[addressLength];
            buffer.get(address) ;
            port = buffer.getInt();

            try {
                InetAddress inetAddress = InetAddress.getByAddress(address);

                Node node = new Node(nodeID, inetAddress, port);
                this.nodes.add(node);
            } catch ( UnknownHostException e ) {
                _log.warn ( "Unrecognized address: " + Arrays.toString(address), e) ;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeLookupResponse that = (NodeLookupResponse) o;

        return nodes.equals(that.nodes);
    }

    @Override
    public int hashCode() {
        return nodes.hashCode();
    }

    @Override
    public String toString() {
        return "NodeLookupResponseMessage{" +
                "nodes=" + nodes +
                '}';
    }
}
