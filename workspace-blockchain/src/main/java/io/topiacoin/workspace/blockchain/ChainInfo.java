package io.topiacoin.workspace.blockchain;

import java.util.HashMap;
import java.util.Map;

public class ChainInfo {
	public int rpcPort;
	public int peerPort;
	public String workspaceId;
	public Process proc;
	public RPCAdapter rpcAdapter;
	public Map<String, String> extraInfo = new HashMap<>();

	public ChainInfo(String name, int rpc, int peer, Process multichaindProcess, RPCAdapter rpcAdap) {
		rpcPort = rpc;
		peerPort = peer;
		workspaceId = name;
		proc = multichaindProcess;
		rpcAdapter = rpcAdap;
	}

	public boolean isChainRunning() {
		return proc != null;
	}

	public void stopChain() {
		if(isChainRunning()) {
			proc.destroy();
			proc = null;
		}
	}
}
