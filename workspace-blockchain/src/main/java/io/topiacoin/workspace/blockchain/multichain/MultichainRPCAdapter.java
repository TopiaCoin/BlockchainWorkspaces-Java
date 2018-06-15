package io.topiacoin.workspace.blockchain.multichain;

import io.topiacoin.chainmail.exceptions.ChainNotRunningException;
import io.topiacoin.chainmail.exceptions.CouldNotCreateStreamException;

public class MultichainRPCAdapter {

    /**
     * Sets up the RPCAdapter with a connection to the Multichain for the given workspaceId
     */
    public MultichainRPCAdapter(/*MultiChainCommand*/ Object rpcConnection, String workspaceId) throws ChainNotRunningException {

    }

    /**
     * Returns results from the RPC Command getinfo
     */
    public String getChainInfo() { //This probably isn't useful, and might get removed
        return null;
    }

    /**
     * Creates a stream on the blockchain if it doesn't already exists. Returns true if the chain didn't exist and was
     * created, false otherwise.
     */
    public boolean createStreamIfNotExists(String streamName) throws CouldNotCreateStreamException {
        return false;
    }

    /**
     * Creates a Transaction on the given Stream for the given Key-Data pair. Handles the necessary Hex-encoding of the
     * Data and signing of the final transaction.
     */
    public void createStreamTransaction(String streamName, String key, String data, String privateKey) {

    }

    /**
     * Returns the Hex-decoded Data of all the transactions for the given Stream with the given key starting at the
     * given block. This will cause the lastModified time of the blockchain to be updated, which is the primary
     * mechanism by which Chainmail determines which chain to shut down when needed
     */
    public String[] listStreamTransactions(String streamName, String key, int startingAt) {
        return null;
    }

    /**
     * Gets the timestamp when the blockchain was last modified.
     */
    public long getLastModified() {
        return 0;
    }
}
