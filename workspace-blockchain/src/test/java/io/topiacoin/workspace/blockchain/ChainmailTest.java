package io.topiacoin.workspace.blockchain;

import io.topiacoin.workspace.blockchain.eos.EOSAdapter;
import io.topiacoin.workspace.blockchain.exceptions.ChainAlreadyExistsException;
import io.topiacoin.workspace.blockchain.exceptions.NoSuchChainException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.fail;

public abstract class ChainmailTest {

	public abstract Chainmail getChainmailInstance(int portstart, int portend);

	public abstract Chainmail getChainmailInstance();

	public abstract void updateRPCLastModified(EOSAdapter adapter);

	@Test
	public void testToSeeIfItWorks() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		ChainmailCallback callback = new ChainmailCallback() {
			@Override public void onBlockchainStarted(long workspaceId, String nodeURL, String walletURL) {
				latch.countDown();
			}

			@Override public void onBlockchainStopped(long workspaceId) {
				latch.countDown();
			}
		};
		Chainmail chainmail = getChainmailInstance();
		RPCAdapterManager manager = new RPCAdapterManager(chainmail);
		chainmail.start(manager);
		chainmail.addBlockchainListener(callback);
		long workspaceID = new Random().nextLong();
		chainmail.createBlockchain("usera", workspaceID);
		chainmail.startBlockchain("usera", workspaceID, null);
		//This is the part where we do whatever it is we do with a running blockchain.
		chainmail.stopBlockchain(workspaceID);
		chainmail.destroyBlockchain(workspaceID);
		chainmail.stop();
		Assert.assertEquals(0, latch.getCount());
	}

	@Test
	public void testIllegalPortsFails() throws IOException {
		try {
			Chainmail chainmail = getChainmailInstance(9240, 9241);
			RPCAdapterManager manager = new RPCAdapterManager(chainmail);
			chainmail.start(manager);
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
		try {
			Chainmail chainmail = getChainmailInstance(9241, 9241);
			RPCAdapterManager manager = new RPCAdapterManager(chainmail);
			chainmail.start(manager);
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
		try {
			Chainmail chainmail = getChainmailInstance(9242, 9241);
			RPCAdapterManager manager = new RPCAdapterManager(chainmail);
			chainmail.start(manager);
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
		try {
			Chainmail chainmail = getChainmailInstance(-1, 9241);
			RPCAdapterManager manager = new RPCAdapterManager(chainmail);
			chainmail.start(manager);
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
		try {
			Chainmail chainmail = getChainmailInstance(0, 9241);
			RPCAdapterManager manager = new RPCAdapterManager(chainmail);
			chainmail.start(manager);
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
	}

	@Test
	public void testWhatHappensWhenThereArentEnoughPorts() throws IOException, ChainAlreadyExistsException, NoSuchChainException {
		Chainmail chainmail = getChainmailInstance(9240, 9242);
		RPCAdapterManager manager = new RPCAdapterManager(chainmail);
		chainmail.start(manager);
		long workspaceID = new Random().nextLong();
		long workspaceID2 = new Random().nextLong();
		try {
			chainmail.createBlockchain("usera", workspaceID);
			chainmail.startBlockchain("usera", workspaceID, null);
			chainmail.createBlockchain("usera", workspaceID2);
			chainmail.startBlockchain("usera", workspaceID2, null);
			Assert.assertFalse(chainmail.stopBlockchain(workspaceID));
			Assert.assertTrue(chainmail.stopBlockchain(workspaceID2));
		} finally {
			chainmail.destroyBlockchain(workspaceID);
			chainmail.destroyBlockchain(workspaceID2);
			chainmail.stop();
		}
	}

	@Test
	public void makeSureLRUWorks() throws IOException, ChainAlreadyExistsException, InterruptedException, NoSuchChainException {
		Chainmail chainmail = getChainmailInstance(9240, 9244);
		RPCAdapterManager manager = new RPCAdapterManager(chainmail);
		chainmail.start(manager);
		long workspaceID = new Random().nextLong();
		long workspaceID2 = new Random().nextLong();
		long workspaceID3 = new Random().nextLong();
		try {
			chainmail.createBlockchain("usera", workspaceID);
			chainmail.startBlockchain("usera", workspaceID, null);
			updateRPCLastModified(manager.getRPCAdapter(workspaceID));
			Thread.sleep(1);
			chainmail.createBlockchain("usera", workspaceID2);
			chainmail.startBlockchain("usera", workspaceID2, null);
			updateRPCLastModified(manager.getRPCAdapter(workspaceID2));
			Thread.sleep(1);
			updateRPCLastModified(manager.getRPCAdapter(workspaceID));
			Thread.sleep(1);
			chainmail.createBlockchain("usera", workspaceID3);
			chainmail.startBlockchain("usera", workspaceID3, null);
			updateRPCLastModified(manager.getRPCAdapter(workspaceID3));
			Assert.assertFalse(chainmail.stopBlockchain(workspaceID2)); //Because it should've already been stopped
			Assert.assertTrue(chainmail.stopBlockchain(workspaceID));
			Assert.assertTrue(chainmail.stopBlockchain(workspaceID3));
		} finally {
			chainmail.destroyBlockchain(workspaceID);
			chainmail.destroyBlockchain(workspaceID2);
			chainmail.destroyBlockchain(workspaceID3);
			chainmail.stop();
		}
	}

	@Test
	public void makeSureBlockchainsCantBeDoubleStarted() throws IOException, ChainAlreadyExistsException, InterruptedException, NoSuchChainException {
		Chainmail chainmail = getChainmailInstance(9240, 9244);
		RPCAdapterManager manager = new RPCAdapterManager(chainmail);
		chainmail.start(manager);
		long workspaceID = new Random().nextLong();
		long workspaceID2 = new Random().nextLong();
		try {
			chainmail.createBlockchain("usera", workspaceID);
			chainmail.startBlockchain("usera", workspaceID, null);
			updateRPCLastModified(manager.getRPCAdapter(workspaceID));
			Thread.sleep(1);
			chainmail.createBlockchain("usera", workspaceID2);
			chainmail.startBlockchain("usera", workspaceID2, null);
			updateRPCLastModified(manager.getRPCAdapter(workspaceID2));
			Thread.sleep(1);
			updateRPCLastModified(manager.getRPCAdapter(workspaceID));
			Thread.sleep(1);
			//Ok, so since workspaceID2 has the older datestamp on it, if I start workspaceID again and it actually starts, LRU will kill workspaceID2.
			chainmail.startBlockchain("usera", workspaceID, null);
			Assert.assertTrue(chainmail.stopBlockchain(workspaceID));
			Assert.assertTrue(chainmail.stopBlockchain(workspaceID2));
		} finally {
			chainmail.destroyBlockchain(workspaceID);
			chainmail.destroyBlockchain(workspaceID2);
			chainmail.stop();
		}
	}
}
