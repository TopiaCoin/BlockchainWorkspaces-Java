package io.topiacoin.sdk;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.crypto.HashUtils;
import io.topiacoin.dht.DHT;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.sdk.impl.DHTEventsAPI;
import io.topiacoin.util.Notification;
import io.topiacoin.util.NotificationCenter;
import io.topiacoin.util.NotificationHandler;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DHTEventsAPITest {

	@Test
	public void happyPath() throws CryptographicException, NoSuchAlgorithmException, IOException, NoSuchUserException, InterruptedException {
		Configuration config = new DefaultConfiguration();
		DHTConfiguration dhtconfig = new DHTConfiguration(config);
		dhtconfig.setC1(4);
		dhtconfig.setC2(8);
		KeyPair DHTKeyPair = CryptoUtils.generateECKeyPair();
		DHT fauxBootstrapNode = new DHT(0, DHTKeyPair, dhtconfig);
		fauxBootstrapNode.start(false);
		dhtconfig.setBootstrapNodeHostname(fauxBootstrapNode.getNode().getAddress().getHostName());
		dhtconfig.setBootstrapNodeID(fauxBootstrapNode.getNode().getNodeID().getNodeID());
		dhtconfig.setBootstrapNodePort(fauxBootstrapNode.getNode().getPort());

		DataModel model = DataModel.getInstance();
		KeyPair kp1 = CryptoUtils.generateECKeyPair();
		CurrentUser user = new CurrentUser("user1-63HzRNRH7h1", "user1-63HzRNRH71h@hotmail.com", kp1.getPublic(), kp1.getPrivate());
		model.setCurrentUser(user);

		Map<Long, SecretKey> testWorkspaces = new HashMap<>();
		testWorkspaces.put(123L, CryptoUtils.generateAESKey());
		testWorkspaces.put(456L, CryptoUtils.generateAESKey());
		testWorkspaces.put(789L, CryptoUtils.generateAESKey());
		Set<Long> notificationsIveReceived = new HashSet<>();
		final CountDownLatch latch = new CountDownLatch(testWorkspaces.keySet().size());
		final Object lock = new Object();
		NotificationCenter center = NotificationCenter.defaultCenter();
		center.addHandler(new NotificationHandler() {
			@Override public void handleNotification(Notification notification) {
				synchronized (lock) {
					Assert.assertTrue(testWorkspaces.keySet().contains(Long.parseLong(notification.getClassifier())));
					Assert.assertTrue(notificationsIveReceived.add(Long.parseLong(notification.getClassifier())));
					latch.countDown();
				}
			}
		}, "newWorkspace", null);
		DHTEventsAPI api = new DHTEventsAPI();
		api.startEventFetching(config, model);
		Assert.assertTrue(api.isRunning());

		//Add workspaces for the user - make sure the Notifications fire. This is weirdly more of an emulation of the createWorkspace call,
		//but it'll have to do.
		synchronized (lock) {
			for (long workspaceID : testWorkspaces.keySet()) {
				SecretKey workspaceNodeKey = testWorkspaces.get(workspaceID);
				//Store the nodeKey for the new user
				String pkString = Base64.getEncoder().encodeToString(user.getPublicKey().getEncoded());
				String dhtKey = HashUtils.sha256String(workspaceID + pkString);
				String dhtValue = Base64.getEncoder().encodeToString(CryptoUtils.encryptWithPublicKey(workspaceNodeKey.getEncoded(), user.getPublicKey()));
				fauxBootstrapNode.storeContent(dhtKey, dhtValue);
				//Store the workspaceID for the user
				dhtKey = HashUtils.sha256String(user.getUserID());
				dhtValue = CryptoUtils.encryptWithPublicKeyToString("" + workspaceID, user.getPublicKey());
				String hash = HashUtils.sha256String(HashUtils.sha256String("" + workspaceID));
				fauxBootstrapNode.storeContent(dhtKey, hash + "\n" + dhtValue);
			}
		}
		Assert.assertTrue("Notifications never received", latch.await(10, TimeUnit.SECONDS));
		Assert.assertEquals(testWorkspaces.keySet().size(), notificationsIveReceived.size());
		Assert.assertTrue(notificationsIveReceived.containsAll(testWorkspaces.keySet()));
		final CountDownLatch latch2 = new CountDownLatch(testWorkspaces.keySet().size());
		center.addHandler(new NotificationHandler() {
			@Override public void handleNotification(Notification notification) {
				synchronized (lock) {
					Assert.assertTrue(testWorkspaces.keySet().contains(Long.parseLong(notification.getClassifier())));
					Assert.assertTrue(notificationsIveReceived.remove(Long.parseLong(notification.getClassifier())));
					latch2.countDown();
				}
			}
		}, "removedFromWorkspace", null);
		//remove workspaces for the user - make sure the Notifications fire.
		synchronized (lock) {
			for (long workspaceID : testWorkspaces.keySet()) {
				SecretKey workspaceNodeKey = testWorkspaces.get(workspaceID);
				//Fetch the User's workspaceIDs and remove this workspace from the list
				String dhtKey = HashUtils.sha256String(user.getUserID());
				String hash = HashUtils.sha256String(HashUtils.sha256String("" + workspaceID));
				Set<String> values = fauxBootstrapNode.fetchContent(dhtKey);
				for (String value : values) {
					String[] split = value.split("\n");
					if (split.length == 2) {
						if (split[0].equals(hash)) {
							fauxBootstrapNode.removeContent(dhtKey, value);
						}
					} else {
						fauxBootstrapNode.removeContent(dhtKey, value);
					}
				}
				//Fetch the user's Node Key (or keys - but that would imply bigger issues...) and remove it
				String pkString = Base64.getEncoder().encodeToString(user.getPublicKey().getEncoded());
				dhtKey = HashUtils.sha256String(workspaceID + pkString);
				Set<String> encWksNodeKeyStrs = fauxBootstrapNode.fetchContent(dhtKey);
				for (String keyStr : encWksNodeKeyStrs) {
					fauxBootstrapNode.removeContent(dhtKey, keyStr);
				}
				//Fetch the workspace's nodes, and remove this user's nodes from the list
				dhtKey = HashUtils.sha256String("" + workspaceID);
				hash = HashUtils.sha256String(HashUtils.sha256String(user.getUserID()));
				values = fauxBootstrapNode.fetchContent(dhtKey);
				for (String value : values) {
					String[] split = value.split("\n");
					if (split.length == 2) {
						if (split[0].equals(hash)) {
							fauxBootstrapNode.removeContent(dhtKey, value);
						}
					} else {
						fauxBootstrapNode.removeContent(dhtKey, value);
					}
				}
			}
		}
		Assert.assertTrue("Notifications never received", latch2.await(10, TimeUnit.SECONDS));
		Assert.assertEquals(0, notificationsIveReceived.size());
	}
}
