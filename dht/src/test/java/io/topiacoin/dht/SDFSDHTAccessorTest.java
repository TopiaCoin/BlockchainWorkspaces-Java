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
import io.topiacoin.model.UserNode;
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
					nodeKey = accessor1.addMyMemberNode(workspace.getGuid(), user1Node1, members);
					workspaceNodeKeys.put(workspace.getGuid(), nodeKey);
					List<MemberNode> memberNodes = accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid()));
					workspaceNodeKeys.put(workspace.getGuid(), nodeKey);
					Assert.assertEquals(1, memberNodes.size());
					Assert.assertEquals(user1Node1, memberNodes.get(0));
					//Remove my Member Node
					removed = accessor1.removeMyMemberNode(workspace.getGuid(), user1Node1, workspaceNodeKeys.get(workspace.getGuid()));
					Assert.assertTrue(removed);
					Assert.assertEquals(0, accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid())).size());
					//Add my Member Node again (I think we'll need it)
					nodeKey = accessor1.addMyMemberNode(workspace.getGuid(), user1Node1, members);
					workspaceNodeKeys.put(workspace.getGuid(), nodeKey);
					memberNodes = accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid()));
					Assert.assertEquals(1, memberNodes.size());
					Assert.assertEquals(user1Node1, memberNodes.get(0));
					//I should have a workspaceID now.
					List<String> workspaceIDs = accessor1.fetchMyWorkspaceIDs();
					Assert.assertEquals(1, workspaceIDs.size());
					Assert.assertEquals(workspace.getGuid(), workspaceIDs.get(0));
					//I should be able to fetch the Workspace Node Key, which should contain the one I stored in the map
					Map<String, SecretKey> keys = accessor1.fetchWorkspaceNodeKeys(workspaceIDs);
					Assert.assertEquals(1, keys.size());
					Assert.assertArrayEquals(workspaceNodeKeys.get(workspace.getGuid()).getEncoded(), keys.get(workspace.getGuid()).getEncoded());
					//Clean up, and make sure it actually cleans stuff up
					accessor1.leaveWorkspace(workspace.getGuid());
					Assert.assertEquals(0, accessor1.fetchMyWorkspaceIDs().size());
					Assert.assertEquals(0, accessor1.fetchMemberNodes(workspace.getGuid(), workspaceNodeKeys.get(workspace.getGuid())).size());
					Assert.assertEquals(0, accessor1.fetchWorkspaceNodeKeys(workspaceIDs).size());
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
