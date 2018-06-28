package io.topiacoin.core.callbacks;

public interface AddMessageCallback {
	public void addedMessage(long workspaceGUID, String message);
	public void failedToAddMessage(long workspaceGUID, String message);
}
