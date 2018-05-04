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
import java.util.List;
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
		try {
			Configuration configA = new DefaultConfiguration();
			configA.setConfigurationOption("chunkStorageLoc", chunkStorageLocA.getAbsolutePath());
			configA.setConfigurationOption("chunkStorageQuota", "1000");
			configA.setConfigurationOption("chunkListenerPort", "" + userAPort);
			Configuration configB = new DefaultConfiguration();
			configB.setConfigurationOption("chunkStorageLoc", chunkStorageLocB.getAbsolutePath());
			configB.setConfigurationOption("chunkStorageQuota", "1000");
			configB.setConfigurationOption("chunkListenerPort", "" + userBPort);

			//final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			//final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
			//System.out.println("USERATK: " + DatatypeConverter.printHexBinary(userAChunkTransferKeyPair.getPublic().getEncoded()));
			//System.out.println("USERBTK: " + DatatypeConverter.printHexBinary(userBChunkTransferKeyPair.getPublic().getEncoded()));
			String testContainerId = "containerA";
			String userAAuthToken = "userAAuthToken";
			String userBAuthToken = "userBAuthToken";
			//List<UserNode> containerAMemberNodes = new ArrayList<>();
			//containerAMemberNodes.add(userAMemberNode);
			//containerAMemberNodes.add(userBMemberNode);
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
			io.topiacoin.model.File f = new io.topiacoin.model.File();
			f.setEntryID("FileFoo");

			DataModel modelA = new TestDataModel();
			modelA.addUser(new User(currentUserA));
			modelA.addUser(new User(currentUserB));
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
			modelB.setCurrentUser(currentUserB);
			modelB.addWorkspace(workspace);
			modelB.addMemberToWorkspace("containerA", memberA);
			modelB.addMemberToWorkspace("containerA", memberB);
			modelB.addFileToWorkspace("containerA", f);
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

			//UserNode userANode = new UserNode("userA", "127.0.0.1", userAPort, managerA._myChunkTransferPair.getPublic().getEncoded());
			UserNode userANode = managerA.getMyUserNode();
			//UserNode userBNode = new UserNode("userB", "127.0.0.1", userBPort, managerB._myChunkTransferPair.getPublic().getEncoded());
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
}
