package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface RemoveFileTagCallback {
	public void removedFileTag(File fileToUntag, String tagName);
	public void failedToRemoveFile(File fileToUntag, String tagName);
}
