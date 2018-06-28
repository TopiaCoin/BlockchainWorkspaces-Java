package io.topiacoin.core.callbacks;

import io.topiacoin.model.Workspace;

public interface InviteUserCallback {
	public void invitedUser(Workspace workspace, String userID);
	public void failedToInviteUser(Workspace workspace, String userID);
}
