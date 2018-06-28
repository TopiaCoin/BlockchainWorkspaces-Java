package io.topiacoin.core.callbacks;

import io.topiacoin.model.Workspace;

public interface DeclineInvitationCallback {
	public void decliendInvitation(Workspace workspace);
	public void failedToDeclineInvitation(Workspace workspace);
}
