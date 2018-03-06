package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.intf.ChunkInfoManager;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryChunkInfoManager implements ChunkInfoManager {

    private List<String> unpurgeableChunks ;

    public InMemoryChunkInfoManager () {
        unpurgeableChunks = new ArrayList<>();
    }

    @PostConstruct
    public void init() {

    }

    @PreDestroy
    public void shudown() {

    }

    @Override
    public boolean canPurgeChunk(String chunkID) {
        return ! unpurgeableChunks.contains(chunkID);
    }

    public void addUnpurgeableChunk(String chunkID) {
        unpurgeableChunks.add(chunkID) ;
        Collections.sort(unpurgeableChunks);
    }

    public void removeUnpurgeableChunk(String chunkID) {
        unpurgeableChunks.remove(chunkID) ;
        Collections.sort(unpurgeableChunks);
    }
}
