package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface UnlockFileCallback {
	public void unlockedFile(File fileToUnlock);
	public void failedToUnlockFile(File fileToUnlock);
}
