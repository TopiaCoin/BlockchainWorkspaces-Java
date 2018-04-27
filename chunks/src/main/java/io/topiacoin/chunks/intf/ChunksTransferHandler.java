package io.topiacoin.chunks.intf;

public interface ChunksTransferHandler {

    void didFetchChunk(String chunkID, Object state) ;

    void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) ;

    void fetchedAllChunks(Object state);

    void failedToBuildFetchPlan();
}
