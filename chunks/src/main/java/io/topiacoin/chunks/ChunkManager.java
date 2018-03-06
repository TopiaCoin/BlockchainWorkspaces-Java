package io.topiacoin.chunks;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunkFetchHandler;
import io.topiacoin.chunks.intf.ChunksFetchHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class ChunkManager {

    /**
     * Adds a chunk to the Chunk Manager.  The given chunkData is stored in the Chunk Manager under the specified
     * chunkID.
     *
     * @param chunkID   The ID under which to store the chunk data.
     * @param chunkData A byte array containing the chunk data to be stored.
     */
    public void addChunk(final String chunkID, final byte[] chunkData) throws DuplicateChunkException {
        addChunk(chunkID, new ByteArrayInputStream(chunkData));
    }

    /**
     * Adds a chunk to the Chunk Manager.  The data stored in the given chunkFile is stored in the Chunk Manager under
     * the specified chunkID.
     *
     * @param chunkID   The ID under which to store the chunk data contained in chunkFile.
     * @param chunkFile A reference to the file containing the chunk data to be stored.
     *
     * @throws FileNotFoundException
     */
    public void addChunk(final String chunkID, final File chunkFile) throws DuplicateChunkException, FileNotFoundException {
        addChunk(chunkID, new FileInputStream(chunkFile));
    }

    /**
     * Adds a chunk to the Chunk Manager.  The data contained in the chunkStream is stored in the Chunk Manager under
     * the specified chunkID.  The chunkStream should be of a finite size.
     *
     * @param chunkID     The ID under which to store the chunk data contained in chunkFile.
     * @param chunkStream The InputStream from which the chunk data will be read.
     */
    public void addChunk(final String chunkID, final InputStream chunkStream) throws DuplicateChunkException {
        // TODO - Implement this method
    }

    /**
     * Retrieves chunk data stored in the Chunk Manager.  The chunk data associated with the specified chunkID is
     * returned. If the Chunk Manager does not contain the requested chunk, a NoSuchChunkException is thrown.
     *
     * @param chunkID The ID of the chunk whose data is being requested.
     *
     * @return A byte array containing the requested chunk data.
     *
     * @throws NoSuchChunkException If the Chunk Manager does not have data for the requested chunk.
     */
    public byte[] getChunkData(final String chunkID) throws NoSuchChunkException {
        // TODO - Implement this method
        return null;
    }

    /**
     * Retrieves chunk data stored in the Chunk Manager.  The chunk data associated with the specified chunkID is
     * returned. If the Chunk Manager does not contain the requested chunk, a NoSuchChunkException is thrown.
     *
     * @param chunkID The ID of the chunk whose data is being requested.
     *
     * @return An InputStream containing the requested chunk data.
     *
     * @throws NoSuchChunkException If the Chunk Manager does not have data for the requested chunk.
     */
    public InputStream getChunkDataAsStream(final String chunkID) throws NoSuchChunkException {
        // TODO - Implement this method
        return null;
    }

    /**
     * Indicates whether the Chunk Manager has chunk data for the specified chunkID.
     *
     * @param chunkID The ID of the chunk whose existence is being checked.
     *
     * @return True if the Chunk Manager has the data for the specified chunkID.  False if the Chunk Manager does not
     * have data for the specified chunkID.
     */
    public boolean hasChunk(final String chunkID) {
        return false;
    }

    /**
     * Tells the Chunk Manager to remove the specified chunk.  The chunk data for the specified chunkID is purged from
     * the Chunk Manager's local storage.
     *
     * @param chunkID The ID of the chunk that is being removed.
     *
     * @return True if the chunk data has been removed.  False if the Chunk Manager does not have data for the requested
     * chunkID.
     */
    public boolean removeChunk(final String chunkID) {
        // TODO - Implement this method
        return false;
    }

    /**
     * Requests that the Chunk Manager fetch the chunk data for the specified chunkID.  The Chunk Manager will
     * communicate with the other nodes that make up the micro-network for the associated container to retrieve the data
     * for the requested chunk. The fetch operation is performed asynchronously and the handler is called when the fetch
     * operation is completed.
     *
     * @param chunkID     The ID of the chunk whose data is to be fetched.
     * @param containerID The ID of the container from which the chunk is being fetched.
     * @param handler     The object that should be notified when the fetch operation is completed.
     * @param state       An opaque object that will be passed to the handler on fetch operation completion.  This can
     *                    be used to carry state between the initiator of the fetch and the handler.
     */
    public void fetchChunk(final String chunkID, final String containerID, final ChunkFetchHandler handler, final Object state) {
        // TODO - Implement this method
    }

    /**
     * Requests that the Chunk Manager fetch the chunk data for the specified chunkIDs.  The Chunk Manager will
     * communicate with the other nodes that make up the micro-network for the associated container to retrieve the data
     * for the requested chunks.  The fetch operation is performed asynchronously and the handler is called when the
     * fetch operation is completed.
     *
     * @param chunkIDs    A list of the IDs of chunks whose data is to be fetched.
     * @param containerID The ID of the container from which the chunk is being fetched.
     * @param handler     The object that will be notified when the fetch operation is completed.
     * @param state       An opaque object that will be passed to the handler on fetch operation completion.  This can
     *                    be used to carry state between the initiator of the fetch and the handler.
     */
    public void fetchChunks(final List<String> chunkIDs, final String containerID, final ChunksFetchHandler handler, final Object state) {
        // TODO - Implement this method
    }
}
