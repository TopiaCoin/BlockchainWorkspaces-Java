package io.topiacoin.chunks.intf;

import java.io.ByteArrayInputStream;

public interface ChunkFetchHandler {

    void didFetchChunk(String chunkID, Object state) ;

    void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) ;
}
