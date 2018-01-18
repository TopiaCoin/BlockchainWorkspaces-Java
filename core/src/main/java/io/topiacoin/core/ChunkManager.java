package io.topiacoin.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ChunkManager {

    /**
     * Saves a chunk into the chunk manager. Depending on the implementation, this may involve sending the chunk to
     * another system for storage. In any case, a copy of the chunk is placed into the cache for fast access in the
     * future.
     */
    public void saveChunk(String chunkID, ByteArrayInputStream data){

    }

    /**
     * Saves a chunk into the chunk manager. Depending on the implementation, this may involve sending the chunk to
     * another system for storage. In any case, a copy of the chunk is placed into the cache for fast access in the
     * future.
     */
    public void saveChunk(String chunkID, byte[] data){

    }

    /**
     * Retrieves the specified chunk and returns it. Depending on the implementation, this may involve fetching the chunk
     * from the server, or from another member of the workspace. In any case, the chunk cache is first consulted to see
     * if it has the chunk. If so, it is returned. If not, the chunk is fetched from a remote location, then stored in
     * the cache after it is acquired. If the chunk cannot be found in the cache and cannot be retrieved from a remote
     * site, an exception is thrown.
     */
    public ByteArrayOutputStream getChunkAsStream(String chunkID) {
        return null ;
    }

    /**
     * Retrieves the specified chunk and returns it. Depending on the implementation, this may involve fetching the chunk
     * from the server, or from another member of the workspace. In any case, the chunk cache is first consulted to see
     * if it has the chunk. If so, it is returned. If not, the chunk is fetched from a remote location, then stored in
     * the cache after it is acquired. If the chunk cannot be found in the cache and cannot be retrieved from a remote
     * site, an exception is thrown.
     */
    public byte[] getChunk(String chunkID){
        return null;
    }

    /**
     * Removes the specified chunk ID. This will purge the chunk from the cache, if present. Then it may attempt to
     * remove the chunk from the remote storage location(s), if appropriate.
     */
    public void removeChunk(String chunkID){

    }

    /**
     * Indicates if the Chunk Manager has the specified chunk in its cache. Returns true if the chunk is in the cache.
     * Otherwise, returns false.
     */
    public boolean hasChunk(String chunkID){
        return false ;
    }

}
