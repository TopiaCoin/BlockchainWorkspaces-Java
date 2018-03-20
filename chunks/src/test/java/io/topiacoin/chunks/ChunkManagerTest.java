package io.topiacoin.chunks;

import io.topiacoin.chunks.intf.ChunkTransferHandler;
import io.topiacoin.chunks.intf.ChunkTransferer;
import io.topiacoin.chunks.intf.ChunksFetchHandler;
import io.topiacoin.chunks.intf.ChunksTransferHandler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ChunkManagerTest {

	@Test
	public void fetchChunksSuccessfully() throws Exception {
		ChunkManager manager = new ChunkManager();
		final List<String> testChunkIDs = new ArrayList<String>(Arrays.asList("foo", "bar", "baz"));
		final String testContainerID = "potato";
		final String testState = "its a test";
		manager.chunkTransferer = new ChunkTransferer() {
			@Override public void fetchChunksRemotely(List<String> chunkIDs, String containerID, final ChunksTransferHandler handler, final Object state) {
				final List<String> chunksToTransfer = new ArrayList<String>(chunkIDs);
				final ChunkTransferHandler transferHandler = new ChunkTransferHandler() {
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
				for(final String chunkID : chunkIDs) {
					new Thread(new Runnable() {
						@Override public void run() {
							try {
								Thread.sleep(100 + new Random().nextInt(400));
							} catch (InterruptedException e) {
								//NOP
							}
							transferHandler.didFetchChunk(chunkID, new byte[10]);
						}
					}).start();
				}
			}
		};
		final CountDownLatch lock = new CountDownLatch(1);
		manager.fetchChunks(testChunkIDs, testContainerID, new ChunksFetchHandler() {
			@Override public void finishedFetchingChunks(List<String> successfulChunks, List<String> unsuccessfulChunks, Object state) {
				assertTrue("unsuccessfulChunks not empty", unsuccessfulChunks.isEmpty());
				assertEquals("State wrong", testState, state);
				assertTrue("Chunk lists do not match", successfulChunks.containsAll(testChunkIDs));
				lock.countDown();
			}

			@Override public void errorFetchingChunks(String message, Exception cause, Object state) {
				fail("Didn't expect any errors, got " + message + "\n" + cause.getLocalizedMessage());
			}
		}, testState);
		assertTrue("Success handler never called.", lock.await(100 + (500 * testChunkIDs.size()), TimeUnit.MILLISECONDS));
	}

	@Test
	public void fetchChunksFailed() throws Exception {
		ChunkManager manager = new ChunkManager();
		final List<String> testChunkIDs = new ArrayList<String>(Arrays.asList("foo", "bar", "baz"));
		final String testContainerID = "potato";
		final String testState = "its a test";
		manager.chunkTransferer = new ChunkTransferer() {
			@Override public void fetchChunksRemotely(List<String> chunkIDs, String containerID, final ChunksTransferHandler handler, final Object state) {
				final List<String> chunksToTransfer = new ArrayList<String>(chunkIDs);
				final ChunkTransferHandler transferHandler = new ChunkTransferHandler() {
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
				for(final String chunkID : chunkIDs) {
					new Thread(new Runnable() {
						@Override public void run() {
							try {
								Thread.sleep(100 + new Random().nextInt(400));
							} catch (InterruptedException e) {
								//NOP
							}
							transferHandler.failedToFetchChunk(chunkID, "Test failure", null);
						}
					}).start();
				}
			}
		};
		final CountDownLatch lock = new CountDownLatch(1);
		manager.fetchChunks(testChunkIDs, testContainerID, new ChunksFetchHandler() {
			@Override public void finishedFetchingChunks(List<String> successfulChunks, List<String> unsuccessfulChunks, Object state) {
				assertTrue("successfulChunks not empty", successfulChunks.isEmpty());
				assertEquals("State wrong", testState, state);
				assertTrue("Chunk lists do not match", unsuccessfulChunks.containsAll(testChunkIDs));
				lock.countDown();
			}

			@Override public void errorFetchingChunks(String message, Exception cause, Object state) {
				fail("Didn't expect any errors, got " + message + "\n" + cause.getLocalizedMessage());
			}
		}, testState);
		assertTrue("Success handler never called.", lock.await(100 + (500 * testChunkIDs.size()), TimeUnit.MILLISECONDS));
	}
}