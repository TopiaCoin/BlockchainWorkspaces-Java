package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface AddFileTagCallback {
	public void addedFileTag(File fileToTag, String tagName);
	public void failedToAddFileTag(File fileToTag, String tagName);
}
