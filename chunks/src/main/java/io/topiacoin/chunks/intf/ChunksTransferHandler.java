package io.topiacoin.chunks.intf;

public interface ChunksTransferHandler {

    void didFetchChunk(String chunkID, Object state) ;

    void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) ;

    void fetchedAllChunksSuccessfully(Object state);

    void failedToBuildFetchPlan();

    void failedToFetchAllChunks(Object state);
}
