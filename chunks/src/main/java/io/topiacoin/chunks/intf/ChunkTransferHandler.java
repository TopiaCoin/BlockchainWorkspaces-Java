package io.topiacoin.chunks.intf;

public interface ChunkTransferHandler {
	void didFetchChunk(String chunkID, byte[] chunkdata) ;

	void failedToFetchChunk(String chunkID, String message, Exception cause) ;
}
