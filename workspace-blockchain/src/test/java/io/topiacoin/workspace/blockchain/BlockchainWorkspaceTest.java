package io.topiacoin.workspace.blockchain;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.callbacks.CreateWorkspaceCallback;
import io.topiacoin.core.exceptions.NotLoggedInException;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.dht.DHT;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Ignore
public class BlockchainWorkspaceTest {


	@Test
	public void theGreatestIntegrationTestTheWorldHasEverSeen() throws NotLoggedInException, CryptographicException, NoSuchAlgorithmException, IOException {
		File blockchainStorageDir = new File("C:\\Users\\csandwith\\AppData\\Roaming\\EOSTestChains");
		new File(new File(blockchainStorageDir, "wallet"), "sdfstmp.wallet").deleteOnExit();
		Configuration config = new DefaultConfiguration();
		config.setConfigurationOption("nodeos_install", "/mnt/c/EOS/eos/build/programs/nodeos/nodeos");
		config.setConfigurationOption("keos_install", "/mnt/c/EOS/eos/build/programs/keosd/keosd");
		config.setConfigurationOption("cleos_install", "/mnt/c/EOS/eos/build/programs/cleos/cleos");
		config.setConfigurationOption("blockchain_storage_dir", "C:\\Users\\csandwith\\AppData\\Roaming\\EOSTestChains");
		config.setConfigurationOption("blockchain_storage_dir_linux", "/mnt/c/Users/csandwith/AppData/Roaming/EOSTestChains");
		config.setConfigurationOption("smart_contract_dir_linux", "/mnt/c/Users/csandwith/AppData/Roaming/EOSTestChains/secrataContainer");
		config.setConfigurationOption("chainmail_port_range_start", "9240");
		config.setConfigurationOption("chainmail_port_range_end", "9250");

		DHTConfiguration dhtconfig = new DHTConfiguration(config);
		dhtconfig.setRestoreInterval(60 * 1000);
		dhtconfig.setResponseTimeout(2000);
		dhtconfig.setOperationTimeout(2000);
		dhtconfig.setMaxConcurrentMessages(10);
		dhtconfig.setC1(4);
		dhtconfig.setC2(8);
		dhtconfig.setK(20);
		dhtconfig.setStaleLimit(1);
		dhtconfig.setNodeDataFolder("kademlia");
		dhtconfig.setEntryExpirationTime(86400000);
		KeyPair DHTKeyPair = CryptoUtils.generateECKeyPair();
		DHT fauxBootstrapNode = new DHT(0, DHTKeyPair, dhtconfig);
		fauxBootstrapNode.start(false);
		dhtconfig.setBootstrapNodeHostname(fauxBootstrapNode.getNode().getAddress().getHostName());
		dhtconfig.setBootstrapNodeID(fauxBootstrapNode.getNode().getNodeID().getNodeID());
		dhtconfig.setBootstrapNodePort(fauxBootstrapNode.getNode().getPort());

		BlockchainWorkspace workspace = new BlockchainWorkspace(config);
		DataModel dataModel = DataModel.getInstance();
		KeyPair myKeys = CryptoUtils.generateECKeyPair();
		CurrentUser me = new CurrentUser("usera", "foo@bar.com", myKeys.getPublic(), myKeys.getPrivate());
		try {
			dataModel.setCurrentUser(me);
			final Map<String, Boolean> success = new HashMap<String, Boolean>();
			while(success.get("success") == null) {
				workspace.createWorkspace("A Workspace", "A description", new CreateWorkspaceCallback() {
					@Override public void createdWorkspace(long workspaceID) {
						System.out.println("Nifty!");
						success.put("success", true);
					}

					@Override public void failedToCreateWorkspace() {
						System.out.println("Failed");
						success.put("success", false);
					}
				});
				Thread.sleep(100);
			}
			Assert.assertTrue(success.get("success"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			workspace.stop();
			fauxBootstrapNode.shutdown(false);
		}
	}

}
