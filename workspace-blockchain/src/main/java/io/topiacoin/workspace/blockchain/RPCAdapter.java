package io.topiacoin.workspace.blockchain;

public class RPCAdapter {
	private long lastModified = System.currentTimeMillis();

	public long getLastModified() {
		return lastModified;
	}

	public void updateLastModified() {
		lastModified = System.currentTimeMillis();
	}
}
