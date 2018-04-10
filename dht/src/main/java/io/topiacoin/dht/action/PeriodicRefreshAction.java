package io.topiacoin.dht.action;

import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.network.Node;

public class PeriodicRefreshAction implements Action{

    private final Node _thisNode;
    private final DHTComponents _dhtComponents;

    public PeriodicRefreshAction(Node thisNode, DHTComponents dhtComponents) {
        _thisNode = thisNode;
        this._dhtComponents = dhtComponents;
    }

    public void execute() {
        System.out.println ( "Executing a Periodic Refresh" ) ;

        // Perform a Bucket Refresh to update the Routing Table
        BucketRefreshAction bucketRefreshAction = new BucketRefreshAction(_thisNode, _dhtComponents);
        bucketRefreshAction.execute();

        // Perform a Content Refresh to insure that content is replicated, and to purge content from our storage
        ContentRefreshAction contentRefreshAction = new ContentRefreshAction(_thisNode, _dhtComponents);
        contentRefreshAction.execute();
    }

}
