package io.topiacoin.workspace.blockchain.eos;

import java.util.List;

public class TableResponse<T> {
    private List<T> items;
    private boolean hasMore;
    private Object continuationToken;

    public TableResponse(List<T> items, boolean hasMore, Object continuationToken) {
        this.items = items;
        this.hasMore = hasMore;
        this.continuationToken = continuationToken;
    }

    protected List<T> getItems() {
        return items;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public Object getContinuationToken() {
        return continuationToken;
    }
}
