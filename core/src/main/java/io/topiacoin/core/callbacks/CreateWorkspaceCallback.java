package io.topiacoin.core.callbacks;

public interface CreateWorkspaceCallback {
	public void createdWorkspace(long workspaceID);
	public void failedToCreateWorkspace();
}
