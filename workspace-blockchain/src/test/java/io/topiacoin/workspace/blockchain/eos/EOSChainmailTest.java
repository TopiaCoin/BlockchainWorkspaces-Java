package io.topiacoin.workspace.blockchain.eos;

import io.topiacoin.workspace.blockchain.Chainmail;
import io.topiacoin.workspace.blockchain.ChainmailTest;

public class EOSChainmailTest extends ChainmailTest {

	@Override public Chainmail getChainmailInstance(int portstart, int portend) {
		return new EOSChainmail(portstart, portend);
	}

	@Override public Chainmail getChainmailInstance() {
		return new EOSChainmail();
	}

	public void updateRPCLastModified(EOSAdapter eosa) {
		eosa.updateLastBlockTime(System.currentTimeMillis());
	}
}
