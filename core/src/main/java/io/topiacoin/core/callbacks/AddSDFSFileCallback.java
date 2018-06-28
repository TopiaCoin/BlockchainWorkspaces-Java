package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface AddSDFSFileCallback {
	void didAddFile(File fileToAdd);

	void failedToAddFile(File fileToAdd);
}
