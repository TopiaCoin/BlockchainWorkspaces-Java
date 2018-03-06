package io.topiacoin.chunks.intf;

import java.util.List;

public interface ChunksFetchHandler {

    void finishedFetchingChunks(List<String> successfulChunks,
                                List<String> unsuccessfulChunks,
                                Object state);

    void errorFetchingChunks(String message,
                             Exception cause,
                             Object state) ;
}
