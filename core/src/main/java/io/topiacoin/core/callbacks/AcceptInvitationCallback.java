package io.topiacoin.core.callbacks;

import io.topiacoin.model.Workspace;

public interface AcceptInvitationCallback {
	public void acceptedInvitation(Workspace workspace);
	public void failedToAcceptInvitation(Workspace workspace);
}
