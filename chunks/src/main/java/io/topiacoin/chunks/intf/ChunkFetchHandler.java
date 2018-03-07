package io.topiacoin.chunks.intf;

public interface ChunkFetchHandler {

    void didFetchChunk(String chunkID, Object state, byte[] chunkdata) ;

    void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) ;
}
