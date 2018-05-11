package io.topiacoin.dht;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DHTWorkspaceEntry;
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
	private static final long NETWORK_WAIT_SLEEP_TIME = 100;

	@Test
	public void testSingleUserWorkspaceCRUDLowLevel() throws NoSuchUserException, CryptographicException, NoSuchAlgorithmException, IOException, WorkspaceAlreadyExistsException, UserAlreadyExistsException, InterruptedException, NoSuchWorkspaceException, MemberAlreadyExistsException {
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
					SecretKey nodeKey = accessor1.addNewWorkspaceToDHTInternal(workspace.getGuid());
					workspaceNodeKeys.put(workspace.getGuid(), nodeKey);
					//Test that removing a Member Node is idempotent
					boolean removed = accessor1.removeMyMemberNode(workspace.getGuid());
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
					removed = accessor1.removeMyMemberNode(workspace.getGuid());
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
	public void testInviteLowLevel() throws NoSuchUserException, CryptographicException, NoSuchAlgorithmException, IOException, WorkspaceAlreadyExistsException, UserAlreadyExistsException, InterruptedException, NoSuchWorkspaceException, MemberAlreadyExistsException {
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
					SecretKey nodeKey = accessor1.addNewWorkspaceToDHTInternal(workspace.getGuid());
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
					Thread.sleep(NETWORK_WAIT_SLEEP_TIME);

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
					Thread.sleep(NETWORK_WAIT_SLEEP_TIME);
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
				accessor2.stop();
			}
		} finally {
			System.out.println("finally 3");
			routingTable.delete();
			hashTable.delete();
		}
	}

	@Test
	public void testSingleUserWorkspaceCRUDHighLevel() throws NoSuchUserException, CryptographicException, NoSuchAlgorithmException, IOException, WorkspaceAlreadyExistsException, UserAlreadyExistsException, InterruptedException, NoSuchWorkspaceException, MemberAlreadyExistsException {
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
				Assert.assertEquals(0, accessor1.fetchMyDHTWorkspaces().size());
				try {
					//Create Workspace
					DHTWorkspaceEntry entry = accessor1.addNewWorkspaceToDHT(workspace.getGuid());
					//Make sure new Workspace is in the list
					List<DHTWorkspaceEntry> entries = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries.size());
					Assert.assertTrue(entries.contains(entry));

					//Test that removing my Member Node is idempotent
					Assert.assertTrue(!accessor1.removeMyMemberNode(entry));
					entries = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries.size());
					Assert.assertEquals(0, entries.get(0).getMemberNodes().size());
					//Add my Member Node
					Assert.assertTrue(accessor1.addMyMemberNode(entries.get(0), user1Node1));
					entries = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries.size());
					Assert.assertEquals(1, entries.get(0).getMemberNodes().size());
					Assert.assertEquals(user1Node1, entries.get(0).getMemberNodes().get(0));
					//Add my Member Node again shouldn't work
					Assert.assertTrue(!accessor1.addMyMemberNode(entries.get(0), user1Node1));
					entries = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries.size());
					Assert.assertEquals(1, entries.get(0).getMemberNodes().size());
					Assert.assertEquals(user1Node1, entries.get(0).getMemberNodes().get(0));
					//Remove my Member Node
					Assert.assertTrue(accessor1.removeMyMemberNode(entry));
					entries = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries.size());
					Assert.assertEquals(0, entries.get(0).getMemberNodes().size());
					//Add my Member Node again (I think we'll need it)
					Assert.assertTrue(accessor1.addMyMemberNode(entries.get(0), user1Node1));
					entries = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries.size());
					Assert.assertEquals(1, entries.get(0).getMemberNodes().size());
					Assert.assertEquals(user1Node1, entries.get(0).getMemberNodes().get(0));
					//Clean up, and make sure it actually cleans stuff up
					accessor1.leaveWorkspace(entries.get(0));
					entries = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(0, entries.size());
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
	public void testInviteHighLevel() throws NoSuchUserException, CryptographicException, NoSuchAlgorithmException, IOException, WorkspaceAlreadyExistsException, UserAlreadyExistsException, InterruptedException, NoSuchWorkspaceException, MemberAlreadyExistsException {
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
		User user1 = new User(member1.getUserID(), "foo@bar.com", kp1.getPublic());
		User userToInvite = new User("user2-f7zql0bn25", "whatever@blah.com", kp2.getPublic());
		model1.addUser(user1);
		model2.addUser(user1);
		model1.addUser(userToInvite);
		model2.addUser(userToInvite);

		MemberNode user1Node1 = new MemberNode("user1-63HzRNRH7h", "user1-63HzRNRH7h.ms.edu", 1);
		MemberNode user2Node1 = new MemberNode("user2-f7zql0bn25", "uuser2-f7zql0bn25.whatever.gov", 2);

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
				Assert.assertEquals(0, accessor1.fetchMyDHTWorkspaces().size());
				Assert.assertEquals(0, accessor2.fetchMyDHTWorkspaces().size());
				List<DHTWorkspaceEntry> entries1 = null;
				List<DHTWorkspaceEntry> entries2 = null;
				try {
					//Create Workspace
					DHTWorkspaceEntry entry1 = accessor1.addNewWorkspaceToDHT(workspace.getGuid());
					//Member 1 should have a DHTWorkspaceEntry now, and it should have a Node Key
					entries1 = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries1.size());
					Assert.assertEquals(workspace.getGuid(), entries1.get(0).getWorkspaceID());
					Assert.assertNotNull(entries1.get(0).getDhtKey());
					Assert.assertArrayEquals(entry1.getDhtKey().getEncoded(), entries1.get(0).getDhtKey().getEncoded());
					//Add Member 1's Member Node
					Assert.assertTrue(accessor1.addMyMemberNode(entries1.get(0), user1Node1));
					entries1 = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries1.size());
					Assert.assertEquals(1, entries1.get(0).getMemberNodes().size());
					Assert.assertEquals(user1Node1, entries1.get(0).getMemberNodes().get(0));
					//Invite Member2
					accessor1.addInvitation(entries1.get(0), userToInvite);
					entries1 = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries1.get(0).getMemberNodes().size());
					Assert.assertTrue(entries1.get(0).getMemberNodes().contains(user1Node1));
					//Wait for the network
					Thread.sleep(NETWORK_WAIT_SLEEP_TIME);

					//The Invitee should have a DHTWorkspaceEntry now, and it should have a Node Key (which should be the same as the one used to invite)
					entries2 = accessor2.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries2.size());
					Assert.assertEquals(workspace.getGuid(), entries2.get(0).getWorkspaceID());
					Assert.assertNotNull(entries2.get(0).getDhtKey());
					Assert.assertArrayEquals(entries1.get(0).getDhtKey().getEncoded(), entries2.get(0).getDhtKey().getEncoded());
					//Add Member 2's Member Node
					Assert.assertTrue(accessor2.addMyMemberNode(entries2.get(0), user2Node1));
					entries2 = accessor2.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries2.size());
					Assert.assertEquals(2, entries2.get(0).getMemberNodes().size());
					Assert.assertTrue(entries2.get(0).getMemberNodes().contains(user1Node1));
					Assert.assertTrue(entries2.get(0).getMemberNodes().contains(user2Node1));
					//At this point we have enough info to connect to the blockchain and should be able to pull membership info
					model2.addWorkspace(workspace);
					model2.addMemberToWorkspace(workspace.getGuid(), member1);
					model2.addMemberToWorkspace(workspace.getGuid(), memberToInvite);
					//Remove Member 1 from the workspace
					accessor2.removeMemberFromWorkspace(entries2.get(0), member1);
					entries2 = accessor2.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries2.get(0).getMemberNodes().size());
					Assert.assertTrue(!entries2.get(0).getMemberNodes().contains(user1Node1));
					Assert.assertTrue(entries2.get(0).getMemberNodes().contains(user2Node1));
					//Wait for the network
					Thread.sleep(NETWORK_WAIT_SLEEP_TIME);

					//Member 1 should now be booted
					entries1 = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(0, entries1.size());
					//Re-invite Member 1
					accessor2.addInvitation(entries2.get(0), user1);
					entries2 = accessor2.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries2.get(0).getMemberNodes().size());
					Assert.assertTrue(entries2.get(0).getMemberNodes().contains(user2Node1));
					//Wait for the network
					Thread.sleep(NETWORK_WAIT_SLEEP_TIME);

					//Member 1 should be back in the workspace
					entries1 = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries1.size());
					Assert.assertEquals(workspace.getGuid(), entries1.get(0).getWorkspaceID());
					Assert.assertNotNull(entries1.get(0).getDhtKey());
					Assert.assertArrayEquals(entry1.getDhtKey().getEncoded(), entries1.get(0).getDhtKey().getEncoded());
					//Add Member 1's Member Node
					Assert.assertTrue(accessor1.addMyMemberNode(entries1.get(0), user1Node1));
					entries1 = accessor1.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries1.size());
					Assert.assertEquals(2, entries1.get(0).getMemberNodes().size());
					Assert.assertTrue(entries1.get(0).getMemberNodes().contains(user1Node1));
					Assert.assertTrue(entries1.get(0).getMemberNodes().contains(user2Node1));
					//Wait for the network
					Thread.sleep(NETWORK_WAIT_SLEEP_TIME);

					//Member 2 should be able to see Member 1 again
					entries2 = accessor2.fetchMyDHTWorkspaces();
					Assert.assertEquals(2, entries2.get(0).getMemberNodes().size());
					Assert.assertTrue(entries2.get(0).getMemberNodes().contains(user1Node1));
					Assert.assertTrue(entries2.get(0).getMemberNodes().contains(user2Node1));
					//Shut down Member 1
					accessor1.stop();
					entries1 = null;
					//Wait for the network
					Thread.sleep(NETWORK_WAIT_SLEEP_TIME);
					//Member 2 should no longer be able to see Member 1's Node
					entries2 = accessor2.fetchMyDHTWorkspaces();
					Assert.assertEquals(1, entries2.get(0).getMemberNodes().size());
					Assert.assertTrue(!entries2.get(0).getMemberNodes().contains(user1Node1));
					Assert.assertTrue(entries2.get(0).getMemberNodes().contains(user2Node1));

				} finally {
					System.out.println("finally 1");
					//Clean up, and make sure it actually cleans stuff up
					if(entries1 != null && entries1.size() == 1) {
						accessor1.leaveWorkspace(entries1.get(0));
						Assert.assertEquals(0, accessor1.fetchMyDHTWorkspaces().size());
						//Wait for the network
						Thread.sleep(NETWORK_WAIT_SLEEP_TIME);
					}
					if(entries2 != null && entries2.size() == 1) {
						accessor2.leaveWorkspace(entries2.get(0));
						Assert.assertEquals(0, accessor2.fetchMyDHTWorkspaces().size());
					}
				}
			} finally {
				System.out.println("finally 2");
				accessor1.stop();
				accessor2.stop();
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
					SecretKey nodeKey = accessor1.addNewWorkspaceToDHTInternal(workspace.getGuid());
					workspaceNodeKeys.put(workspace.getGuid(), nodeKey);
					//Add somebody else's Member Node - should fail
					try {
						accessor1.addMyMemberNode(workspace.getGuid(), user2Node1, nodeKey);
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
