package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.intf.ChunkTransferer;
import io.topiacoin.chunks.intf.ChunksTransferHandler;

import java.util.List;

public class SecrataEnterpriseChunkTransferer implements ChunkTransferer {

	/**
	 *
	 * @param chunkIDs a List of ChunkIDs to be transferred. The items in this list will be used to determine which chunks should be downloaded
	 * @param containerID The ID of the container which these chunks belong to
	 * @param handler The Handler for notifying when each chunk succeeds/fails to transfer, and for when the whole list is done being processed
	 * @param state An opaque object that will be passed to the handler on fetch operation completion.  This can be used to carry state between the initiator of the fetch and the handler.
	 */
	@Override public void fetchChunksRemotely(List<String> chunkIDs, String containerID, ChunksTransferHandler handler, Object state) {
		/*
		foreach chunkID in chunkIDs {
			synchronously fetch chunk whose ID is chunkID
			if(200) {
				handler.didFetchChunk(chunkID, state, resp.data)
			} else {
				handler.failedToFetchChunk(...)
			}
		}
		handler.fetchedAllChunks(state);
		 */
	}
}
