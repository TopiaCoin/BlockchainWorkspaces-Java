package io.topiacoin.dht.routing;

import io.topiacoin.dht.config.Configuration;
import io.topiacoin.dht.network.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class RouteBucket {

    private final Configuration configuration;

    private int distance;
    private TreeSet<NodeInfo> nodes;
    private TreeSet<NodeInfo> replacementCache;

    public RouteBucket(int distance, Configuration configuration) {
        this.configuration = configuration;
        this.distance = distance;
        this.nodes = new TreeSet<NodeInfo>();
        this.replacementCache = new TreeSet<NodeInfo>();
    }

    public int getDistance() {
        return distance;
    }

    public void insert(Node node) {
        NodeInfo newNodeInfo = new NodeInfo(node);

        if (nodes.contains(newNodeInfo)) {

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
            if (this.getNode().equals(o.getNode())) {
                return 0;
            }

            return (this.getLastContactTime() > o.getLastContactTime()) ? 1 : -1;
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
