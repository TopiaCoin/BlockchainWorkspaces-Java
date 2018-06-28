package io.topiacoin.core.callbacks;

import io.topiacoin.model.Workspace;

public interface LeaveWorkspaceCallback {
	public void leftWorkspace(Workspace workspace);
	public void failedToLeaveWorkspace(Workspace workspace);
}
