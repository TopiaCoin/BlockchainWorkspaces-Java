package io.topiacoin.workspace.blockchain;

import java.util.HashMap;
import java.util.Map;

public class ChainInfo {
	public int rpcPort;
	public int peerPort;
	public String workspaceId;
	public Process proc;
	public Map<String, String> extraInfo = new HashMap<>();

	public ChainInfo(String name, int rpc, int peer, Process multichaindProcess) {
		rpcPort = rpc;
		peerPort = peer;
		workspaceId = name;
		proc = multichaindProcess;
	}
}
