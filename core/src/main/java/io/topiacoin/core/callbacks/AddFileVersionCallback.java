package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface AddFileVersionCallback {
	public void addedFileVersion(File fileToBeAdded);
	public void failedToAddFileVersion(File fileToBeAdded);
}
