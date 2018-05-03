package io.topiacoin.core.callbacks;

import java.io.File;

public interface SaveFileVersionCallback {
    void didSaveFile(String fileGUID, String fileVersionGUID, File targetFile);

    void failedToSaveFile(String fileGUID, String fileVersionGUID, String failureMessage) ;
}
