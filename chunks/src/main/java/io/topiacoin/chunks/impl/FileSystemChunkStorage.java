package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunkStorage;
import io.topiacoin.chunks.intf.ReservationID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class FileSystemChunkStorage implements ChunkStorage {

    private final String purgeExtension = ".lastAccess";
    private Log _log = LogFactory.getLog(this.getClass());

    private File chunkStorageDirectory;

    private static final int SPLIT_LENGTH = 2;
    private static final int SPLIT_COUNT = 4;

    @PostConstruct
    public void init() {
        _log.info("Initializing File System Chunk Storage");

        if ( !chunkStorageDirectory.exists() ) {
            if ( ! chunkStorageDirectory.mkdirs() ) {
                _log.fatal ( "Unable to create the Chunk Storage Directory" ) ;
                throw new RuntimeException("Unable to create the chunk storage directory: " + chunkStorageDirectory ) ;
            }
        }

        _log.info("Initialized File System Chunk Storage");
    }

    @PreDestroy
    public void shutdown() {
        _log.info("Shutting Down File System Chunk Storage");

        _log.info("Shut Down File System Chunk Storage");

    }

    /**
     * Adds a chunk to the Chunk Storage using the data in the chunkStream.
     *
     * @param chunkID     The ID of the chunk whose data is being added.
     * @param chunkStream An InputStream containing the chunk data.
     *
     * @throws DuplicateChunkException If there is already data in Storage with the specified chunkID.
     */
    public void addChunk(final String chunkID, final InputStream chunkStream, ReservationID reservationID, boolean purgeable) throws DuplicateChunkException, IOException {
        File storageFile = getDataFilePathForChunkID(chunkID);

        if ( storageFile.exists() ) {
            throw new DuplicateChunkException("This chunk is already in chunk Storage") ;
        }

        // Create the parent directories so that we can successfully save this file.
        if ( !storageFile.getParentFile().exists() && !storageFile.getParentFile().mkdirs()) {
            throw new IOException("Unable to save the Chunk");
        }

        Files.copy(chunkStream, storageFile.toPath());

        if ( purgeable ) {
            File purgeFlag = new File (storageFile.getParentFile(), storageFile.getName() + purgeExtension ) ;
            purgeFlag.createNewFile();
        }
    }

    /**
     * Retrieves the chunk data for the chunk with the specified chunkID.  Throws an exception if no chunk data with the
     * specifid chunkID is available in the storage.
     *
     * @param chunkID The ID of the chunk whose data is being retrieved.
     *
     * @return An InputStream containing the data for the requested chunkID.
     *
     * @throws NoSuchChunkException If there is no chunk data with the specified chunkID.
     */
    public InputStream getChunkData(final String chunkID) throws NoSuchChunkException {
        File storageFile = getDataFilePathForChunkID(chunkID);
        File purgeFlag = new File (storageFile.getParentFile(), storageFile.getName() + purgeExtension ) ;

        if (!storageFile.exists()) {
            throw new NoSuchChunkException("Chunk " + chunkID + " does not exist.");
        }

        try {
            FileInputStream fis = new FileInputStream(storageFile);

            if ( purgeFlag.exists()) {
                purgeFlag.setLastModified(System.currentTimeMillis()) ;
            }

            return fis;
        } catch (FileNotFoundException e) {
            _log.info("Requested chunk not found!");
            throw new NoSuchChunkException("Chunk " + chunkID + " does not exist.");
        }
    }

    /**
     * Checks whether data for a specific chunk is available in the storage.
     *
     * @param chunkID The ID of the chunk whose existence is being checked.
     *
     * @return True if the data for the requested chunk is available.  False if the data for the requested chunk is not
     * available.
     */
    public boolean hasChunk(final String chunkID) {
        File storageFile = getDataFilePathForChunkID(chunkID);

        return storageFile.exists();
    }

    /**
     * Removes the data for the specified chunkID.
     *
     * @param chunkID The ID of the chunk whose data is being removed.
     *
     * @return True if the data was removed from storage.  False if no data was stored with the specified chunkID.
     */
    public boolean removeChunk(final String chunkID) {
        File storageFile = getDataFilePathForChunkID(chunkID);
        File purgeFlag = new File (storageFile.getParentFile(), storageFile.getName() + purgeExtension) ;

        boolean deleted = storageFile.delete();
        purgeFlag.delete();

        // Walk up the tree removing empty directories
        File parentDir = storageFile.getParentFile();
        boolean done = parentDir.equals(chunkStorageDirectory) ;
        while ( ! done ) {
            String[] filesInDir = parentDir.list();
            if ( filesInDir != null && filesInDir.length == 0 ) {
                if ( parentDir.delete()) {
                    parentDir = parentDir.getParentFile();
                    done = parentDir.equals(chunkStorageDirectory) ;
                } else {
                    done = true ;
                }
            } else {
                done = true;
            }
        }
        return deleted;
    }

    /**
     * Returns the total amount of storage available from the chunk storage.  Generally speaking, the chunk storage
     * instance will not be able to store more than this amount of data.
     *
     * @return The total number of bytes of storage available from the chunk storage.
     */
    @Override
    public long getTotalStorage() {
        return 0;
    }

    /**
     * Returns the amount of unused storage available from the chunk storage.  This amount is the total amount of
     * storage managed by the chunk storage minus the total size of the chunks currently stored in it.
     *
     * @return The number of bytes of unused storage available from the chunk storage.
     */
    @Override
    public long getAvailableStorage() {
        return 0;
    }

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
    @Override
    public boolean purgeStorage(long neededAvailableSpace) {
        return false;
    }

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
     */
    @Override
    public ReservationID reserveStorageSpace(long spaceToReserve) {
        return null;
    }

    /**
     * Releases a reservation for storage space.  The space previously held by this reservation is released back into
     * the general storage pool
     *
     * @param reservationID The ID of the reservation that is being released.
     */
    @Override
    public void releaseSpaceReservation(ReservationID reservationID) {

    }


    // -------- Accessor Methods --------

    public File getChunkStorageDirectory() {
        return chunkStorageDirectory;
    }

    public void setChunkStorageDirectory(File chunkStorageDirectory) {
        this.chunkStorageDirectory = chunkStorageDirectory;
    }


    // -------- Private Methods --------

    private File getDataFilePathForChunkID(final String chunkID) {

        int splitCount = SPLIT_COUNT;
        int splitLength = SPLIT_LENGTH;

        StringBuilder toReturn = new StringBuilder();
        for (int i = 0; i < splitCount; i++) {
            int beginIdx = i * splitLength;
            int endIdx = Math.min((i + 1) * splitLength, chunkID.length());
            toReturn.append(chunkID.substring(beginIdx, endIdx));
            toReturn.append((i + splitLength < chunkID.length() ? File.separator : ""));
        }

        toReturn.append(chunkID);

        return new File(chunkStorageDirectory, toReturn.toString());
    }

    // -------- Inner Reservation ID Class --------

    public class FSReservationID implements ReservationID {

        /**
         * Returns the current validity of this reservation.  A reservation is valid if and only if, it has not expired and
         * all of its reserved storage has not been consumed.
         *
         * @return True if the reservation is valid.  False if the reservation is not valid.
         */
        @Override
        public boolean isValid() {
            return false;
        }

        /**
         * Returns the time when this reservation will expire.  This time may change as the reservation is used.  Expiration
         * is based on elapsed time since last use of the reservation.  If a reservation sits idle, it will expire faster
         * than one that is being actively used to store data.
         *
         * @return The time when this reservation is currently set to expire, in milliseconds since the epoch.
         */
        @Override
        public long getExpirationTime() {
            return 0;
        }
    }
}
