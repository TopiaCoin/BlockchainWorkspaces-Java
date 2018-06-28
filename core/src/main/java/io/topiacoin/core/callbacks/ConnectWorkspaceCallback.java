package io.topiacoin.core.callbacks;

public interface ConnectWorkspaceCallback {
	public void connectedWorkspace(long workspaceID);
	public void failedToConnectWorkspace(long workspaceID);
}
