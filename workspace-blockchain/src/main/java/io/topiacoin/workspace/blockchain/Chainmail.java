package io.topiacoin.workspace.blockchain;

import io.topiacoin.model.MemberNode;
import io.topiacoin.workspace.blockchain.exceptions.ChainAlreadyExistsException;
import io.topiacoin.workspace.blockchain.exceptions.NoSuchChainException;

import java.io.IOException;
import java.util.List;

public interface Chainmail {

	public void start(RPCAdapterManager manager) throws IOException;

	public void stop();

	public boolean createBlockchain(String currentUserID, long workspaceID) throws ChainAlreadyExistsException;

	public boolean startBlockchain(String currentUserID, long workspaceID, List<MemberNode> memberNodes) throws IOException, NoSuchChainException;

	public boolean stopBlockchain(long workspaceID) throws IOException;

	public void addBlockchainListener(ChainmailCallback callback);

	void destroyBlockchain(long workspaceID) throws IOException;
}
