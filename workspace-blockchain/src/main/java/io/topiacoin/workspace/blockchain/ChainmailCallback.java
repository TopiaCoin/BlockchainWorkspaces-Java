package io.topiacoin.workspace.blockchain;


public interface ChainmailCallback {

	public void onBlockchainStarted(String workspaceId, String nodeURL, String walletURL);

	public void onBlockchainStopped(String workspaceId);
}
