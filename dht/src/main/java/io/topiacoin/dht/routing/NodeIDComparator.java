package io.topiacoin.dht.routing;

import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;

public class NodeIDComparator implements Comparator<Node> {
    private final BigInteger _target;

    public NodeIDComparator(NodeID targetID) {
        _target = new BigInteger(targetID.getNodeID());
    }

    /**
     * Compares the two Node's IDs to determine which one is closer to the target.
     *
     * @param n1 The first node to compare
     * @param n2 The second node to compare
     */
    public int compare(Node n1, Node n2) {

        BigInteger b1 = new BigInteger(n1.getNodeID().getNodeID());
        BigInteger b2 = new BigInteger(n2.getNodeID().getNodeID());

        b1 = b1.xor(this._target);
        b2 = b2.xor(this._target);

        return b1.abs().compareTo(b2.abs());
    }
}
