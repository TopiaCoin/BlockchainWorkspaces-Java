package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface RemoveFolderCallback {
	public void removedFolder(File folderToRemove);
	public void failedToRemoveFolder(File folderToRemove);
}
