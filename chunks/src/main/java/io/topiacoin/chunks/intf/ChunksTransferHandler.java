package io.topiacoin.chunks.intf;

public interface ChunksTransferHandler {

    void didFetchChunk(String chunkID, ChunkRetrievalStrategy strategy, Object state) ;

    void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) ;

    void fetchedAllChunksSuccessfully(Object state);

    void failedToBuildFetchPlan(Object state);

    void fetchPlanBuiltSuccessfully(ChunkRetrievalStrategy strategy, Object state);

    void failedToFetchAllChunks(Object state);
}
