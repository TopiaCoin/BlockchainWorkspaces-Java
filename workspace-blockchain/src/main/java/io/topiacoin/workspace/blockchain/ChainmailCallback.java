package io.topiacoin.workspace.blockchain;


public interface ChainmailCallback {

	public void onBlockchainStarted(long workspaceId, String nodeURL, String walletURL);

	public void onBlockchainStopped(long workspaceId);
}
