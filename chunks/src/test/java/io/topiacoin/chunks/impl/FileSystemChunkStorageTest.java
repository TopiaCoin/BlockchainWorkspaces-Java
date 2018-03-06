package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.intf.AbstractChunkStorageTest;
import io.topiacoin.chunks.intf.ChunkInfoManager;
import io.topiacoin.chunks.intf.ChunkStorage;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;

public class FileSystemChunkStorageTest extends AbstractChunkStorageTest {

    private static File _chunkDir;

    @BeforeClass
    public static void setupClass() {
        _chunkDir = new File("./target/chunks");

        // Delete everything in the directory
        cleanDir(_chunkDir);
    }

    @AfterClass
    public static void tearDownClass() {
        // Delete everything in the directory
//        cleanDir(_chunkDir);

        // Delete the directory
        _chunkDir.delete();
    }

    @Override
    protected ChunkStorage getChunkStorage(long quota, long inactivityTimeout, ChunkInfoManager chunkInfoManager) {

        FileSystemChunkStorage chunkStorage = new FileSystemChunkStorage();
        chunkStorage.setChunkStorageDirectory(_chunkDir);
        chunkStorage.setStorageQuota(quota);
        chunkStorage.setReservationInactivityTimeout(inactivityTimeout);
        chunkStorage.setChunkInfoManager(chunkInfoManager);

        chunkStorage.init();

        return chunkStorage;
    }

    @Override
    protected void emptyChunkStorage(ChunkStorage chunkStorage) {
        cleanDir(_chunkDir);
        ((FileSystemChunkStorage)chunkStorage).clearReservations() ;
        ((FileSystemChunkStorage)chunkStorage).updateUsedStorage();
    }

    private static void cleanDir(File dir) {
        if ( dir == null || !dir.exists() ) {
            return;
        }

        File[] children = dir.listFiles();

        for (File child : children) {
            if (child.isFile()) {
                child.delete();
            } else {
                cleanDir(child);
                child.delete();
            }
        }
    }
}
