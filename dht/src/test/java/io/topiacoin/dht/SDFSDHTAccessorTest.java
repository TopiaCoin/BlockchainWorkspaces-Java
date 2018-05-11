package io.topiacoin.dht;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.Member;
import io.topiacoin.model.MemberNode;
import io.topiacoin.model.User;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.MemberAlreadyExistsException;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import io.topiacoin.model.exceptions.UserAlreadyExistsException;
import io.topiacoin.model.exceptions.WorkspaceAlreadyExistsException;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SDFSDHTAccessorTest {

	@Test
	public void testSingleUserWorkspaceCRUD() throws NoSuchUserException, CryptographicException, NoSuchAlgorithmException, IOException, WorkspaceAlreadyExistsException, UserAlreadyExistsException, InterruptedException, NoSuchWorkspaceException, MemberAlreadyExistsException {
		Configuration config = new DefaultConfiguration();
		DHTConfiguration dhtconfig = new DHTTestConfiguration(config);
		KeyPair DHTKeyPair = CryptoUtils.generateECKeyPair();
		DHT fauxBootstrapNode = new DHT(0, DHTKeyPair, dhtconfig);
		fauxBootstrapNode.start(false);
		dhtconfig.setBootstrapNodeHostname(fauxBootstrapNode.getNode().getAddress().getHostName());
		dhtconfig.setBootstrapNodeID(fauxBootstrapNode.getNode().getNodeID().getNodeID());
		dhtconfig.setBootstrapNodePort(fauxBootstrapNode.getNode().getPort());
		KeyPair kp1 = CryptoUtils.generateECKeyPair();

		DataModel model1 = new TestDataModel();
		model1.setCurrentUser(new CurrentUser("user1-63HzRNRH7h", "user1-63HzRNRH7h@hotmail.com", kp1.getPublic(), kp1.getPrivate()));
		Member member1 = new Member("user1-63HzRNRH7h", 1, System.currentTimeMillis(), "user1-63HzRNRH7h", "authToken");
		List<Member> members = new ArrayList<>();
		members.add(member1);
		Workspace workspace = new Workspace("My Workspace", "a test Workspace", 1, CryptoUtils.generateAESKey(), "wks1", System.currentTimeMillis(), null, null, null);
		model1.addWorkspace(workspace);
		model1.addMemberToWorkspace(workspace.getGuid(), member1);
		model1.addUser(new User(member1.getUserID(), "foo@bar.com", kp1.getPublic()));

		MemberNode user1Node1 = new MemberNode("user1-63HzRNRH7h", "user1-63HzRNRH7h.ms.edu", 1);

		Map<String, SecretKey> workspaceNodeKeys = new HashMap<>();

		File configDir = new File(dhtconfig.getNodeDataFolder());
		File routingTable = new File(configDir, "routingTable");
		File hashTable = new File(configDir, "hashTable");
		try {
			Assert.assertTrue(!routingTable.exists());
			Assert.assertTrue(!hashTable.exists());
			SDFSDHTAccessor accessor1 = new SDFSDHTAccessor(config, model1);
			try {
				//Make sure we start empty
				Assert.assertEquals(0, accessor1.fetchMyWorkspaceIDs().size());
				//Make sure removal is idempotent
				accessor1.removeMemberFromWorkspace(workspace.getGuid(), member1);
				try {
					//Create Workspace
					SecretKey nodeKey = accessor1.createWorkspace(workspace.getGuid());
					workspaceNodeKeys.put(workspace.getGuid(), nodeKey);
					//Test that removing a Member Node is idempotent
					boolean removed = accessor1.removeMyMemberNode(workspace.getGuid(), user1Node1, workspaceNodeKeys.get(workspace.getGuid()));
					Assert.assertTrue(!removed);
					Assert.assertEquals(0, accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid())).size());
					//Add my Member Node
					Assert.assertTrue(accessor1.addMyMemberNode(workspace.getGuid(), user1Node1, nodeKey));
					List<MemberNode> memberNodes = accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid()));
					Assert.assertEquals(1, memberNodes.size());
					Assert.assertEquals(user1Node1, memberNodes.get(0));
					//Add my Member Node again shouldn't work
					Assert.assertTrue(!accessor1.addMyMemberNode(workspace.getGuid(), user1Node1, nodeKey));
					memberNodes = accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid()));
					Assert.assertEquals(1, memberNodes.size());
					Assert.assertEquals(user1Node1, memberNodes.get(0));
					//Remove my Member Node
					removed = accessor1.removeMyMemberNode(workspace.getGuid(), user1Node1, workspaceNodeKeys.get(workspace.getGuid()));
					Assert.assertTrue(removed);
					Assert.assertEquals(0, accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid())).size());
					//Add my Member Node again (I think we'll need it)
					Assert.assertTrue(accessor1.addMyMemberNode(workspace.getGuid(), user1Node1, nodeKey));
					memberNodes = accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid()));
					Assert.assertEquals(1, memberNodes.size());
					Assert.assertEquals(user1Node1, memberNodes.get(0));
					//I should have a workspaceID now.
					List<String> workspaceIDs = accessor1.fetchMyWorkspaceIDs();
					Assert.assertEquals(1, workspaceIDs.size());
					Assert.assertEquals(workspace.getGuid(), workspaceIDs.get(0));
					//I should be able to fetch the Workspace Node Key, which should contain the one I stored in the map
					SecretKey key = accessor1.fetchMyWorkspaceNodeKey(workspace.getGuid());
					Assert.assertNotNull(key);
					Assert.assertArrayEquals(workspaceNodeKeys.get(workspace.getGuid()).getEncoded(), key.getEncoded());
					//Clean up, and make sure it actually cleans stuff up
					accessor1.leaveWorkspace(workspace.getGuid());
					Assert.assertEquals(0, accessor1.fetchMyWorkspaceIDs().size());
					Assert.assertEquals(0, accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid())).size());
					Assert.assertNull(accessor1.fetchMyWorkspaceNodeKey(workspace.getGuid()));
				} finally {
					System.out.println("finally 1");
					//Clean up
					accessor1.leaveWorkspace(workspace.getGuid());
				}
			} finally {
				System.out.println("finally 2");
				accessor1.stop();
			}
		} finally {
			System.out.println("finally 3");
			routingTable.delete();
			hashTable.delete();
		}
	}

	@Test
	public void testInvite() throws NoSuchUserException, CryptographicException, NoSuchAlgorithmException, IOException, WorkspaceAlreadyExistsException, UserAlreadyExistsException, InterruptedException, NoSuchWorkspaceException, MemberAlreadyExistsException {
		Configuration config = new DefaultConfiguration();
		DHTConfiguration dhtconfig = new DHTTestConfiguration(config);
		KeyPair DHTKeyPair = CryptoUtils.generateECKeyPair();
		DHT fauxBootstrapNode = new DHT(0, DHTKeyPair, dhtconfig);
		fauxBootstrapNode.start(false);
		dhtconfig.setBootstrapNodeHostname(fauxBootstrapNode.getNode().getAddress().getHostName());
		dhtconfig.setBootstrapNodeID(fauxBootstrapNode.getNode().getNodeID().getNodeID());
		dhtconfig.setBootstrapNodePort(fauxBootstrapNode.getNode().getPort());
		KeyPair kp1 = CryptoUtils.generateECKeyPair();
		KeyPair kp2 = CryptoUtils.generateECKeyPair();

		DataModel model1 = new TestDataModel();
		DataModel model2 = new TestDataModel();
		model1.setCurrentUser(new CurrentUser("user1-63HzRNRH7h", "user1-63HzRNRH7h@hotmail.com", kp1.getPublic(), kp1.getPrivate()));
		model2.setCurrentUser(new CurrentUser("user2-f7zql0bn25", "user2-f7zql0bn25@hotmail.com", kp2.getPublic(), kp2.getPrivate()));
		Member member1 = new Member("user1-63HzRNRH7h", 1, System.currentTimeMillis(), "user1-63HzRNRH7h", "authToken");
		Member memberToInvite = new Member("user2-f7zql0bn25", 1, System.currentTimeMillis(), "user1-63HzRNRH7h", "authToken2");
		Workspace workspace = new Workspace("My Workspace", "a test Workspace", 1, CryptoUtils.generateAESKey(), "wks1", System.currentTimeMillis(), null, null, null);
		model1.addWorkspace(workspace);
		model1.addMemberToWorkspace(workspace.getGuid(), member1);
		model1.addUser(new User(member1.getUserID(), "foo@bar.com", kp1.getPublic()));
		User userToInvite = new User("user2-f7zql0bn25", "whatever@blah.com", kp2.getPublic());
		model1.addUser(userToInvite);
		model2.addUser(userToInvite);

		MemberNode user1Node1 = new MemberNode("user1-63HzRNRH7h", "user1-63HzRNRH7h.ms.edu", 1);
		MemberNode user2Node1 = new MemberNode("user2-f7zql0bn25", "uuser2-f7zql0bn25.whatever.gov", 2);

		Map<String, SecretKey> workspaceNodeKeys1 = new HashMap<>();
		Map<String, SecretKey> workspaceNodeKeys2 = new HashMap<>();

		File configDir = new File(dhtconfig.getNodeDataFolder());
		File routingTable = new File(configDir, "routingTable");
		File hashTable = new File(configDir, "hashTable");
		try {
			Assert.assertTrue(!routingTable.exists());
			Assert.assertTrue(!hashTable.exists());
			SDFSDHTAccessor accessor1 = new SDFSDHTAccessor(config, model1);
			SDFSDHTAccessor accessor2 = new SDFSDHTAccessor(config, model2);
			try {
				//Make sure we start empty
				Assert.assertEquals(0, accessor1.fetchMyWorkspaceIDs().size());
				Assert.assertEquals(0, accessor2.fetchMyWorkspaceIDs().size());
				try {
					//Create Workspace
					SecretKey nodeKey = accessor1.createWorkspace(workspace.getGuid());
					workspaceNodeKeys1.put(workspace.getGuid(), nodeKey);
					//I should have a workspaceID now.
					List<String> workspaceIDs = accessor1.fetchMyWorkspaceIDs();
					Assert.assertEquals(1, workspaceIDs.size());
					Assert.assertEquals(workspace.getGuid(), workspaceIDs.get(0));
					//I should be able to fetch the Workspace Node Key, which should contain the one I stored in the map
					SecretKey key = accessor1.fetchMyWorkspaceNodeKey(workspace.getGuid());
					Assert.assertNotNull(key);
					Assert.assertArrayEquals(workspaceNodeKeys1.get(workspace.getGuid()).getEncoded(), key.getEncoded());
					//Add my Member Node
					Assert.assertTrue(accessor1.addMyMemberNode(workspace.getGuid(), user1Node1, nodeKey));
					List<MemberNode> memberNodes = accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys1.get(workspace.getGuid()));
					Assert.assertEquals(1, memberNodes.size());
					Assert.assertEquals(user1Node1, memberNodes.get(0));
					//Invite Member2
					accessor1.addInvitation(workspace.getGuid(), userToInvite, workspaceNodeKeys1.get(workspace.getGuid()));
					model1.addMemberToWorkspace(workspace.getGuid(), memberToInvite);
					//Wait for the network
					Thread.sleep(1000);

					workspaceIDs = accessor2.fetchMyWorkspaceIDs();
					Assert.assertEquals(1, workspaceIDs.size());
					Assert.assertEquals(workspace.getGuid(), workspaceIDs.get(0));
					//I should be able to fetch the Workspace Node Key, which should contain the one I stored in the map
					key = accessor2.fetchMyWorkspaceNodeKey(workspace.getGuid());
					Assert.assertNotNull(key);
					Assert.assertArrayEquals(workspaceNodeKeys1.get(workspace.getGuid()).getEncoded(), key.getEncoded());
					//Add my Member Node (I think prod code should fetch,add,fetch, but w/e
					Assert.assertTrue(accessor2.addMyMemberNode(workspace.getGuid(), user2Node1, nodeKey));
					workspaceNodeKeys2.put(workspace.getGuid(), nodeKey);
					memberNodes = accessor2.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys2.get(workspace.getGuid()));
					Assert.assertEquals(2, memberNodes.size());
					Assert.assertTrue(memberNodes.contains(user1Node1));
					Assert.assertTrue(memberNodes.contains(user2Node1));
					//At this point we have enough info to connect to the blockchain and should be able to pull membership info
					model2.addWorkspace(workspace);
					model2.addMemberToWorkspace(workspace.getGuid(), member1);
					model2.addMemberToWorkspace(workspace.getGuid(), memberToInvite);

					//Clean up, and make sure it actually cleans stuff up
					accessor1.leaveWorkspace(workspace.getGuid());
					Assert.assertEquals(0, accessor1.fetchMyWorkspaceIDs().size());
					Assert.assertNull(accessor1.fetchMyWorkspaceNodeKey(workspace.getGuid()));
					Assert.assertEquals(1, accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys2.get(workspace.getGuid())).size());
					//Wait for the network
					Thread.sleep(1000);
					accessor2.leaveWorkspace(workspace.getGuid());
					Assert.assertEquals(0, accessor2.fetchMyWorkspaceIDs().size());
					Assert.assertNull(accessor2.fetchMyWorkspaceNodeKey(workspace.getGuid()));
					Assert.assertEquals(0, accessor2.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys2.get(workspace.getGuid())).size());
				} finally {
					System.out.println("finally 1");
					//Clean up
					accessor1.leaveWorkspace(workspace.getGuid());
				}
			} finally {
				System.out.println("finally 2");
				accessor1.stop();
			}
		} finally {
			System.out.println("finally 3");
			routingTable.delete();
			hashTable.delete();
		}
	}

	@Test
	public void testNegativeThings() throws Exception {
		Configuration config = new DefaultConfiguration();
		DHTConfiguration dhtconfig = new DHTTestConfiguration(config);
		KeyPair DHTKeyPair = CryptoUtils.generateECKeyPair();
		DHT fauxBootstrapNode = new DHT(0, DHTKeyPair, dhtconfig);
		fauxBootstrapNode.start(false);
		dhtconfig.setBootstrapNodeHostname(fauxBootstrapNode.getNode().getAddress().getHostName());
		dhtconfig.setBootstrapNodeID(fauxBootstrapNode.getNode().getNodeID().getNodeID());
		dhtconfig.setBootstrapNodePort(fauxBootstrapNode.getNode().getPort());
		KeyPair kp1 = CryptoUtils.generateECKeyPair();

		DataModel model1 = new TestDataModel();
		model1.setCurrentUser(new CurrentUser("user1-63HzRNRH7h", "user1-63HzRNRH7h@hotmail.com", kp1.getPublic(), kp1.getPrivate()));
		Member member1 = new Member("user1-63HzRNRH7h", 1, System.currentTimeMillis(), "user1-63HzRNRH7h", "authToken");
		List<Member> members = new ArrayList<>();
		members.add(member1);
		Workspace workspace = new Workspace("My Workspace", "a test Workspace", 1, CryptoUtils.generateAESKey(), "wks1", System.currentTimeMillis(), null, null, null);
		model1.addWorkspace(workspace);
		model1.addMemberToWorkspace(workspace.getGuid(), member1);
		model1.addUser(new User(member1.getUserID(), "foo@bar.com", kp1.getPublic()));

		MemberNode user1Node1 = new MemberNode("user1-63HzRNRH7h", "user1-63HzRNRH7h.ms.edu", 1);
		MemberNode user2Node1 = new MemberNode("user2-f7zql0bn25", "uuser2-f7zql0bn25.whatever.gov", 2);

		Map<String, SecretKey> workspaceNodeKeys = new HashMap<>();

		File configDir = new File(dhtconfig.getNodeDataFolder());
		File routingTable = new File(configDir, "routingTable");
		File hashTable = new File(configDir, "hashTable");
		try {
			Assert.assertTrue(!routingTable.exists());
			Assert.assertTrue(!hashTable.exists());
			SDFSDHTAccessor accessor1 = new SDFSDHTAccessor(config, model1);
			try {
				//Make sure we start empty
				Assert.assertEquals(0, accessor1.fetchMyWorkspaceIDs().size());
				try {
					//Create Workspace
					SecretKey nodeKey = accessor1.createWorkspace(workspace.getGuid());
					workspaceNodeKeys.put(workspace.getGuid(), nodeKey);
					//Add somebody else's Member Node - should fail
					try {
						accessor1.addMyMemberNode(workspace.getGuid(), user2Node1, nodeKey);
						Assert.fail();
					} catch(IllegalArgumentException ex) {
						//Good
					}
					//Remove somebody else's Member Node - should fail
					try {
						accessor1.removeMyMemberNode(workspace.getGuid(), user2Node1, nodeKey);
						Assert.fail();
					} catch(IllegalArgumentException ex) {
						//Good
					}
					//Remove somebody that isn't in the workspace
					try {
						accessor1.removeMemberFromWorkspace(workspace.getGuid(), new Member(user2Node1.getUserID(), 1, System.currentTimeMillis(), "whocares", "not important"));
						Assert.fail();
					} catch(NoSuchUserException ex) {
						//good
					}
				} finally {
					System.out.println("finally 1");
					//Clean up
					accessor1.leaveWorkspace(workspace.getGuid());
				}
			} finally {
				System.out.println("finally 2");
				accessor1.stop();
			}
		} finally {
			System.out.println("finally 3");
			routingTable.delete();
			hashTable.delete();
		}
	}
}
