package io.topiacoin.workspace.blockchain.eos;

import io.topiacoin.workspace.blockchain.Chainmail;
import io.topiacoin.workspace.blockchain.ChainmailTest;
import org.junit.Ignore;

@Ignore
public class EOSDockerChainmailTest extends ChainmailTest {

	@Override public Chainmail getChainmailInstance(int portstart, int portend) {
		return new EOSDockerChainmail(portstart, portend);
	}

	@Override public Chainmail getChainmailInstance() {
		return new EOSDockerChainmail();
	}

	public void updateRPCLastModified(EOSAdapter eosa) {
		eosa.updateLastBlockTime(System.currentTimeMillis());
	}
}
