package io.topiacoin.workspace.blockchain.chainmailImpl;

import io.topiacoin.workspace.blockchain.Chainmail;
import io.topiacoin.workspace.blockchain.ChainmailTest;

public class EOSChainmailTest extends ChainmailTest {

	@Override public Chainmail getChainmailInstance(int portstart, int portend) {
		return new EOSChainmail(portstart, portend);
	}

	@Override public Chainmail getChainmailInstance() {
		return new EOSChainmail();
	}
}
