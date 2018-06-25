package io.topiacoin.workspace.blockchain;

import io.topiacoin.model.MemberNode;
import io.topiacoin.workspace.blockchain.exceptions.ChainAlreadyExistsException;
import io.topiacoin.workspace.blockchain.exceptions.NoSuchChainException;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

public interface Chainmail {

	public void start(RPCAdapterManager manager) throws IOException;

	public void stop();

	public boolean createBlockchain(String currentUserID, String workspaceID) throws ChainAlreadyExistsException;

	public boolean startBlockchain(String currentUserID, String workspaceID, List<MemberNode> memberNodes) throws IOException, NoSuchChainException;

	public boolean stopBlockchain(String workspaceID) throws IOException;

	public void addBlockchainListener(ChainmailCallback callback);

	void destroyBlockchain(String workspaceID) throws IOException;
}
