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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractChunkTransfererTest {

	public abstract ChunkTransferer getChunkTransferer(MemberNode myMemberNode, KeyPair chunkTransferPair) throws IOException, FailedToStartCommsListenerException;

	@Test
	public void testTransferOneChunkSuccessfully() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADBEEF".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new InMemoryChunkStorage();
			for (String chunkID : testChunks.keySet()) {
				transfererBChunkStorage.addChunk(chunkID, new ByteArrayInputStream(testChunks.get(chunkID)), null, true);
			}

			final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final CountDownLatch lock = new CountDownLatch(testChunks.keySet().size() + 1);
			String testContainerId = "containerA";
			int userAPort = 7777;
			int userBPort = 7778;
			String userAAuthToken = "potawto";
			String userBAuthToken = "potatoe";
			MemberNode userAMemberNode = new MemberNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded(), userAAuthToken);
			MemberNode userBMemberNode = new MemberNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
			List<MemberNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com");
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
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
			Set<String> chunksExpected = new HashSet<>(testChunks.keySet());
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					assertTrue(chunksExpected.remove(chunkID));
					lock.countDown();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunks(Object state) {
					for (String chunkID : testChunks.keySet()) {
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

				@Override public void failedToBuildFetchPlan() {
					fail();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
		} finally {
			if (transfererA != null) {
				transfererA.stop();
			}
			if (transfererB != null) {
				transfererB.stop();
			}
		}
	}

	@Test
	public void testTransferMultipleChunksSuccessfully() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADFOO".getBytes());
		testChunks.put("bar", "DEADBAR".getBytes());
		testChunks.put("baz", "DEADBAZ".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new InMemoryChunkStorage();
			for (String chunkID : testChunks.keySet()) {
				transfererBChunkStorage.addChunk(chunkID, new ByteArrayInputStream(testChunks.get(chunkID)), null, true);
			}

			final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final CountDownLatch lock = new CountDownLatch(testChunks.keySet().size() + 1);
			String testContainerId = "containerA";
			int userAPort = 7777;
			int userBPort = 7778;
			String userAAuthToken = "potawto";
			String userBAuthToken = "potatoe";
			MemberNode userAMemberNode = new MemberNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded(), userAAuthToken);
			MemberNode userBMemberNode = new MemberNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
			List<MemberNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com");
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
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
					assertTrue(testChunks.containsKey(chunkID));
					lock.countDown();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunks(Object state) {
					for (String chunkID : testChunks.keySet()) {
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

				@Override public void failedToBuildFetchPlan() {
					fail();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
		} finally {
			if (transfererA != null) {
				transfererA.stop();
			}
			if (transfererB != null) {
				transfererB.stop();
			}
		}
	}

	@Test
	public void testTransferMultipleChunksFromMultipleLocationsSuccessfully() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADFOO".getBytes());
		testChunks.put("bar", "DEADBAR".getBytes());
		testChunks.put("baz", "DEADBAZ".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		ChunkTransferer transfererC = null;
		ChunkTransferer transfererD = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererCChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererDChunkStorage = new InMemoryChunkStorage();
			transfererBChunkStorage.addChunk("foo", new ByteArrayInputStream(testChunks.get("foo")), null, true);
			transfererCChunkStorage.addChunk("bar", new ByteArrayInputStream(testChunks.get("bar")), null, true);
			transfererDChunkStorage.addChunk("baz", new ByteArrayInputStream(testChunks.get("baz")), null, true);

			final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userCChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userDChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final CountDownLatch lock = new CountDownLatch(testChunks.keySet().size() + 1);
			String testContainerId = "containerA";
			int userAPort = 7777;
			int userBPort = 7778;
			int userCPort = 7779;
			int userDPort = 7780;
			String userAAuthToken = "potawto";
			String userBAuthToken = "potatoe";
			String userCAuthToken = "tomatoe";
			String userDAuthToken = "tomawto";
			MemberNode userAMemberNode = new MemberNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded(), userAAuthToken);
			MemberNode userBMemberNode = new MemberNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
			MemberNode userCMemberNode = new MemberNode("userC", "127.0.0.1", userCPort, userCChunkTransferKeyPair.getPublic().getEncoded(), userCAuthToken);
			MemberNode userDMemberNode = new MemberNode("userD", "127.0.0.1", userDPort, userDChunkTransferKeyPair.getPublic().getEncoded(), userDAuthToken);
			List<MemberNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			containerAMemberNodes.add(userCMemberNode);
			containerAMemberNodes.add(userDMemberNode);
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com");
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com");
			CurrentUser currentUserC = new CurrentUser("userC", "userC@email.com");
			CurrentUser currentUserD = new CurrentUser("userD", "userD@email.com");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererC = getChunkTransferer(userCMemberNode, userCChunkTransferKeyPair);
			transfererD = getChunkTransferer(userDMemberNode, userDChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererC.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererD.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);
			transfererC.setChunkStorage(transfererCChunkStorage);
			transfererD.setChunkStorage(transfererDChunkStorage);

			DataModel mockModelA = EasyMock.mock(DataModel.class);
			transfererA.setDataModel(mockModelA);
			EasyMock.expect(mockModelA.getMemberNodesForContainer("containerA")).andReturn(containerAMemberNodes);
			EasyMock.expect(mockModelA.getCurrentUser()).andReturn(currentUserA).anyTimes();
			EasyMock.replay(mockModelA);
			DataModel mockModelB = EasyMock.mock(DataModel.class);
			EasyMock.expect(mockModelB.getCurrentUser()).andReturn(currentUserB).anyTimes();
			transfererB.setDataModel(mockModelB);
			EasyMock.replay(mockModelB);
			DataModel mockModelC = EasyMock.mock(DataModel.class);
			EasyMock.expect(mockModelC.getCurrentUser()).andReturn(currentUserC).anyTimes();
			transfererC.setDataModel(mockModelC);
			EasyMock.replay(mockModelC);
			DataModel mockModelD = EasyMock.mock(DataModel.class);
			EasyMock.expect(mockModelD.getCurrentUser()).andReturn(currentUserD).anyTimes();
			transfererD.setDataModel(mockModelD);
			EasyMock.replay(mockModelD);
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					assertTrue(testChunks.containsKey(chunkID));
					lock.countDown();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunks(Object state) {
					for (String chunkID : testChunks.keySet()) {
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

				@Override public void failedToBuildFetchPlan() {
					fail();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
		} finally {
			if (transfererA != null) {
				transfererA.stop();
			}
			if (transfererB != null) {
				transfererB.stop();
			}
			if (transfererC != null) {
				transfererC.stop();
			}
			if (transfererD != null) {
				transfererD.stop();
			}
		}
	}

	@Test
	public void testCantFetchAllChunks() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADFOO".getBytes());
		testChunks.put("bar", "DEADBAR".getBytes());
		testChunks.put("baz", "DEADBAZ".getBytes());
		testChunks.put("blah", "DEADBLAH".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		ChunkTransferer transfererC = null;
		ChunkTransferer transfererD = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererCChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererDChunkStorage = new InMemoryChunkStorage();
			transfererBChunkStorage.addChunk("foo", new ByteArrayInputStream(testChunks.get("foo")), null, true);
			transfererCChunkStorage.addChunk("bar", new ByteArrayInputStream(testChunks.get("bar")), null, true);
			transfererDChunkStorage.addChunk("baz", new ByteArrayInputStream(testChunks.get("baz")), null, true);

			final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userCChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userDChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final CountDownLatch lock = new CountDownLatch(1);
			String testContainerId = "containerA";
			int userAPort = 7777;
			int userBPort = 7778;
			int userCPort = 7779;
			int userDPort = 7780;
			String userAAuthToken = "potawto";
			String userBAuthToken = "potatoe";
			String userCAuthToken = "tomatoe";
			String userDAuthToken = "tomawto";
			MemberNode userAMemberNode = new MemberNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded(), userAAuthToken);
			MemberNode userBMemberNode = new MemberNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
			MemberNode userCMemberNode = new MemberNode("userC", "127.0.0.1", userCPort, userCChunkTransferKeyPair.getPublic().getEncoded(), userCAuthToken);
			MemberNode userDMemberNode = new MemberNode("userD", "127.0.0.1", userDPort, userDChunkTransferKeyPair.getPublic().getEncoded(), userDAuthToken);
			List<MemberNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			containerAMemberNodes.add(userCMemberNode);
			containerAMemberNodes.add(userDMemberNode);
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com");
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com");
			CurrentUser currentUserC = new CurrentUser("userC", "userC@email.com");
			CurrentUser currentUserD = new CurrentUser("userD", "userD@email.com");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererC = getChunkTransferer(userCMemberNode, userCChunkTransferKeyPair);
			transfererD = getChunkTransferer(userDMemberNode, userDChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererC.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererD.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);
			transfererC.setChunkStorage(transfererCChunkStorage);
			transfererD.setChunkStorage(transfererDChunkStorage);

			DataModel mockModelA = EasyMock.mock(DataModel.class);
			transfererA.setDataModel(mockModelA);
			EasyMock.expect(mockModelA.getMemberNodesForContainer("containerA")).andReturn(containerAMemberNodes);
			EasyMock.expect(mockModelA.getCurrentUser()).andReturn(currentUserA).anyTimes();
			EasyMock.replay(mockModelA);
			DataModel mockModelB = EasyMock.mock(DataModel.class);
			EasyMock.expect(mockModelB.getCurrentUser()).andReturn(currentUserB).anyTimes();
			transfererB.setDataModel(mockModelB);
			EasyMock.replay(mockModelB);
			DataModel mockModelC = EasyMock.mock(DataModel.class);
			EasyMock.expect(mockModelC.getCurrentUser()).andReturn(currentUserC).anyTimes();
			transfererC.setDataModel(mockModelC);
			EasyMock.replay(mockModelC);
			DataModel mockModelD = EasyMock.mock(DataModel.class);
			EasyMock.expect(mockModelD.getCurrentUser()).andReturn(currentUserD).anyTimes();
			transfererD.setDataModel(mockModelD);
			EasyMock.replay(mockModelD);
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					fail();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunks(Object state) {
					fail();
				}

				@Override public void failedToBuildFetchPlan() {
					lock.countDown();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			assertTrue(lock.await(10, TimeUnit.SECONDS));
		} finally {
			if (transfererA != null) {
				transfererA.stop();
			}
			if (transfererB != null) {
				transfererB.stop();
			}
			if (transfererC != null) {
				transfererC.stop();
			}
			if (transfererD != null) {
				transfererD.stop();
			}
		}
	}

	@Test
	public void testTransferChunkBadAuthKey() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADBEEF".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new InMemoryChunkStorage();
			for (String chunkID : testChunks.keySet()) {
				transfererBChunkStorage.addChunk(chunkID, new ByteArrayInputStream(testChunks.get(chunkID)), null, true);
			}

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
			MemberNode userBMemberNodeWBadAuthKey = new MemberNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken + "wrong");
			List<MemberNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNodeWBadAuthKey);
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com");
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
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
					fail();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunks(Object state) {
					fail();
				}

				@Override public void failedToBuildFetchPlan() {
					lock.countDown();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
		} finally {
			if (transfererA != null) {
				transfererA.stop();
			}
			if (transfererB != null) {
				transfererB.stop();
			}
		}
	}

	@Test
	public void testFetchChunksRemotelyNegative() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADBEEF".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new InMemoryChunkStorage();
			for (String chunkID : testChunks.keySet()) {
				transfererBChunkStorage.addChunk(chunkID, new ByteArrayInputStream(testChunks.get(chunkID)), null, true);
			}

			final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final CountDownLatch lock = new CountDownLatch(testChunks.keySet().size() + 1);
			String testContainerId = "containerA";
			int userAPort = 7777;
			int userBPort = 7778;
			String userAAuthToken = "potawto";
			String userBAuthToken = "potatoe";
			MemberNode userAMemberNode = new MemberNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded(), userAAuthToken);
			MemberNode userBMemberNode = new MemberNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
			List<MemberNode> containerAMemberNodes = new ArrayList<>();
			//containerAMemberNodes.add(userAMemberNode);
			//containerAMemberNodes.add(userBMemberNode);
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com");
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
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
			Set<String> chunksExpected = new HashSet<>(testChunks.keySet());
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					assertTrue(chunksExpected.remove(chunkID));
					lock.countDown();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunks(Object state) {
					for (String chunkID : testChunks.keySet()) {
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

				@Override public void failedToBuildFetchPlan() {
					fail();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			fail();
		} catch (IllegalStateException ex) {
			//good
		} finally {
			if (transfererA != null) {
				transfererA.stop();
			}
			if (transfererB != null) {
				transfererB.stop();
			}
		}
	}
}
