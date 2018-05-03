package io.topiacoin.core.callbacks;

public interface DownloadFileVersionCallback {

    void didDownloadFileVersion(String fileGUID, String fileVersionGUID) ;

    void failedToDownloadFileVersion(String fileGUID, String fileVersionGUID, String failureMessage) ;
}
