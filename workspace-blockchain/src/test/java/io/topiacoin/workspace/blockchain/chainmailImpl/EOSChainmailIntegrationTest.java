package io.topiacoin.workspace.blockchain.chainmailImpl;

import io.topiacoin.chainmail.multichainstuff.exception.ChainAlreadyExistsException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static junit.framework.TestCase.*;

public class EOSChainmailIntegrationTest {

	@Test
	public void testToSeeIfItWorks() throws Exception {
		EOSChainmail chainmail = new EOSChainmail();
		chainmail.start();
		String workspaceID = UUID.randomUUID().toString();
		chainmail.createBlockchain(workspaceID);
		chainmail.startBlockchain(workspaceID);
		//This is the part where we do whatever it is we do with a running blockchain.
		chainmail.stopBlockchain(workspaceID);
		//Thread.sleep(2000);
		chainmail.destroyBlockchain(workspaceID);
		chainmail.stop();
	}

	@Test
	public void testIllegalPortsFails() throws IOException {
		try {
			new EOSChainmail(9240, 9241).start();
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
		try {
			new EOSChainmail(9241, 9241).start();
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
		try {
			new EOSChainmail(9242, 9241).start();
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
		try {
			new EOSChainmail(-1, 9241).start();
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
		try {
			new EOSChainmail(0, 9241).start();
			fail();
		} catch(IllegalArgumentException ex) {
			//Good
		}
	}

	@Test
	public void testWhatHappensWhenThereArentEnoughPorts() throws IOException, ChainAlreadyExistsException {
		EOSChainmail chainmail = new EOSChainmail(9240, 9242);
		chainmail.start();
		String workspaceID = UUID.randomUUID().toString();
		String workspaceID2 = UUID.randomUUID().toString();
		try {
			chainmail.createBlockchain(workspaceID);
			chainmail.startBlockchain(workspaceID);
			chainmail.createBlockchain(workspaceID2);
			chainmail.startBlockchain(workspaceID2);
			Assert.assertFalse(chainmail.stopBlockchain(workspaceID));
			Assert.assertTrue(chainmail.stopBlockchain(workspaceID2));
		} finally {
			chainmail.destroyBlockchain(workspaceID);
			chainmail.destroyBlockchain(workspaceID2);
			chainmail.stop();
		}
	}

	@Test
	public void makeSureLRUWorks() throws IOException, ChainAlreadyExistsException {
		EOSChainmail chainmail = new EOSChainmail(9240, 9244);
		chainmail.start();
		String workspaceID = UUID.randomUUID().toString();
		String workspaceID2 = UUID.randomUUID().toString();
		String workspaceID3 = UUID.randomUUID().toString();
		try {
			chainmail.createBlockchain(workspaceID);
			chainmail.startBlockchain(workspaceID);
			chainmail.createBlockchain(workspaceID2);
			chainmail.startBlockchain(workspaceID2);
			chainmail.chainInfo.get(workspaceID).rpcAdapter.updateLastModified();
			chainmail.createBlockchain(workspaceID3);
			chainmail.startBlockchain(workspaceID3);
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
}
