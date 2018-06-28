package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface RemoveFileCallback {
	public void removedFile(File fileToRemove);
	public void failedToRemoveFile(File fileToRemove);
}
