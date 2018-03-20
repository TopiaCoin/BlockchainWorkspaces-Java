package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.intf.ChunkTransferHandler;
import io.topiacoin.chunks.intf.ChunkTransferer;
import io.topiacoin.chunks.intf.ChunksTransferHandler;
import io.topiacoin.core.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SecrataEnterpriseChunkTransferer implements ChunkTransferer {

	private Executor chunkFetchPool;
	private ChunkTransferRunnableFactory runnableFactory;
	private String _location;
	private Configuration configuration;

	public SecrataEnterpriseChunkTransferer(ChunkTransferRunnableFactory factory) {
		chunkFetchPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override public Thread newThread(Runnable r) {
				return new Thread(r, "chunk-fetch");
			}
		});
		runnableFactory = factory;
		String host = configuration.getConfigurationOption("host.name");
		if(host == null) {
			throw new RuntimeException("Cannot init SecrataEnterpriseChunkTransfer - host.name not found in Configuration");
		}
		setLocation(host);
	}

	/**
	 *
	 * @param chunkIDs a List of ChunkIDs to be transferred. The items in this list will be used to determine which chunks should be downloaded
	 * @param containerID The ID of the container which these chunks belong to
	 * @param handler The Handler for notifying when each chunk succeeds/fails to transfer, and for when the whole list is done being processed
	 * @param state An opaque object that will be passed to the handler on fetch operation completion.  This can be used to carry state between the initiator of the fetch and the handler.
	 */
	@Override public void fetchChunksRemotely(List<String> chunkIDs, String containerID, final ChunksTransferHandler handler, final Object state) {
		final List<String> chunksToTransfer = new ArrayList<String>(chunkIDs);
		ChunkTransferHandler transferHandler = new ChunkTransferHandler() {
			@Override public void didFetchChunk(String chunkID, byte[] chunkdata) {
				handler.didFetchChunk(chunkID, chunkdata, state);
				synchronized (chunksToTransfer) {
					chunksToTransfer.remove(chunkID);
					if(chunksToTransfer.isEmpty()) {
						handler.fetchedAllChunks(state);
					}
				}
			}

			@Override public void failedToFetchChunk(String chunkID, String message, Exception cause) {
				handler.failedToFetchChunk(chunkID, message, cause, state);
				synchronized (chunksToTransfer) {
					chunksToTransfer.remove(chunkID);
					if(chunksToTransfer.isEmpty()) {
						handler.fetchedAllChunks(state);
					}
				}
			}
		};
		for(String chunkID : chunkIDs) {
			chunkFetchPool.execute(runnableFactory.getTransferRunnable("TCP", transferHandler, _location, chunkID));
		}
	}

	private void setLocation(String hostname) {
		_location = "https://" + hostname + "/fileChunks";
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
}
