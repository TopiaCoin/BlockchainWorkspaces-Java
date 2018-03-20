package io.topiacoin.chunks.intf;

public interface ChunksTransferHandler {

    void didFetchChunk(String chunkID, byte[] chunkdata, Object state) ;

    void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) ;

    void fetchedAllChunks(Object state);
}
