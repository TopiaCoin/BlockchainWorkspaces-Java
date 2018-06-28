package io.topiacoin.core.callbacks;

import io.topiacoin.model.Workspace;

public interface UpdateWorkspaceDescriptionCallback {
	public void updatedWorkspaceDescription(Workspace workspaceToUpdate);
	public void failedToUpdateWorkspaceDescription(Workspace workspaceToUpdate);
}
