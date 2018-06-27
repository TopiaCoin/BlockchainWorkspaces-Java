package io.topiacoin.chunks.intf;

import io.topiacoin.model.DataModel;
import io.topiacoin.model.UserNode;

import java.util.List;

public interface ChunkTransferer {

	/**
	 * Given a list of chunkIDs, a container/workspace ID, a handler, and an optional state Object, find the chunks in the network
	 * and fetch their binary data. Once a chunk is fetched, the appropriate ChunkFetchHandler's didFetchChunk() will be invoked with the data. Should a chunk fail to download, its
	 * handler's failedToFetchChunk() will be invoked (but the list will continue to be processed)
	 * @param chunkIDs a List of ChunkIDs to be transferred. The items in this list will be used to determine which chunks should be downloaded
	 * @param containerID The ID of the container which these chunks belong to
	 * @param handler The Handler for notifying when each chunk succeeds/fails to transfer, and for when the whole list is done being processed
	 * @param state An opaque object that will be passed to the handler on fetch operation completion.  This can be used to carry state between the initiator of the fetch and the handler.
	 */
	public void fetchChunksRemotely(final List<String> chunkIDs, final long containerID, final ChunksTransferHandler handler, final Object state);

	public int getListenPort();

	void setChunkRetrievalStrategyFactory(ChunkRetrievalStrategyFactory stratFac);

	void setChunkStorage(ChunkStorage storage);

	void setDataModel(DataModel model);

	void stop();
}
