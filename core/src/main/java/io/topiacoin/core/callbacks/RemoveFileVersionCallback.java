package io.topiacoin.core.callbacks;

public interface RemoveFileVersionCallback {
	public void removedFileVersion(String fileVersionGUID);
	public void failedToRemoveFileVersion(String fileVersionGUID);
}
