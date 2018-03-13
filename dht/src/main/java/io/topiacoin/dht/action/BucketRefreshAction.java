package io.topiacoin.dht.action;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.NodeLookupRequest;
import io.topiacoin.dht.network.CommunicationServer;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.routing.RoutingTable;

public class BucketRefreshAction implements Action {

    private final Node _thisNode;
    private final DHTComponents _dhtComponents;

    private final CommunicationServer _communicationServer;
    private final RoutingTable _routingTable;

    public BucketRefreshAction(Node thisNode, DHTComponents dhtComponents) {
        _thisNode = thisNode;
        _dhtComponents = dhtComponents;

        _communicationServer = _dhtComponents.getCommunicationServer();
        _routingTable = _dhtComponents.getRoutingTable();
    }

    public void execute() {
        System.out.println ( "Executing a Bucket Refresh" ) ;

        // For each Bucket in the Routing Table, perform a Node Lookup

        // Populate the Routing Table by asking the bootstrap node for nodes that fit in each of our buckets

        for (int i = 0; i < 160; i++) {
            NodeID bucketNodeID = _thisNode.getNodeID().generateNodeIDByDistance(i);
            NodeLookupRequest nodeLookupMessage = new NodeLookupRequest(bucketNodeID);

            new NodeLookupAction(_thisNode, bucketNodeID, this._dhtComponents).execute();
        }

        System.out.println ( "Executed a Bucket Refresh" ) ;
    }
}
