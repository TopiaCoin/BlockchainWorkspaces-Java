package io.topiacoin.workspace.blockchain;

import io.topiacoin.chainmail.multichainstuff.exception.ChainAlreadyExistsException;

import java.io.IOException;

public interface Chainmail {

	public void start() throws IOException;

	public void stop();

	public void createBlockchain(String workspaceID) throws ChainAlreadyExistsException;

	public void startBlockchain(String workspaceID) throws IOException;

	public boolean stopBlockchain(String workspaceID) throws IOException;

	public void addBlockchainListener(ChainmailCallback callback);
}
