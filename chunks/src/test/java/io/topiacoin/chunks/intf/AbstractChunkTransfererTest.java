package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.InMemoryChunkStorage;
import io.topiacoin.chunks.TestDataModel;
import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.InvalidReservationException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.impl.SimpleChunkRetrievalStrategyFactory;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.File;
import io.topiacoin.model.FileChunk;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.Member;
import io.topiacoin.model.UserNode;
import io.topiacoin.model.User;
import io.topiacoin.model.Workspace;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

	public abstract ChunkTransferer getChunkTransferer(UserNode myMemberNode, KeyPair chunkTransferPair) throws IOException, FailedToStartCommsListenerException;

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
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk c = new FileChunk();
			c.setChunkID("foo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", c);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			Set<String> chunksExpected = new HashSet<>(testChunks.keySet());
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					assertTrue(chunksExpected.remove(chunkID));
					lock.countDown();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
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

				@Override public void failedToFetchAllChunks(Object state) {
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
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk fooChunk = new FileChunk();
			fooChunk.setChunkID("foo");
			FileChunk barChunk = new FileChunk();
			barChunk.setChunkID("bar");
			FileChunk bazChunk = new FileChunk();
			bazChunk.setChunkID("baz");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", bazChunk);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", bazChunk);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					assertTrue(testChunks.containsKey(chunkID));
					lock.countDown();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
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

				@Override public void failedToFetchAllChunks(Object state) {
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
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userCMemberNode = new UserNode("userC", "127.0.0.1", userCPort, userCChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userDMemberNode = new UserNode("userD", "127.0.0.1", userDPort, userDChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			containerAMemberNodes.add(userCMemberNode);
			containerAMemberNodes.add(userDMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userCKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userDKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			CurrentUser currentUserC = new CurrentUser("userC", "userC@email.com", userCKeyPair.getPublic(), userCKeyPair.getPrivate());
			CurrentUser currentUserD = new CurrentUser("userD", "userD@email.com", userDKeyPair.getPublic(), userDKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			Member memberC = new Member();
			Member memberD = new Member();
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			memberC.setAuthToken(userCAuthToken);
			memberD.setAuthToken(userDAuthToken);
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			memberC.setUserID(currentUserC.getUserID());
			memberD.setUserID(currentUserD.getUserID());
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			members.add(memberC);
			members.add(memberD);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk chunkFoo = new FileChunk();
			chunkFoo.setChunkID("foo");
			FileChunk chunkBar = new FileChunk();
			chunkBar.setChunkID("bar");
			FileChunk chunkBaz = new FileChunk();
			chunkBaz.setChunkID("baz");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

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

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUser(new User(currentUserC));
			modelA.addUser(new User(currentUserD));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.addUserNode(userCMemberNode);
			modelA.addUserNode(userDMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addMemberToWorkspace("containerA", memberC);
			modelA.addMemberToWorkspace("containerA", memberD);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", chunkFoo);
			modelA.addChunkForFile("FileFoo", "VersionFoo", chunkBar);
			modelA.addChunkForFile("FileFoo", "VersionFoo", chunkBaz);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUser(new User(currentUserC));
			modelB.addUser(new User(currentUserD));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.addUserNode(userCMemberNode);
			modelB.addUserNode(userDMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addMemberToWorkspace("containerA", memberC);
			modelB.addMemberToWorkspace("containerA", memberD);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", chunkFoo);
			modelB.addChunkForFile("FileFoo", "VersionFoo", chunkBar);
			modelB.addChunkForFile("FileFoo", "VersionFoo", chunkBaz);

			DataModel modelC = new TestDataModel();
			modelC.addUser(new User(currentUserA));
			modelC.addUser(new User(currentUserB));
			modelC.addUser(new User(currentUserC));
			modelC.addUser(new User(currentUserD));
			modelC.addUserNode(userAMemberNode);
			modelC.addUserNode(userBMemberNode);
			modelC.addUserNode(userCMemberNode);
			modelC.addUserNode(userDMemberNode);
			modelC.setCurrentUser(currentUserC);
			modelC.addWorkspace(workspace);
			modelC.addMemberToWorkspace("containerA", memberA);
			modelC.addMemberToWorkspace("containerA", memberB);
			modelC.addMemberToWorkspace("containerA", memberC);
			modelC.addMemberToWorkspace("containerA", memberD);
			modelC.addFileToWorkspace("containerA", f);
			modelC.addFileVersion("FileFoo", v);
			modelC.addChunkForFile("FileFoo", "VersionFoo", chunkFoo);
			modelC.addChunkForFile("FileFoo", "VersionFoo", chunkBar);
			modelC.addChunkForFile("FileFoo", "VersionFoo", chunkBaz);

			DataModel modelD = new TestDataModel();
			modelD.addUser(new User(currentUserA));
			modelD.addUser(new User(currentUserB));
			modelD.addUser(new User(currentUserC));
			modelD.addUser(new User(currentUserD));
			modelD.addUserNode(userAMemberNode);
			modelD.addUserNode(userBMemberNode);
			modelD.addUserNode(userCMemberNode);
			modelD.addUserNode(userDMemberNode);
			modelD.setCurrentUser(currentUserD);
			modelD.addWorkspace(workspace);
			modelD.addMemberToWorkspace("containerA", memberA);
			modelD.addMemberToWorkspace("containerA", memberB);
			modelD.addMemberToWorkspace("containerA", memberC);
			modelD.addMemberToWorkspace("containerA", memberD);
			modelD.addFileToWorkspace("containerA", f);
			modelD.addFileVersion("FileFoo", v);
			modelD.addChunkForFile("FileFoo", "VersionFoo", chunkFoo);
			modelD.addChunkForFile("FileFoo", "VersionFoo", chunkBar);
			modelD.addChunkForFile("FileFoo", "VersionFoo", chunkBaz);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			transfererC.setDataModel(modelC);
			transfererD.setDataModel(modelD);
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					assertTrue(testChunks.containsKey(chunkID));
					lock.countDown();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
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

				@Override public void failedToFetchAllChunks(Object state) {
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
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userCMemberNode = new UserNode("userC", "127.0.0.1", userCPort, userCChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userDMemberNode = new UserNode("userD", "127.0.0.1", userDPort, userDChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			containerAMemberNodes.add(userCMemberNode);
			containerAMemberNodes.add(userDMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userCKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userDKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			CurrentUser currentUserC = new CurrentUser("userC", "userC@email.com", userCKeyPair.getPublic(), userCKeyPair.getPrivate());
			CurrentUser currentUserD = new CurrentUser("userD", "userD@email.com", userDKeyPair.getPublic(), userDKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			Member memberC = new Member();
			Member memberD = new Member();
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			memberC.setAuthToken(userCAuthToken);
			memberD.setAuthToken(userDAuthToken);
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			memberC.setUserID(currentUserC.getUserID());
			memberD.setUserID(currentUserD.getUserID());
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			members.add(memberC);
			members.add(memberD);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk c = new FileChunk();
			c.setChunkID("foo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

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

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUser(new User(currentUserC));
			modelA.addUser(new User(currentUserD));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.addUserNode(userCMemberNode);
			modelA.addUserNode(userDMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addMemberToWorkspace("containerA", memberC);
			modelA.addMemberToWorkspace("containerA", memberD);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUser(new User(currentUserC));
			modelB.addUser(new User(currentUserD));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.addUserNode(userCMemberNode);
			modelB.addUserNode(userDMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addMemberToWorkspace("containerA", memberC);
			modelB.addMemberToWorkspace("containerA", memberD);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelC = new TestDataModel();
			modelC.addUser(new User(currentUserA));
			modelC.addUser(new User(currentUserB));
			modelC.addUser(new User(currentUserC));
			modelC.addUser(new User(currentUserD));
			modelC.addUserNode(userAMemberNode);
			modelC.addUserNode(userBMemberNode);
			modelC.addUserNode(userCMemberNode);
			modelC.addUserNode(userDMemberNode);
			modelC.setCurrentUser(currentUserC);
			modelC.addWorkspace(workspace);
			modelC.addMemberToWorkspace("containerA", memberA);
			modelC.addMemberToWorkspace("containerA", memberB);
			modelC.addMemberToWorkspace("containerA", memberC);
			modelC.addMemberToWorkspace("containerA", memberD);
			modelC.addFileToWorkspace("containerA", f);
			modelC.addFileVersion("FileFoo", v);
			modelC.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelD = new TestDataModel();
			modelD.addUser(new User(currentUserA));
			modelD.addUser(new User(currentUserB));
			modelD.addUser(new User(currentUserC));
			modelD.addUser(new User(currentUserD));
			modelD.addUserNode(userAMemberNode);
			modelD.addUserNode(userBMemberNode);
			modelD.addUserNode(userCMemberNode);
			modelD.addUserNode(userDMemberNode);
			modelD.setCurrentUser(currentUserD);
			modelD.addWorkspace(workspace);
			modelD.addMemberToWorkspace("containerA", memberA);
			modelD.addMemberToWorkspace("containerA", memberB);
			modelD.addMemberToWorkspace("containerA", memberC);
			modelD.addMemberToWorkspace("containerA", memberD);
			modelD.addFileToWorkspace("containerA", f);
			modelD.addFileVersion("FileFoo", v);
			modelD.addChunkForFile("FileFoo", "VersionFoo", c);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			transfererC.setDataModel(modelC);
			transfererD.setDataModel(modelD);
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					fail();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
					fail();
				}

				@Override public void failedToBuildFetchPlan() {
					lock.countDown();
				}

				@Override public void failedToFetchAllChunks(Object state) {
					fail();
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
			String userBWrongAuthToken = "tomato";
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			Member memberBWrong = new Member();
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			memberBWrong.setAuthToken(userBWrongAuthToken);
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			memberBWrong.setUserID(currentUserB.getUserID());
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk c = new FileChunk();
			c.setChunkID("foo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberBWrong);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", c);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					fail();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
					fail();
				}

				@Override public void failedToBuildFetchPlan() {
					lock.countDown();
				}

				@Override public void failedToFetchAllChunks(Object state) {
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
	public void testFetchNonExistantChunk() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADBEEF".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new ChunkStorage() {
				@Override public void addChunk(String chunkID, InputStream chunkStream, ReservationID reservationID, boolean purgeable) throws DuplicateChunkException, InvalidReservationException, InsufficientSpaceException, IOException {
					//lol no
				}

				@Override public InputStream getChunkDataStream(String chunkID) throws NoSuchChunkException {
					throw new NoSuchChunkException("This is by design for the unit test");
				}

				@Override public byte[] getChunkData(String chunkID) throws NoSuchChunkException, IOException {
					throw new NoSuchChunkException("This is by design for the unit test");
				}

				@Override public boolean hasChunk(String chunkID) {
					return true;
				}

				@Override public boolean removeChunk(String chunkID) {
					return true;
				}

				@Override public long getStorageQuota() {
					return 999999999;
				}

				@Override public long getAvailableStorage() {
					return 999999999;
				}

				@Override public boolean purgeStorage(long neededAvailableSpace) {
					return false;
				}

				@Override public ReservationID reserveStorageSpace(long spaceToReserve) throws InsufficientSpaceException {
					return null;
				}

				@Override public void releaseSpaceReservation(ReservationID reservationID) throws InvalidReservationException {

				}
			};
			for (String chunkID : testChunks.keySet()) {
				transfererBChunkStorage.addChunk(chunkID, new ByteArrayInputStream(testChunks.get(chunkID)), null, true);
			}

			final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final CountDownLatch lock = new CountDownLatch(2);
			String testContainerId = "containerA";
			int userAPort = 7777;
			int userBPort = 7778;
			String userAAuthToken = "potawto";
			String userBAuthToken = "potatoe";
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk c = new FileChunk();
			c.setChunkID("foo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", c);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			Set<String> chunksExpected = new HashSet<>(testChunks.keySet());
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					assertTrue(chunksExpected.remove(chunkID));
					lock.countDown();
				}

				@Override public void failedToFetchChunk(String failedChunkID, String message, Exception cause, Object state) {
					assertTrue(testChunks.containsKey(failedChunkID));
					lock.countDown();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
					fail();
				}

				@Override public void failedToFetchAllChunks(Object state) {
					lock.countDown();
				}

				@Override public void failedToBuildFetchPlan() {
					fail();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			assertTrue("Chunk fetch never finished in its failed state", lock.await(10, TimeUnit.SECONDS));
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
	public void testFetchNonExistantChunkPt2() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADBEEF".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new ChunkStorage() {
				@Override public void addChunk(String chunkID, InputStream chunkStream, ReservationID reservationID, boolean purgeable) throws DuplicateChunkException, InvalidReservationException, InsufficientSpaceException, IOException {
					//lol no
				}

				@Override public InputStream getChunkDataStream(String chunkID) throws NoSuchChunkException {
					throw new NoSuchChunkException("haha");
				}

				@Override public byte[] getChunkData(String chunkID) throws NoSuchChunkException, IOException {
					throw new NoSuchChunkException("wut?");
				}

				@Override public boolean hasChunk(String chunkID) {
					return false;
				}

				@Override public boolean removeChunk(String chunkID) {
					return true;
				}

				@Override public long getStorageQuota() {
					return 999999999;
				}

				@Override public long getAvailableStorage() {
					return 999999999;
				}

				@Override public boolean purgeStorage(long neededAvailableSpace) {
					return false;
				}

				@Override public ReservationID reserveStorageSpace(long spaceToReserve) throws InsufficientSpaceException {
					return null;
				}

				@Override public void releaseSpaceReservation(ReservationID reservationID) throws InvalidReservationException {

				}
			};
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
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk c = new FileChunk();
			c.setChunkID("foo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", c);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					fail();
				}

				@Override public void failedToFetchChunk(String failedChunkID, String message, Exception cause, Object state) {
					fail();

				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
					fail();
				}

				@Override public void failedToBuildFetchPlan() {
					lock.countDown();
				}

				@Override public void failedToFetchAllChunks(Object state) {
					fail();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			assertTrue("Chunk fetch never finished in its failed state", lock.await(10, TimeUnit.SECONDS));
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
	public void testFetchNonExistantChunkPt3() throws Exception {
		Map<String, byte[]> testChunks = new HashMap<>();
		testChunks.put("foo", "DEADBEEF".getBytes());
		ChunkTransferer transfererA = null;
		ChunkTransferer transfererB = null;
		try {
			ChunkStorage transfererAChunkStorage = new InMemoryChunkStorage();
			ChunkStorage transfererBChunkStorage = new ChunkStorage() {
				Set<String> chunksImKeenOnLyingAbout = new HashSet<String>();
				@Override public void addChunk(String chunkID, InputStream chunkStream, ReservationID reservationID, boolean purgeable) throws DuplicateChunkException, InvalidReservationException, InsufficientSpaceException, IOException {
					//lol no
				}

				@Override public InputStream getChunkDataStream(String chunkID) throws NoSuchChunkException {
					throw new NoSuchChunkException("haha");
				}

				@Override public byte[] getChunkData(String chunkID) throws NoSuchChunkException, IOException {
					throw new NoSuchChunkException("wut?");
				}

				@Override public boolean hasChunk(String chunkID) {
					 return chunksImKeenOnLyingAbout.add(chunkID);
				}

				@Override public boolean removeChunk(String chunkID) {
					return true;
				}

				@Override public long getStorageQuota() {
					return 999999999;
				}

				@Override public long getAvailableStorage() {
					return 999999999;
				}

				@Override public boolean purgeStorage(long neededAvailableSpace) {
					return false;
				}

				@Override public ReservationID reserveStorageSpace(long spaceToReserve) throws InsufficientSpaceException {
					return null;
				}

				@Override public void releaseSpaceReservation(ReservationID reservationID) throws InvalidReservationException {

				}
			};
			for (String chunkID : testChunks.keySet()) {
				transfererBChunkStorage.addChunk(chunkID, new ByteArrayInputStream(testChunks.get(chunkID)), null, true);
			}

			final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			final CountDownLatch lock = new CountDownLatch(2);
			String testContainerId = "containerA";
			int userAPort = 7777;
			int userBPort = 7778;
			String userAAuthToken = "potawto";
			String userBAuthToken = "potatoe";
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk c = new FileChunk();
			c.setChunkID("foo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", c);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			Set<String> chunksExpected = new HashSet<>(testChunks.keySet());
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					fail();
				}

				@Override public void failedToFetchChunk(String failedChunkID, String message, Exception cause, Object state) {
					lock.countDown();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
					fail();
				}

				@Override public void failedToBuildFetchPlan() {
					fail();
				}

				@Override public void failedToFetchAllChunks(Object state) {
					lock.countDown();
				}
			};
			List<String> testChunkIDs = new ArrayList<>(testChunks.keySet());
			transfererA.fetchChunksRemotely(testChunkIDs, testContainerId, handler, null);
			assertTrue("Chunk fetch never finished in its failed state", lock.await(10, TimeUnit.SECONDS));
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
	public void testAttemptRequestWithBadSignature() throws Exception {
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
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair jackedUpKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser jackedUpUserA = new CurrentUser("userA", "userA@email.com", jackedUpKeyPair.getPublic(), jackedUpKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			List<Member> members = new ArrayList<Member>();
			members.add(memberA);
			members.add(memberB);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk c = new FileChunk();
			c.setChunkID("foo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(jackedUpUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.setCurrentUser(jackedUpUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", c);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			Set<String> chunksExpected = new HashSet<>(testChunks.keySet());
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					fail();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
					fail();
				}

				@Override public void failedToBuildFetchPlan() {
					lock.countDown();
				}

				@Override public void failedToFetchAllChunks(Object state) {
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
	public void testAttemptRequestFromNonCohort() throws Exception {
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
			UserNode userAMemberNode = new UserNode("userA", "127.0.0.1", userAPort, userAChunkTransferKeyPair.getPublic().getEncoded());
			UserNode userBMemberNode = new UserNode("userB", "127.0.0.1", userBPort, userBChunkTransferKeyPair.getPublic().getEncoded());
			List<UserNode> containerAMemberNodes = new ArrayList<>();
			containerAMemberNodes.add(userAMemberNode);
			containerAMemberNodes.add(userBMemberNode);
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			KeyPair userBKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			CurrentUser currentUserB = new CurrentUser("userB", "userB@email.com", userBKeyPair.getPublic(), userBKeyPair.getPrivate());
			Member memberA = new Member();
			Member memberB = new Member();
			memberA.setAuthToken(userAAuthToken);
			memberB.setAuthToken(userBAuthToken);
			memberA.setUserID(currentUserA.getUserID());
			memberB.setUserID(currentUserB.getUserID());
			List<Member> members = new ArrayList<Member>();
			//members.add(memberA);
			members.add(memberB);
			Workspace workspace = new Workspace();
			workspace.setMembers(members);
			List<Workspace> workspaces = new ArrayList<Workspace>();
			workspace.setGuid("containerA");
			workspaces.add(workspace);
			FileChunk c = new FileChunk();
			c.setChunkID("foo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			File f = new File();
			f.setEntryID("FileFoo");

			transfererA = getChunkTransferer(userAMemberNode, userAChunkTransferKeyPair);
			transfererB = getChunkTransferer(userBMemberNode, userBChunkTransferKeyPair);
			transfererA.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererB.setChunkRetrievalStrategyFactory(new SimpleChunkRetrievalStrategyFactory());
			transfererA.setChunkStorage(transfererAChunkStorage);
			transfererB.setChunkStorage(transfererBChunkStorage);

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.addUserNode(userAMemberNode);
			modelA.addUserNode(userBMemberNode);
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace("containerA", memberA);
			modelA.addMemberToWorkspace("containerA", memberB);
			modelA.addFileToWorkspace("containerA", f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", c);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.addUserNode(userAMemberNode);
			modelB.addUserNode(userBMemberNode);
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			//modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", c);

			transfererA.setDataModel(modelA);
			transfererB.setDataModel(modelB);
			Set<String> chunksExpected = new HashSet<>(testChunks.keySet());
			ChunksTransferHandler handler = new ChunksTransferHandler() {
				@Override public void didFetchChunk(String chunkID, Object state) {
					fail();
				}

				@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
					fail();
				}

				@Override public void fetchedAllChunksSuccessfully(Object state) {
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
					lock.countDown();
				}

				@Override public void failedToFetchAllChunks(Object state) {
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
}
