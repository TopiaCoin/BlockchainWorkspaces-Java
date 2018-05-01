package io.topiacoin.chunks;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.InvalidReservationException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunkStorage;
import io.topiacoin.chunks.intf.ChunkTransferer;
import io.topiacoin.chunks.intf.ChunksFetchHandler;
import io.topiacoin.chunks.intf.ChunksTransferHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class ChunkManager {
	private static final Log _log = LogFactory.getLog(ChunkManager.class);
	ChunkTransferer _chunkTransferer;
	ChunkStorage _chunkStorage;

	/**
	 * Adds a chunk to the Chunk Manager.  The given chunkData is stored in the Chunk Manager under the specified
	 * chunkID.
	 *
	 * @param chunkID   The ID under which to store the chunk data.
	 * @param chunkData A byte array containing the chunk data to be stored.
	 * @throws DuplicateChunkException if a chunk with the specified chunkID has already been added
	 * @throws InsufficientSpaceException if not enough space is available to store the chunk data
	 * @throws IOException if some exception occurs regarding IO
	 */
	public void addChunk(final String chunkID, final byte[] chunkData) throws DuplicateChunkException, InsufficientSpaceException, IOException {
		addChunk(chunkID, new ByteArrayInputStream(chunkData));
	}

	/**
	 * Adds a chunk to the Chunk Manager.  The data stored in the given chunkFile is stored in the Chunk Manager under
	 * the specified chunkID.
	 *
	 * @param chunkID   The ID under which to store the chunk data contained in chunkFile.
	 * @param chunkFile A reference to the file containing the chunk data to be stored.
	 *
	 * @throws DuplicateChunkException if a chunk with the specified chunkID has already been added
	 * @throws InsufficientSpaceException if not enough space is available to store the chunk data
	 * @throws IOException if some exception occurs regarding IO
	 */
	public void addChunk(final String chunkID, final File chunkFile) throws DuplicateChunkException, IOException, InsufficientSpaceException {
		addChunk(chunkID, new FileInputStream(chunkFile));
	}

	/**
	 * Adds a chunk to the Chunk Manager.  The data contained in the chunkStream is stored in the Chunk Manager under
	 * the specified chunkID.  The chunkStream should be of a finite size.
	 *
	 * @param chunkID     The ID under which to store the chunk data contained in chunkFile.
	 * @param chunkStream The InputStream from which the chunk data will be read.
	 * @throws DuplicateChunkException if a chunk with the specified chunkID has already been added
	 * @throws InsufficientSpaceException if not enough space is available to store the chunk data
	 * @throws IOException if some exception occurs regarding IO
	 */
	public void addChunk(final String chunkID, final InputStream chunkStream) throws DuplicateChunkException, InsufficientSpaceException, IOException {
		try {
			_chunkStorage.addChunk(chunkID, chunkStream, null, false);
		} catch (InvalidReservationException e) {
			_log.error("Unexpected error", e);
			throw new IOException("", e);
		}
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
		try {
			return _chunkStorage.getChunkData(chunkID);
		} catch (IOException e) {
			_log.error("Failed to get ChunkData", e);
			throw new NoSuchChunkException("Failed to get Chunk", e);
		}
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
		return _chunkStorage.getChunkDataStream(chunkID);
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
		return _chunkStorage.hasChunk(chunkID);
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
		return _chunkStorage.removeChunk(chunkID);
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
	 * @throws IllegalArgumentException If chunkIDs or containerID is null or empty
	 */
	public void fetchChunks(final List<String> chunkIDs, final String containerID, final ChunksFetchHandler handler, final Object state) {
		if (chunkIDs == null || chunkIDs.isEmpty() || containerID == null || containerID.isEmpty()) {
			throw new IllegalArgumentException();
		}
		final List<String> successfulChunks = new ArrayList<>();
		final List<String> failedChunks = new ArrayList<>();
		final List<String> unfetchedChunks = new ArrayList<>();
		for (String chunkID : chunkIDs) {
			if (hasChunk(chunkID)) {
				successfulChunks.add(chunkID);
			} else {
				unfetchedChunks.add(chunkID);
			}
		}
		ChunksTransferHandler chunkFetchHandler = new ChunksTransferHandler() {
			@Override public void didFetchChunk(String chunkID, Object state) {
				successfulChunks.add(chunkID);
			}

			@Override public void failedToFetchChunk(String chunkID, String message, Exception cause, Object state) {
				failedChunks.add(chunkID);
			}

			@Override public void fetchedAllChunksSuccessfully(Object state) {
				handler.finishedFetchingChunks(successfulChunks, failedChunks, state);
			}

			@Override public void failedToBuildFetchPlan() {
				handler.errorFetchingChunks("Failed to find a place to download the requested chunks", null, state);
			}

			@Override public void failedToFetchAllChunks(Object state) {
				handler.finishedFetchingChunks(successfulChunks, failedChunks, state);
			}
		};
		_chunkTransferer.fetchChunksRemotely(unfetchedChunks, containerID, chunkFetchHandler, state);
	}
}
