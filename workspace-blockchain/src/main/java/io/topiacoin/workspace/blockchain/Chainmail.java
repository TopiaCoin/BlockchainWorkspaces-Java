package io.topiacoin.workspace.blockchain;

import io.topiacoin.chainmail.multichainstuff.exception.ChainAlreadyExistsException;
import io.topiacoin.chainmail.multichainstuff.exception.NoSuchChainException;

import java.io.IOException;

public interface Chainmail {

	public void start(RPCAdapterManager manager) throws IOException;

	public void stop();

	public void createBlockchain(String workspaceID) throws ChainAlreadyExistsException;

	public void startBlockchain(String workspaceID) throws IOException, NoSuchChainException;

	public boolean stopBlockchain(String workspaceID) throws IOException;

	public void addBlockchainListener(ChainmailCallback callback);

	void destroyBlockchain(String workspaceID) throws IOException;
}
