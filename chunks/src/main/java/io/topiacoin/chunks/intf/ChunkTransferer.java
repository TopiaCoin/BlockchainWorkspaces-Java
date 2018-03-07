package io.topiacoin.chunks.intf;

import java.util.List;
import java.util.Map;

public interface ChunkTransferer {

	/**
	 * Given a list of chunkIDs (paired with their associated handlers), a container/workspace ID, and an optional state Object, find the chunks in the network
	 * and fetch their binary data. Once a chunk is fetched, the appropriate ChunkFetchHandler's didFetchChunk() will be invoked with the data. Should a chunk fail to download, its
	 * handler's failedToFetchChunk() will be invoked (but the list will continue to be processed)
	 * @param chunkIDs a Map of ChunkIDs to their appropriate handlers. The keys in this map will be used to determine which chunks should be downloaded
	 * @param containerID The ID of the container which these chunks belong to
	 * @param state An opaque object that will be passed to the handler on fetch operation completion.  This can be used to carry state between the initiator of the fetch and the handler.
	 */
	public void fetchChunksRemotely(final Map<String, ChunkFetchHandler> chunkIDs, final String containerID, final Object state);
}
