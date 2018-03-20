package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.intf.ChunkRetrievalStrategy;
import io.topiacoin.chunks.intf.ChunkTransferHandler;
import io.topiacoin.chunks.intf.ChunkTransferer;
import io.topiacoin.chunks.intf.ChunksTransferHandler;
import io.topiacoin.chunks.model.ChunkLocationResponse;
import io.topiacoin.chunks.model.ChunkRetrievalPlan;
import io.topiacoin.core.Configuration;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SDFSChunkTransferer implements ChunkTransferer {

	private Executor chunkFetchPool;
	private ChunkTransferRunnableFactory runnableFactory;
	private Configuration configuration;
	private ChunkRetrievalStrategy chunkRetrievalStrategy;

	public SDFSChunkTransferer(ChunkTransferRunnableFactory factory) {
		chunkFetchPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override public Thread newThread(Runnable r) {
				return new Thread(r, "chunk-fetch");
			}
		});
		runnableFactory = factory;
	}

	/**
	 *
	 * @param chunkIDs a List of ChunkIDs to be transferred. The items in this list will be used to determine which chunks should be downloaded
	 * @param containerID The ID of the container which these chunks belong to
	 * @param handler The Handler for notifying when each chunk succeeds/fails to transfer, and for when the whole list is done being processed
	 * @param state An opaque object that will be passed to the handler on fetch operation completion.  This can be used to carry state between the initiator of the fetch and the handler.
	 */
	@Override public void fetchChunksRemotely(List<String> chunkIDs, String containerID, final ChunksTransferHandler handler, final Object state) {
		String userID = null;
		//String[] memberNodes = fetchMemberNodes(containerID);
		//KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
		//KeyPair fetchPair = kpg.generateKeyPair();
		//ChunkLocationResponse[] chunkLocationResponses = determineChunkLocations(memberNodes, chunkIDs, userID, fetchPair.getPublic());
		//ChunkRetrievalPlan plan = chunkRetrievalStrategy.generateRetrievalPlan(chunkLocationResponses, fetchPair, chunkIDs);

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
			//chunkFetchPool.execute(runnableFactory.getTransferRunnable("TCP", transferHandler, _location, chunkID));
		}
	}


	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public void setChunkRetrievalStrategy(ChunkRetrievalStrategy strat) {
		this.chunkRetrievalStrategy = strat;
	}

	ChunkLocationResponse[] determineChunkLocations(String[] memberNodes, List<String> chunkIDs, String userID, Object pubKey) {
		throw new NotImplementedException();
	}
}
