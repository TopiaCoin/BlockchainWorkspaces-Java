package io.topiacoin.core.callbacks;

import io.topiacoin.model.Workspace;

public interface RemoveUserCallback {
	public void removedUser(Workspace workspace, String memberID);
	public void failedToRemoveUser(Workspace workspace, String memberID);
}
