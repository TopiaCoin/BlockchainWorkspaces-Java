package io.topiacoin.chunks;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunksFetchHandler;
import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.FileChunk;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.Member;
import io.topiacoin.model.User;
import io.topiacoin.model.UserNode;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.util.Notification;
import io.topiacoin.util.NotificationCenter;
import io.topiacoin.util.NotificationHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ChunkManagerTest {

	@Test
	public void chunkStorageCRUDTest() throws IOException, CryptographicException, NoSuchUserException, FailedToStartCommsListenerException {
		Configuration config = new DefaultConfiguration();
		File chunkStorageLocA = new File("./target/chunks1");
		try {
			config.setConfigurationOption("chunkStorageLoc", chunkStorageLocA.getAbsolutePath());
			config.setConfigurationOption("chunkStorageQuota", "1000");
			DataModel model = new TestDataModel();
			KeyPair userAKeyPair = CryptoUtils.generateECKeyPair();
			CurrentUser currentUserA = new CurrentUser("userA", "userA@email.com", userAKeyPair.getPublic(), userAKeyPair.getPrivate());
			model.setCurrentUser(currentUserA);
			ChunkManager manager = new ChunkManager(config, model);
			assertTrue(!manager.hasChunk("blah"));
			try {
				manager.getChunkData("blah");
				fail();
			} catch (NoSuchChunkException e) {
				//good
			}
			try {
				manager.getChunkDataAsStream("blah");
				fail();
			} catch (NoSuchChunkException e) {
				//good
			}
			String chunkID1 = "chunk1";
			byte[] chunkData1 = "chunkdata1".getBytes();

			String chunkID2 = "chunk2";
			InputStream chunkData2 = new ByteArrayInputStream("chunkdata2".getBytes());

			String chunkID3 = "chunk3";
			File chunkData3 = new File("./target/chunkCRUDchunk3.dat");
			if (chunkData3.exists()) {
				chunkData3.delete();
			}
			chunkData3.createNewFile();
			FileOutputStream fos = new FileOutputStream(chunkData3);
			fos.write("chunkdata3".getBytes());
			fos.close();

			try {
				manager.addChunk(chunkID1, chunkData1);
				manager.addChunk(chunkID2, chunkData2);
				manager.addChunk(chunkID3, chunkData3);

				assertTrue(manager.hasChunk(chunkID1));
				assertTrue(manager.hasChunk(chunkID2));
				assertTrue(manager.hasChunk(chunkID3));

				assertTrue(Arrays.equals(manager.getChunkData(chunkID1), "chunkdata1".getBytes()));
				assertTrue(Arrays.equals(manager.getChunkData(chunkID2), "chunkdata2".getBytes()));
				assertTrue(Arrays.equals(manager.getChunkData(chunkID3), "chunkdata3".getBytes()));

				InputStream is = manager.getChunkDataAsStream(chunkID1);
				assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata1".getBytes()));
				is.close();
				is = manager.getChunkDataAsStream(chunkID2);
				assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata2".getBytes()));
				is.close();
				is = manager.getChunkDataAsStream(chunkID3);
				assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata3".getBytes()));
				is.close();

				manager.removeChunk(chunkID1);
				manager.removeChunk(chunkID2);
				manager.removeChunk(chunkID3);

				assertTrue(!manager.hasChunk(chunkID1));
				assertTrue(!manager.hasChunk(chunkID2));
				assertTrue(!manager.hasChunk(chunkID3));
			} catch (InsufficientSpaceException e) {
				fail();
			} catch (IOException e) {
				fail();
			} catch (DuplicateChunkException e) {
				fail();
			} catch (NoSuchChunkException e) {
				fail();
			}
		} finally {
			FileUtils.deleteDirectory(chunkStorageLocA);
		}
	}

	@Test
	public void chunkFetchTest() throws Exception {
		int userAPort = 7777;
		int userBPort = 7778;
		File chunkStorageLocA = new File("./target/chunks1");
		File chunkStorageLocB = new File("./target/chunks2");
		ChunkManager managerA = null;
		ChunkManager managerB = null;
		NotificationCenter notificationCenter = NotificationCenter.defaultCenter();
		NotificationHandler notificationHandler = null;
		try {
			Configuration configA = new DefaultConfiguration();
			configA.setConfigurationOption("chunkStorageLoc", chunkStorageLocA.getAbsolutePath());
			configA.setConfigurationOption("chunkStorageQuota", "1000");
			configA.setConfigurationOption("chunkListenerPort", "" + userAPort);
			Configuration configB = new DefaultConfiguration();
			configB.setConfigurationOption("chunkStorageLoc", chunkStorageLocB.getAbsolutePath());
			configB.setConfigurationOption("chunkStorageQuota", "1000");
			configB.setConfigurationOption("chunkListenerPort", "" + userBPort);

			String testContainerId = "containerA";
			String userAAuthToken = "userAAuthToken";
			String userBAuthToken = "userBAuthToken";
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
			workspace.setGuid(12345L);
			workspaces.add(workspace);
			FileChunk fooChunk = new FileChunk();
			fooChunk.setChunkID("foo");
			FileChunk barChunk = new FileChunk();
			barChunk.setChunkID("bar");
			FileChunk bazChunk = new FileChunk();
			bazChunk.setChunkID("baz");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			io.topiacoin.model.File f = new io.topiacoin.model.File();
			f.setEntryID("FileFoo");

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace(12345L, memberA);
			modelA.addMemberToWorkspace(12345L, memberB);
			modelA.addFileToWorkspace(12345L, f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", bazChunk);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace(12345L, memberA);
			modelB.addMemberToWorkspace(12345L, memberB);
			modelB.addFileToWorkspace(12345L, f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", bazChunk);

			managerA = new ChunkManager(configA, modelA);
			assertTrue(!managerA.hasChunk("foo"));
			assertTrue(!managerA.hasChunk("bar"));
			assertTrue(!managerA.hasChunk("baz"));

			managerB = new ChunkManager(configB, modelB);
			assertTrue(!managerB.hasChunk("foo"));
			assertTrue(!managerB.hasChunk("bar"));
			assertTrue(!managerB.hasChunk("baz"));
			managerB.addChunk("foo", "FOODATA".getBytes());
			managerB.addChunk("bar", "BARDATA".getBytes());
			managerB.addChunk("baz", "BAZDATA".getBytes());
			assertTrue(managerB.hasChunk("foo"));
			assertTrue(managerB.hasChunk("bar"));
			assertTrue(managerB.hasChunk("baz"));

			UserNode userANode = managerA.getMyUserNode();
			UserNode userBNode = managerB.getMyUserNode();
			modelA.addUserNode(userBNode);
			modelB.addUserNode(userANode);

			List<String> chunksIWant = new ArrayList<>();
			chunksIWant.add("foo");
			chunksIWant.add("bar");
			chunksIWant.add("baz");
			String stateExpected = "StateTest";
			Set<Integer> expectedIntegers = new HashSet<Integer>();
			expectedIntegers.add(0);
			expectedIntegers.add(1);
			expectedIntegers.add(2);
			expectedIntegers.add(3);
			final CountDownLatch lock = new CountDownLatch(chunksIWant.size() + 2);
			notificationHandler = new NotificationHandler() {
				@Override public void handleNotification(Notification notification) {
					assertEquals(3, notification.getNotificationInfo().get("total"));
					Integer s = (Integer) notification.getNotificationInfo().get("completed");
					assertTrue(expectedIntegers.remove(s));
					lock.countDown();
				}
			};
			notificationCenter.addHandler(notificationHandler, "transferProgress", stateExpected);
			managerA.fetchChunks(chunksIWant, workspace.getGuid(), new ChunksFetchHandler() {
				@Override public void finishedFetchingChunks(List<String> successfulChunks, List<String> unsuccessfulChunks, Object state) {
					assertTrue(successfulChunks.containsAll(chunksIWant));
					assertEquals(successfulChunks.size(), chunksIWant.size());
					assertTrue(unsuccessfulChunks.isEmpty());
					assertEquals(stateExpected, state);
					lock.countDown();
				}

				@Override public void errorFetchingChunks(String message, Exception cause, Object state) {
					fail();
				}
			}, stateExpected);

			assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
		} finally {
			FileUtils.deleteDirectory(chunkStorageLocA);
			FileUtils.deleteDirectory(chunkStorageLocB);
			if(managerA != null) {
				managerA.stop();
			}
			if(managerB != null) {
				managerB.stop();
			}
			if(notificationHandler != null) {
				notificationCenter.removeHandler(notificationHandler);
			}
		}
	}

	@Test
	public void chunkFetchWhenIAlreadyHaveOneTest() throws Exception {
		int userAPort = 7777;
		int userBPort = 7778;
		File chunkStorageLocA = new File("./target/chunks1");
		File chunkStorageLocB = new File("./target/chunks2");
		ChunkManager managerA = null;
		ChunkManager managerB = null;
		try {
			Configuration configA = new DefaultConfiguration();
			configA.setConfigurationOption("chunkStorageLoc", chunkStorageLocA.getAbsolutePath());
			configA.setConfigurationOption("chunkStorageQuota", "1000");
			configA.setConfigurationOption("chunkListenerPort", "" + userAPort);
			Configuration configB = new DefaultConfiguration();
			configB.setConfigurationOption("chunkStorageLoc", chunkStorageLocB.getAbsolutePath());
			configB.setConfigurationOption("chunkStorageQuota", "1000");
			configB.setConfigurationOption("chunkListenerPort", "" + userBPort);

			String testContainerId = "containerA";
			String userAAuthToken = "userAAuthToken";
			String userBAuthToken = "userBAuthToken";
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
			workspace.setGuid(12345L);
			workspaces.add(workspace);
			FileChunk fooChunk = new FileChunk();
			fooChunk.setChunkID("foo");
			FileChunk barChunk = new FileChunk();
			barChunk.setChunkID("bar");
			FileChunk bazChunk = new FileChunk();
			bazChunk.setChunkID("baz");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			io.topiacoin.model.File f = new io.topiacoin.model.File();
			f.setEntryID("FileFoo");

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace(12345L, memberA);
			modelA.addMemberToWorkspace(12345L, memberB);
			modelA.addFileToWorkspace(12345L, f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", bazChunk);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace(12345L, memberA);
			modelB.addMemberToWorkspace(12345L, memberB);
			modelB.addFileToWorkspace(12345L, f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", bazChunk);

			managerA = new ChunkManager(configA, modelA);
			assertTrue(!managerA.hasChunk("foo"));
			assertTrue(!managerA.hasChunk("bar"));
			assertTrue(!managerA.hasChunk("baz"));
			managerA.addChunk("foo", "FOODATA".getBytes());
			assertTrue(managerA.hasChunk("foo"));

			managerB = new ChunkManager(configB, modelB);
			assertTrue(!managerB.hasChunk("foo"));
			assertTrue(!managerB.hasChunk("bar"));
			assertTrue(!managerB.hasChunk("baz"));
			managerB.addChunk("foo", "FOODATA".getBytes());
			managerB.addChunk("bar", "BARDATA".getBytes());
			managerB.addChunk("baz", "BAZDATA".getBytes());
			assertTrue(managerB.hasChunk("foo"));
			assertTrue(managerB.hasChunk("bar"));
			assertTrue(managerB.hasChunk("baz"));

			UserNode userANode = managerA.getMyUserNode();
			UserNode userBNode = managerB.getMyUserNode();
			modelA.addUserNode(userBNode);
			modelB.addUserNode(userANode);

			List<String> chunksIWant = new ArrayList<>();
			chunksIWant.add("foo");
			chunksIWant.add("bar");
			chunksIWant.add("baz");
			String stateExpected = "StateTest";
			final CountDownLatch lock = new CountDownLatch(1);
			managerA.fetchChunks(chunksIWant, workspace.getGuid(), new ChunksFetchHandler() {
				@Override public void finishedFetchingChunks(List<String> successfulChunks, List<String> unsuccessfulChunks, Object state) {
					assertTrue(successfulChunks.containsAll(chunksIWant));
					assertEquals(successfulChunks.size(), chunksIWant.size());
					assertTrue(unsuccessfulChunks.isEmpty());
					assertEquals(stateExpected, state);
					lock.countDown();
				}

				@Override public void errorFetchingChunks(String message, Exception cause, Object state) {
					fail();
				}
			}, stateExpected);

			assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
		} finally {
			FileUtils.deleteDirectory(chunkStorageLocA);
			FileUtils.deleteDirectory(chunkStorageLocB);
			if(managerA != null) {
				managerA.stop();
			}
			if(managerB != null) {
				managerB.stop();
			}
		}
	}

	@Test
	public void chunkFetchFailBuildPlanTest() throws Exception {
		int userAPort = 7777;
		int userBPort = 7778;
		File chunkStorageLocA = new File("./target/chunks1");
		File chunkStorageLocB = new File("./target/chunks2");
		ChunkManager managerA = null;
		ChunkManager managerB = null;
		try {
			Configuration configA = new DefaultConfiguration();
			configA.setConfigurationOption("chunkStorageLoc", chunkStorageLocA.getAbsolutePath());
			configA.setConfigurationOption("chunkStorageQuota", "1000");
			configA.setConfigurationOption("chunkListenerPort", "" + userAPort);
			Configuration configB = new DefaultConfiguration();
			configB.setConfigurationOption("chunkStorageLoc", chunkStorageLocB.getAbsolutePath());
			configB.setConfigurationOption("chunkStorageQuota", "1000");
			configB.setConfigurationOption("chunkListenerPort", "" + userBPort);

			String testContainerId = "containerA";
			String userAAuthToken = "userAAuthToken";
			String userBAuthToken = "userBAuthToken";
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
			workspace.setGuid(12345L);
			workspaces.add(workspace);
			FileChunk fooChunk = new FileChunk();
			fooChunk.setChunkID("foo");
			FileChunk barChunk = new FileChunk();
			barChunk.setChunkID("bar");
			FileChunk bazChunk = new FileChunk();
			bazChunk.setChunkID("baz");
			FileChunk bingoChunk = new FileChunk();
			bazChunk.setChunkID("bingo");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			io.topiacoin.model.File f = new io.topiacoin.model.File();
			f.setEntryID("FileFoo");

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace(12345L, memberA);
			modelA.addMemberToWorkspace(12345L, memberB);
			modelA.addFileToWorkspace(12345L, f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", bazChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", bingoChunk);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace(12345L, memberA);
			modelB.addMemberToWorkspace(12345L, memberB);
			modelB.addFileToWorkspace(12345L, f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", bazChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", bingoChunk);

			managerA = new ChunkManager(configA, modelA);
			assertTrue(!managerA.hasChunk("foo"));
			assertTrue(!managerA.hasChunk("bar"));
			assertTrue(!managerA.hasChunk("baz"));
			assertTrue(!managerA.hasChunk("bingo"));

			managerB = new ChunkManager(configB, modelB);
			assertTrue(!managerB.hasChunk("foo"));
			assertTrue(!managerB.hasChunk("bar"));
			assertTrue(!managerB.hasChunk("baz"));
			assertTrue(!managerB.hasChunk("bingo"));
			managerB.addChunk("foo", "FOODATA".getBytes());
			managerB.addChunk("bar", "BARDATA".getBytes());
			managerB.addChunk("baz", "BAZDATA".getBytes());
			assertTrue(managerB.hasChunk("foo"));
			assertTrue(managerB.hasChunk("bar"));
			assertTrue(managerB.hasChunk("baz"));

			UserNode userANode = managerA.getMyUserNode();
			UserNode userBNode = managerB.getMyUserNode();
			modelA.addUserNode(userBNode);
			modelB.addUserNode(userANode);

			List<String> chunksIWant = new ArrayList<>();
			chunksIWant.add("foo");
			chunksIWant.add("bar");
			chunksIWant.add("baz");
			List<String> chunksIAintGonnaGet = new ArrayList<>();
			chunksIAintGonnaGet.add("bingo");
			List<String> chunksImGonnaAskFor = new ArrayList<>();
			chunksImGonnaAskFor.addAll(chunksIWant);
			chunksImGonnaAskFor.addAll(chunksIAintGonnaGet);
			String stateExpected = "StateTest";
			final CountDownLatch lock = new CountDownLatch(1);
			managerA.fetchChunks(chunksImGonnaAskFor, workspace.getGuid(), new ChunksFetchHandler() {
				@Override public void finishedFetchingChunks(List<String> successfulChunks, List<String> unsuccessfulChunks, Object state) {
					fail();
				}

				@Override public void errorFetchingChunks(String message, Exception cause, Object state) {
					lock.countDown();
				}
			}, stateExpected);

			assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
		} finally {
			FileUtils.deleteDirectory(chunkStorageLocA);
			FileUtils.deleteDirectory(chunkStorageLocB);
			if(managerA != null) {
				managerA.stop();
			}
			if(managerB != null) {
				managerB.stop();
			}
		}
	}

	@Test
	public void chunkFetchFailToFetchAllTest() throws Exception {
		int userAPort = 7777;
		int userBPort = 7778;
		File chunkStorageLocA = new File("./target/chunks1");
		File chunkStorageLocB = new File("./target/chunks2");
		ChunkManager managerA = null;
		final ChunkManager managerB;
		NotificationCenter notificationCenter = NotificationCenter.defaultCenter();
		NotificationHandler notificationHandler = null;
		try {
			Configuration configA = new DefaultConfiguration();
			configA.setConfigurationOption("chunkStorageLoc", chunkStorageLocA.getAbsolutePath());
			configA.setConfigurationOption("chunkStorageQuota", "1000");
			configA.setConfigurationOption("chunkListenerPort", "" + userAPort);
			configA.setConfigurationOption("protocolTimeoutMs", "" + 3000);
			Configuration configB = new DefaultConfiguration();
			configB.setConfigurationOption("chunkStorageLoc", chunkStorageLocB.getAbsolutePath());
			configB.setConfigurationOption("chunkStorageQuota", "1000");
			configB.setConfigurationOption("chunkListenerPort", "" + userBPort);
			configA.setConfigurationOption("protocolTimeoutMs", "" + 3000);

			String testContainerId = "containerA";
			String userAAuthToken = "userAAuthToken";
			String userBAuthToken = "userBAuthToken";
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
			workspace.setGuid(12345L);
			workspaces.add(workspace);
			FileChunk fooChunk = new FileChunk();
			fooChunk.setChunkID("foo");
			FileChunk barChunk = new FileChunk();
			barChunk.setChunkID("bar");
			FileChunk bazChunk = new FileChunk();
			bazChunk.setChunkID("baz");
			FileVersion v = new FileVersion();
			v.setVersionID("VersionFoo");
			io.topiacoin.model.File f = new io.topiacoin.model.File();
			f.setEntryID("FileFoo");

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
			modelA.setCurrentUser(currentUserA);
			modelA.addWorkspace(workspace);
			modelA.addMemberToWorkspace(12345L, memberA);
			modelA.addMemberToWorkspace(12345L, memberB);
			modelA.addFileToWorkspace(12345L, f);
			modelA.addFileVersion("FileFoo", v);
			modelA.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelA.addChunkForFile("FileFoo", "VersionFoo", bazChunk);

			DataModel modelB = new TestDataModel();
			modelB.addUser(new User(currentUserA));
			modelB.addUser(new User(currentUserB));
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace(12345L, memberA);
			modelB.addMemberToWorkspace(12345L, memberB);
			modelB.addFileToWorkspace(12345L, f);
			modelB.addFileVersion("FileFoo", v);
			modelB.addChunkForFile("FileFoo", "VersionFoo", fooChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", barChunk);
			modelB.addChunkForFile("FileFoo", "VersionFoo", bazChunk);

			managerA = new ChunkManager(configA, modelA);
			assertTrue(!managerA.hasChunk("foo"));
			assertTrue(!managerA.hasChunk("bar"));
			assertTrue(!managerA.hasChunk("baz"));

			managerB = new ChunkManager(configB, modelB);
			assertTrue(!managerB.hasChunk("foo"));
			assertTrue(!managerB.hasChunk("bar"));
			assertTrue(!managerB.hasChunk("baz"));
			managerB.addChunk("foo", "FOODATA".getBytes());
			managerB.addChunk("bar", "BARDATA".getBytes());
			managerB.addChunk("baz", "BAZDATA".getBytes());
			assertTrue(managerB.hasChunk("foo"));
			assertTrue(managerB.hasChunk("bar"));
			assertTrue(managerB.hasChunk("baz"));

			UserNode userANode = managerA.getMyUserNode();
			UserNode userBNode = managerB.getMyUserNode();
			modelA.addUserNode(userBNode);
			modelB.addUserNode(userANode);

			List<String> chunksIWant = new ArrayList<>();
			chunksIWant.add("foo");
			chunksIWant.add("bar");
			chunksIWant.add("baz");
			String stateExpected = "StateTest";
			final CountDownLatch lock = new CountDownLatch(1);
			notificationHandler = new NotificationHandler() {
				@Override public void handleNotification(Notification notification) {
					//Murdertime
					managerB.stop();
				}
			};
			notificationCenter.addHandler(notificationHandler, "transferProgress", stateExpected);
			managerA.fetchChunks(chunksIWant, workspace.getGuid(), new ChunksFetchHandler() {
				@Override public void finishedFetchingChunks(List<String> successfulChunks, List<String> unsuccessfulChunks, Object state) {
					assertTrue(unsuccessfulChunks.containsAll(chunksIWant));
					assertEquals(unsuccessfulChunks.size(), chunksIWant.size());
					assertEquals(stateExpected, state);
					lock.countDown();
				}

				@Override public void errorFetchingChunks(String message, Exception cause, Object state) {
					fail();
				}
			}, stateExpected);

			assertTrue("Chunks never fetched", lock.await(10, TimeUnit.SECONDS));
		} finally {
			FileUtils.deleteDirectory(chunkStorageLocA);
			FileUtils.deleteDirectory(chunkStorageLocB);
			if(managerA != null) {
				managerA.stop();
			}
			if(notificationHandler != null) {
				notificationCenter.removeHandler(notificationHandler);
			}
		}
	}
}
