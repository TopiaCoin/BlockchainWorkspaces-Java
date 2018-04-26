package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.InMemoryChunkStorage;
import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.impl.SimpleChunkRetrievalStrategyFactory;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.MemberNode;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractChunkTransfererTest {

	public abstract ChunkTransferer getChunkTransferer(int listenPort, KeyPair chunkTransferPair) throws IOException, FailedToStartCommsListenerException;

	@Test
	public void happyPath() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		byte[] fooBytes = "DEADBEEF".getBytes();
		testChunks.put("foo", fooBytes);
		ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
		ChunkStorage transfererBChunkStorage = new InMemoryChunkStorage();
		transfererBChunkStorage.addChunk("foo", new ByteArrayInputStream(fooBytes), null, true);

		final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final CountDownLatch lock = new CountDownLatch(1);
		String testContainerId = "containerA";
		int userAPort = 7777;
		int userBPort = 7778;
		String userAAuthToken = "potawto";
		String userBAuthToken = "potatoe";
		MemberNode userAMemberNode = new MemberNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded(), userAAuthToken);
		MemberNode userBMemberNode = new MemberNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
		List<MemberNode> containerAMemberNodes = new ArrayList<>();
		//containerAMemberNodes.add(userAMemberNode);
		containerAMemberNodes.add(userBMemberNode);
		CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com");
		CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com");

		ChunkTransferer transfererA = getChunkTransferer(userAPort, userAChunkTransferKeyPair);
		ChunkTransferer transfererB = getChunkTransferer(userBPort, userBChunkTransferKeyPair);
		transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
		transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
		transfererA.setChunkStorage(transfererAChunkStorage);
		transfererB.setChunkStorage(transfererBChunkStorage);

		DataModel mockModelA = EasyMock.mock(DataModel.class);
		transfererA.setDataModel(mockModelA);
		EasyMock.expect(mockModelA.getMemberNodesForContainer("containerA")).andReturn(containerAMemberNodes);
		EasyMock.expect(mockModelA.getCurrentUser()).andReturn(currentUserA).anyTimes();
		EasyMock.replay(mockModelA);
		DataModel mockModelB = EasyMock.mock(DataModel.class);
		EasyMock.expect(mockModelB.getCurrentUser()).andReturn(currentUserB).anyTimes();
		transfererB.setDataModel(mockModelB);
		EasyMock.replay(mockModelB);
		ChunksTransferHandler handler = new ChunksTransferHandler() {
			@Override public void didFetchChunk(String chunkID, Object state) {

			}

			@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {

			}

			@Override public void fetchedAllChunks(Object state) {
				for(String chunkID : testChunks.keySet()) {
					assertTrue(transfererAChunkStorage.hasChunk(chunkID));
					try {
						assertTrue(Arrays.equals(testChunks.get(chunkID), transfererAChunkStorage.getChunkData(chunkID)));
						lock.countDown();
					} catch (NoSuchChunkException | IOException e) {
						e.printStackTrace();
						fail();
					}
				}
			}
		};
		List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
		transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
		assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
	}
}
