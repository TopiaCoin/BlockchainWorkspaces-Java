package io.topiacoin.workspace.blockchain.eos;

import io.topiacoin.model.File;
import io.topiacoin.model.Member;

import java.util.List;

public class Files extends TableResponse<File>{

    public Files(List<File> files, boolean hasMore, Object continuationToken) {
        super(files, hasMore, continuationToken);
    }

    public List<File> getFiles() {
        return getItems();
    }
}
