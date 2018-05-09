package io.topiacoin.dht;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.UserNode;
import io.topiacoin.model.exceptions.NoSuchUserException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class SDFSDHTAccessorTest {

	@Test
	public void testUserNodeCRUD() throws CryptographicException, NoSuchUserException, NoSuchAlgorithmException, IOException {
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
		UserNode user1Node1 = new UserNode("user1-63HzRNRH7h", "user1-63HzRNRH7h.ms.edu", 1, "user1-63HzRNRH7h".getBytes());
		UserNode user1Node2 = new UserNode(user1Node1.getUserID(), "user1-63HzRNRH7h.azure.com", 2, "user1-63HzRNRH7h2".getBytes());

		File configDir = new File(dhtconfig.getNodeDataFolder());
		File routingTable = new File(configDir, "routingTable");
		File hashTable = new File(configDir, "hashTable");
		try {
			Assert.assertTrue(!routingTable.exists());
			Assert.assertTrue(!hashTable.exists());
			//Build the accessors and make sure the base condition (empty dht) is true
			//WARNING
			//WARNING
			//WARNING
			//This is an integration test. If the base condition isn't met, it's possible an actual user has actually claimed these
			//keys, so don't just go around blowing stuff up
			//That said, I chose random noisey keys so that hopefully that'll never happen.
			SDFSDHTAccessor accessor1 = new SDFSDHTAccessor(config, model1);
			try {
				System.out.println("Ding 1");
				Assert.assertEquals(0, accessor1.listUserNodes(user1Node1.getUserID()).size());
				Assert.assertEquals(0, accessor1.listUserNodes(user1Node2.getUserID()).size());

				//Verify that removing a non-existant node doesn't cause cataclysm
				//I don't think there's anything to assert here - just make sure it doesn't throw out.
				accessor1.removeUserNode(user1Node1);
				System.out.println("Ding 2");
				List<UserNode> accessor1Nodes;

				try {
					//Add a node via accessor1 and list for both
					accessor1.submitUserNode(user1Node1);
					accessor1Nodes = accessor1.listUserNodes(user1Node1.getUserID());
					Assert.assertEquals(1, accessor1Nodes.size());
					Assert.assertTrue(accessor1Nodes.contains(user1Node1));
					System.out.println("Ding 3");
					//Add a node via accessor2 and list for both
					System.out.println("Ding 4");
					//0: Add another node for each user, and list
					//1: Add the same node again and verify that nothing changes
					for (int i = 0; i < 2; i++) {
						System.out.println("Ding " + (5 + i));
						accessor1.submitUserNode(user1Node2);
						accessor1Nodes = accessor1.listUserNodes(user1Node1.getUserID());
						Assert.assertEquals(2, accessor1Nodes.size());
						Assert.assertTrue(accessor1Nodes.contains(user1Node1));
						Assert.assertTrue(accessor1Nodes.contains(user1Node2));
					}
					System.out.println("Ding 7");
					//Clean up
					accessor1.removeUserNode(user1Node1);
					accessor1Nodes = accessor1.listUserNodes(user1Node1.getUserID());
					Assert.assertEquals(1, accessor1Nodes.size());
					accessor1.removeUserNode(user1Node2);
					accessor1Nodes = accessor1.listUserNodes(user1Node1.getUserID());
					Assert.assertEquals(0, accessor1Nodes.size());
				} finally {
					System.out.println("finally 1");
					//Clean up
					//Hmm, I don't know what happens when I assert in a finally block, but I'm gonna do it anyway
					accessor1.removeUserNode(user1Node1);
					accessor1.removeUserNode(user1Node2);
					Assert.assertEquals(0, accessor1.listUserNodes(user1Node1.getUserID()).size());
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
	public void testMultiUserNodeCRUD() throws CryptographicException, NoSuchUserException, NoSuchAlgorithmException, IOException, InterruptedException {
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
		model2.setCurrentUser(new CurrentUser("user2-j34k7PyRTm", "user2-j34k7PyRTm@apple.com", kp2.getPublic(), kp2.getPrivate()));
		UserNode user1Node1 = new UserNode("user1-63HzRNRH7h", "user1-63HzRNRH7h.ms.edu", 1, "user1-63HzRNRH7h".getBytes());
		UserNode user1Node2 = new UserNode(user1Node1.getUserID(), "user1-63HzRNRH7h.azure.com", 2, "user1-63HzRNRH7h2".getBytes());
		UserNode user2Node1 = new UserNode("user2-j34k7PyRTm", "user2-j34k7PyRTm.mac.biz", 3, "user2-j34k7PyRTm".getBytes());
		UserNode user2Node2 = new UserNode("user2-j34k7PyRTm", "user2-j34k7PyRTm.icloud.blah", 4, "user2-j34k7PyRTm2".getBytes());

		File configDir = new File(dhtconfig.getNodeDataFolder());
		File routingTable = new File(configDir, "routingTable");
		File hashTable = new File(configDir, "hashTable");
		try {
			Assert.assertTrue(!routingTable.exists());
			Assert.assertTrue(!hashTable.exists());
			//Build the accessors and make sure the base condition (empty dht) is true
			//WARNING
			//WARNING
			//WARNING
			//This is an integration test. If the base condition isn't met, it's possible an actual user has actually claimed these
			//keys, so don't just go around blowing stuff up
			//That said, I chose random noisey keys so that hopefully that'll never happen.
			SDFSDHTAccessor accessor1 = new SDFSDHTAccessor(config, model1);
			SDFSDHTAccessor accessor2 = new SDFSDHTAccessor(config, model2);
			try {
				System.out.println("Ding 1");
				Assert.assertEquals(0, accessor1.listUserNodes(user1Node1.getUserID()).size());
				Assert.assertEquals(0, accessor1.listUserNodes(user2Node1.getUserID()).size());
				Assert.assertEquals(0, accessor2.listUserNodes(user1Node1.getUserID()).size());
				Assert.assertEquals(0, accessor2.listUserNodes(user2Node1.getUserID()).size());

				//Verify that removing a non-existant node doesn't cause cataclysm
				//I don't think there's anything to assert here - just make sure it doesn't throw out.
				accessor1.removeUserNode(user1Node1);
				System.out.println("Ding 2");
				List<UserNode> accessor1Nodes;
				List<UserNode> accessor2Nodes;

				try {
					//Add a node via accessor1 and list for both
					accessor1.submitUserNode(user1Node1);
					accessor1Nodes = accessor1.listUserNodes(user1Node1.getUserID());
					accessor2Nodes = accessor2.listUserNodes(user1Node1.getUserID());
					Assert.assertEquals(1, accessor1Nodes.size());
					Assert.assertEquals(1, accessor2Nodes.size());
					Assert.assertTrue(accessor1Nodes.contains(user1Node1));
					Assert.assertTrue(accessor2Nodes.contains(user1Node1));
					System.out.println("Ding 3");
					//Add a node via accessor2 and list for both
					accessor2.submitUserNode(user2Node1);
					accessor1Nodes = accessor1.listUserNodes(user2Node1.getUserID());
					accessor2Nodes = accessor2.listUserNodes(user2Node1.getUserID());
					Assert.assertEquals(1, accessor1Nodes.size());
					Assert.assertEquals(1, accessor2Nodes.size());
					Assert.assertTrue(accessor1Nodes.contains(user2Node1));
					Assert.assertTrue(accessor2Nodes.contains(user2Node1));
					System.out.println("Ding 4");
					//0: Add another node for each user, and list
					//1: Add the same node again and verify that nothing changes
					for (int i = 0; i < 2; i++) {
						System.out.println("Ding " + (5 + i));
						accessor1.submitUserNode(user1Node2);
						Thread.sleep(1000); //Wait for the network
						accessor1Nodes = accessor1.listUserNodes(user1Node1.getUserID());
						accessor2Nodes = accessor2.listUserNodes(user1Node1.getUserID());
						Assert.assertEquals(2, accessor1Nodes.size());
						Assert.assertEquals(2, accessor2Nodes.size());
						Assert.assertTrue(accessor1Nodes.contains(user1Node1));
						Assert.assertTrue(accessor2Nodes.contains(user1Node1));
						Assert.assertTrue(accessor1Nodes.contains(user1Node2));
						Assert.assertTrue(accessor2Nodes.contains(user1Node2));

						accessor2.submitUserNode(user2Node2);
						Thread.sleep(1000); //Wait for the network
						accessor1Nodes = accessor1.listUserNodes(user2Node1.getUserID());
						accessor2Nodes = accessor2.listUserNodes(user2Node1.getUserID());
						Assert.assertEquals(2, accessor1Nodes.size());
						Assert.assertEquals(2, accessor2Nodes.size());
						Assert.assertTrue(accessor1Nodes.contains(user2Node2));
						Assert.assertTrue(accessor2Nodes.contains(user2Node2));
					}
					System.out.println("Ding 7");
					//Clean up
					accessor1.removeUserNode(user1Node1);
					Thread.sleep(1000); //Wait for the network
					accessor1Nodes = accessor1.listUserNodes(user1Node1.getUserID());
					accessor2Nodes = accessor2.listUserNodes(user1Node1.getUserID());
					Assert.assertEquals(1, accessor1Nodes.size());
					Assert.assertEquals(1, accessor2Nodes.size());
					accessor2.removeUserNode(user2Node1);
					Thread.sleep(1000); //Wait for the network
					accessor1Nodes = accessor1.listUserNodes(user2Node1.getUserID());
					accessor2Nodes = accessor2.listUserNodes(user2Node1.getUserID());
					Assert.assertEquals(1, accessor1Nodes.size());
					Assert.assertEquals(1, accessor2Nodes.size());
					accessor1.removeUserNode(user1Node2);
					Thread.sleep(1000); //Wait for the network
					accessor1Nodes = accessor1.listUserNodes(user1Node1.getUserID());
					accessor2Nodes = accessor2.listUserNodes(user1Node1.getUserID());
					Assert.assertEquals(0, accessor1Nodes.size());
					Assert.assertEquals(0, accessor2Nodes.size());
					accessor2.removeUserNode(user2Node2);
					Thread.sleep(1000); //Wait for the network
					accessor1Nodes = accessor1.listUserNodes(user2Node1.getUserID());
					accessor2Nodes = accessor2.listUserNodes(user2Node1.getUserID());
					Assert.assertEquals(0, accessor1Nodes.size());
					Assert.assertEquals(0, accessor2Nodes.size());
				} finally {
					System.out.println("finally 1");
					//Clean up
					//Hmm, I don't know what happens when I assert in a finally block, but I'm gonna do it anyway
					accessor1.removeUserNode(user1Node1);
					accessor2.removeUserNode(user2Node1);
					accessor1.removeUserNode(user1Node2);
					accessor2.removeUserNode(user2Node2);
					Thread.sleep(1000); //Wait for the network
					Assert.assertEquals(0, accessor1.listUserNodes(user1Node1.getUserID()).size());
					Assert.assertEquals(0, accessor1.listUserNodes(user2Node1.getUserID()).size());
					Assert.assertEquals(0, accessor2.listUserNodes(user1Node1.getUserID()).size());
					Assert.assertEquals(0, accessor2.listUserNodes(user2Node1.getUserID()).size());
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
}
