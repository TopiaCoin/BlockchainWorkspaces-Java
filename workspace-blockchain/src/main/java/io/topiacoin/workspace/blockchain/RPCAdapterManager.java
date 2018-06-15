package io.topiacoin.workspace.blockchain;

import io.topiacoin.eosrpcadapter.EOSRPCAdapter;
import io.topiacoin.workspace.blockchain.eos.EOSAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the RPC Adapters in SDFS.  This class acts as a repository of RPC Adapters used to communicate with the
 * individual workspace blockchains.  This code is tied directly to the Chainmail instance and is notified whenever
 * a blockchain is started or stopped.  When a blockchain is started, an RPC Adapater instance is created that will
 * interact with the blockchain.  When a blockchain is stopped, the associated RPC Adpater is discarded.
 *
 * For this reason, users of this class should not keep RPC Adapter instances long term.  Instead, they should acquire
 * the RPC Adapter instance when they need it, discard it when they are finished using it, and acquire a new instance
 * on subsequent calls.
 */
public class RPCAdapterManager {

    private Map<String,EOSAdapter> _eosrpcAdapterMap ;

    public RPCAdapterManager(/* Chainmail Instance */) {
        _eosrpcAdapterMap = new HashMap<>();

        // Register with the BlockChain Instance to get notified of chain start and stop.
    }

    /**
     * Returns the RPC Adapter associated with the specified workspaceID, if is available.  Otherwise, returns null.
     *
     * @param workspaceID The ID of the workspace whose RPC Adapter is being retrieved.
     *
     * @return The RPC Adapter associated with the specified workspace ID, or null if no such Adapter exists.
     */
    public EOSAdapter getRPCAdapter(String workspaceID) {
        return _eosrpcAdapterMap.get(workspaceID);
    }

    // ======== Chainmail Callback Methods ========

     public void didStartBlockchain(String workspaceID, Map<String, String> blockchainInfo) {
        String nodeURL = blockchainInfo.get("nodeURL");
        String walletURL = blockchainInfo.get("walletURL");

        EOSAdapter adapter = new EOSAdapter(nodeURL, walletURL);

        _eosrpcAdapterMap.put(workspaceID, adapter) ;
     }

     public void didStopBlockchain(String worksapceID) {
         _eosrpcAdapterMap.remove(worksapceID);
     }
}
