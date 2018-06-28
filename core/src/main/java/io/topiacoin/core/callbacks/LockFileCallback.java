package io.topiacoin.core.callbacks;

import io.topiacoin.model.File;

public interface LockFileCallback {
	public void lockedFile(File fileToLock);
	public void failedToLockFile(File fileToLock);
}
