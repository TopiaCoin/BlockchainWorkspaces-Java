package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.InvalidReservationException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;

import java.io.IOException;
import java.io.InputStream;

public interface ChunkStorage {

    /**
     * Adds a chunk to the Chunk Storage using the data in the chunkStream.
     *
     * @param chunkID       The ID of the chunk whose data is being added.
     * @param chunkStream   An InputStream containing the chunk data.
     * @param reservationID Optional reservation ID this chunk is being downloaded against.
     * @param purgeable     Flag indicating whether the Chunk Storage can purge this chunk to make room for other data.
     *
     * @throws DuplicateChunkException If there is already data in Storage with the specified chunkID.
     */
    void addChunk(final String chunkID, final InputStream chunkStream, ReservationID reservationID, boolean purgeable)
            throws DuplicateChunkException, InvalidReservationException, InsufficientSpaceException, IOException;

    /**
     * Retrieves the chunk data for the chunk with the specified chunkID.  Throws an exception if no chunk data with the
     * specified chunkID is available in the storage.
     *
     * @param chunkID The ID of the chunk whose data is being retrieved.
     *
     * @return An InputStream containing the data for the requested chunkID.
     *
     * @throws NoSuchChunkException If there is no chunk data with the specified chunkID.
     */
    InputStream getChunkDataStream(final String chunkID)
            throws NoSuchChunkException;

    /**
     * Retrieves the chunk data for the chunk with the specified chunkID.  Throws an exception if no chunk data with the
     * specified chunkID is available in the storage.
     *
     * @param chunkID The ID of the chunk whose data is being retrieved.
     *
     * @return A byte[] containing the data for the requested chunkID.
     *
     * @throws NoSuchChunkException If there is no chunk data with the specified chunkID.
     */
    byte[] getChunkData(final String chunkID)
            throws NoSuchChunkException, IOException;

    /**
     * Checks whether data for a specific chunk is available in the storage.
     *
     * @param chunkID The ID of the chunk whose existence is being checked.
     *
     * @return True if the data for the requested chunk is available.  False if the data for the requested chunk is not
     * available.
     */
    boolean hasChunk(final String chunkID);

    /**
     * Removes the data for the specified chunkID.
     *
     * @param chunkID The ID of the chunk whose data is being removed.
     *
     * @return True if the data was removed from storage.  False if no data was stored with the specified chunkID.
     */
    boolean removeChunk(final String chunkID);

    /**
     * Returns the total amount of storage available from the chunk storage.  Generally speaking, the chunk storage
     * instance will not be able to store more than this amount of data.
     *
     * @return The total number of bytes of storage available from the chunk storage.
     */
    long getStorageQuota();

    /**
     * Returns the amount of unused storage available from the chunk storage.  This amount is the total amount of
     * storage managed by the chunk storage minus the total size of the chunks currently stored in it.
     *
     * @return The number of bytes of unused storage available from the chunk storage.
     */
    long getAvailableStorage();

    /**
     * Attempts to remove chunks from the chunk storage in order to make the requested amount of space available.  If
     * the chunk storage is able to make enough space available, the method will return true.  If the chunk storage
     * cannot make enough storage space available, the method will return false.
     *
     * @param neededAvailableSpace The amount of space, in bytes, that should be made available.
     *
     * @return True if the chunk storage is able to make the requested amount of space available.  False if the chunk
     * storage NOT able to make the requested amount of space available.
     */
    boolean purgeStorage(long neededAvailableSpace);

    /**
     * Reserves a requested amount of space.  The method will return a reservation ID that is passed in when adding
     * chunk data to the chunk storage.  By reserving storage space, the caller can be assured that the space they need
     * will be available when they need it.
     * <p>
     * All Reservations are subject to being revoked due to disuse.  If a Reservation is not used for a certain time
     * period, which varies according to the configuration, the reservation will be cancelled and can no longer be used
     * to add chunks
     *
     * @param spaceToReserve The amount of storage, in bytes, that is being reserved.
     *
     * @return A reservation ID that is used when adding chunks to storage.
     *
     * @throws InsufficientSpaceException If there is not sufficient storage quota remaining to satisfy the
     *                                    reservation.
     */
    ReservationID reserveStorageSpace(long spaceToReserve) throws InsufficientSpaceException;

    /**
     * Releases a reservation for storage space.  The space previously held by this reservation is released back into
     * the general storage pool
     *
     * @param reservationID The ID of the reservation that is being released.
     */
    void releaseSpaceReservation(ReservationID reservationID) throws InvalidReservationException;
}
