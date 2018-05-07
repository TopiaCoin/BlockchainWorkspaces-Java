package io.topiacoin.core.callbacks;

import java.io.File;

public interface AddFileCallback {

    void didAddFile(File addFile) ;

    void failedToAddFile(File file) ;
}
