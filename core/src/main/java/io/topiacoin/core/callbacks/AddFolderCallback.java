package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface AddFolderCallback {
	public void addedFolder(File folderToAdd);
	public void failedToAddFolder(File folderToAdd);
}
