package io.topiacoin.dht.routing;

import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.network.Node;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class RouteBucket {

    private final DHTConfiguration configuration;

    private int distance;
    private TreeSet<NodeInfo> nodes;
    private TreeSet<NodeInfo> replacementCache;

    public RouteBucket(int distance, DHTConfiguration configuration) {
        this.configuration = configuration;
        this.distance = distance;
        this.nodes = new TreeSet<NodeInfo>();
        this.replacementCache = new TreeSet<NodeInfo>();
    }

    public RouteBucket(ByteBuffer buffer, DHTConfiguration configuration) throws IOException {
        this.configuration = configuration;
        this.nodes = new TreeSet<NodeInfo>();
        this.replacementCache = new TreeSet<NodeInfo>();

        decode(buffer);
    }

    public int getDistance() {
        return distance;
    }

    public void insert(Node node) {
        NodeInfo newNodeInfo = new NodeInfo(node);

        if (containsNode(node)) {

            // Grab the existing nodeInfo object out of the list, mark it as seen, then put it back in.
            // This will insure it re-sorts correctly.
            newNodeInfo = this.getNodeInfo(node);
            this.nodes.remove(newNodeInfo);
            newNodeInfo.markAsSeen();
            this.nodes.add(newNodeInfo);

        } else {
            if (nodes.size() < this.configuration.getK()) {
                // There is room in the node list, so lets add this node.
                nodes.add(newNodeInfo);
            } else {
                NodeInfo stalest = null;
                // The node list is full.  Check to see if there is anything in the replacement cache.
                if (replacementCache.size() == 0) {
                    // Replacement cache is empty.  Lets see if we have any stale nodes in the node list.
                    for (NodeInfo nodeInfo : nodes) {
                        if (nodeInfo.getStaleCount() > this.configuration.getStaleLimit()) {
                            if (stalest == null) {
                                stalest = nodeInfo;
                            } else if (nodeInfo.getStaleCount() > stalest.getStaleCount()) {
                                stalest = nodeInfo;
                            }
                        }
                    }
                }

                if (stalest != null) {
                    // There is a stale node in the node list, swap it for the new node.
                    nodes.remove(stalest);
                    nodes.add(newNodeInfo);
                } else {
                    // No stale nodes were found, stick this into the replacement cache.
                    if ( replacementCache.size() > this.configuration.getK() ) {
                        // The replacement cache is full, so remove the last one, which is the oldest.
                        replacementCache.remove(replacementCache.last()) ;
                    }
                    replacementCache.add(newNodeInfo);
                }
            }
        }
    }

    public NodeInfo getNodeInfo(Node node) {
        for (NodeInfo nodeInfo : this.nodes) {
            if (nodeInfo.getNode().equals(node)) {
                return nodeInfo;
            }
        }
        return null;
    }

    public boolean containsNode(Node node) {
        return this.nodes.contains(new NodeInfo(node));
    }

    public void remove(Node node) {
        this.nodes.remove(new NodeInfo(node));
    }

    public int numNodes() {
        return this.nodes.size();
    }

    public List<NodeInfo> getNodeInfos() {
        return new ArrayList<NodeInfo>(this.nodes);
    }

    public List<NodeInfo> getReplacementCache() {
        return new ArrayList<NodeInfo>(this.replacementCache);
    }

    public void encode(ByteBuffer buffer) {
        buffer.putInt(distance);
        buffer.putInt(nodes.size());
        for (NodeInfo nodeInfo : nodes) {
            nodeInfo.encode(buffer) ;
        }
        buffer.putInt(replacementCache.size());
        for ( NodeInfo nodeInfo : replacementCache ) {
            nodeInfo.encode(buffer) ;
        }
    }

    private void decode(ByteBuffer buffer) throws IOException {
        distance = buffer.getInt();
        int nodeCount = buffer.getInt();
        for (int i = 0; i < nodeCount; i++) {
            NodeInfo nodeInfo = new NodeInfo(buffer);
            nodes.add(nodeInfo);
        }
        int replacementCount = buffer.getInt();
        for (int i = 0; i < replacementCount; i++) {
            NodeInfo nodeInfo = new NodeInfo(buffer);
            replacementCache.add(nodeInfo);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteBucket that = (RouteBucket) o;

        if (distance != that.distance) return false;
        return nodes.equals(that.nodes);
    }

    @Override
    public int hashCode() {
        int result = distance;
        result = 31 * result + nodes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RouteBucket{" +
                "configuration=" + configuration +
                ", distance=" + distance +
                ", nodes=" + nodes +
                ", replacementCache=" + replacementCache +
                '}';
    }

    // -------- Inner Classes --------

    static class NodeInfo implements Comparable<NodeInfo> {
        private Node node;
        private long lastContactTime;
        private int staleCount;

        public NodeInfo(Node node) {
            this.node = node;
            this.lastContactTime = System.currentTimeMillis();
        }

        public NodeInfo(ByteBuffer buffer) throws IOException  {
            decode(buffer);
        }

        public void markAsSeen() {
            this.lastContactTime = System.currentTimeMillis();
            this.staleCount = 0;
        }

        public Node getNode() {
            return node;
        }

        public long getLastContactTime() {
            return lastContactTime;
        }

        public int getStaleCount() {
            return staleCount;
        }

        public void markAsStale() {
            staleCount++;
        }

        public void encode(ByteBuffer buffer) {
            node.encode(buffer);
        }

        private void decode(ByteBuffer buffer) throws IOException {
            this.node = new Node(buffer);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NodeInfo nodeInfo = (NodeInfo) o;

            return node != null ? node.equals(nodeInfo.node) : nodeInfo.node == null;
        }

        @Override
        public int hashCode() {
            return node != null ? node.hashCode() : 0;
        }

        public int compareTo(NodeInfo o) {
            int nodeCompare = this.getNode().compareTo(o.getNode());
            if (nodeCompare != 0) return nodeCompare;

            return nodeCompare;
//            return (this.getLastContactTime() > o.getLastContactTime()) ? 1 : -1;
        }

        @Override
        public String toString() {
            return "NodeInfo{" +
                    "node=" + node +
                    ", lastContactTime=" + lastContactTime +
                    ", staleCount=" + staleCount +
                    '}';
        }
    }
}
