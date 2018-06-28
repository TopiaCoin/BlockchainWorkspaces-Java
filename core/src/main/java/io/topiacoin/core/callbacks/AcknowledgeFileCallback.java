package io.topiacoin.core.callbacks;

public interface AcknowledgeFileCallback {
	public void acknowledgedFile(String fileVersionGUID);
	public void failedToAcknowledgeFile(String fileVersionGUID);
}
