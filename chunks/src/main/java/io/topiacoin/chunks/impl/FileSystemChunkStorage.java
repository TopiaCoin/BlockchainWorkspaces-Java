package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.InvalidReservationException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunkInfoManager;
import io.topiacoin.chunks.intf.ChunkStorage;
import io.topiacoin.chunks.intf.ReservationID;
import org.apache.commons.io.IOUtils;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class FileSystemChunkStorage implements ChunkStorage {

    public static final int DEFAULT_INACTIVITY_TIMEOUT = 300000;

    private Log _log = LogFactory.getLog(this.getClass());

    private File chunkStorageDirectory;

    private static final int SPLIT_LENGTH = 2;
    private static final int SPLIT_COUNT = 2;

    private Map<String, FSReservationID> reservationMap;

    private ChunkInfoManager _chunkInfoManager;

    private long storageQuota;
    private long storageUsed;

    private long reservationInactivityTimeout = DEFAULT_INACTIVITY_TIMEOUT;

    @PostConstruct
    public void init() {
        _log.info("Initializing File System Chunk Storage");

        if (!chunkStorageDirectory.exists()) {
            if (!chunkStorageDirectory.mkdirs()) {
                _log.fatal("Unable to create the Chunk Storage Directory");
                throw new RuntimeException("Unable to create the chunk storage directory: " + chunkStorageDirectory);
            }
        }

        reservationMap = new HashMap<>();
        updateUsedStorage();

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
    public void addChunk(final String chunkID, final InputStream chunkStream, ReservationID reservationID, boolean purgeable)
            throws DuplicateChunkException, InvalidReservationException, InsufficientSpaceException, IOException {

        if ( reservationID != null && ! (reservationID instanceof FSReservationID ) ) {
            throw new InvalidReservationException("Unrecognized Reservation ID") ;
        }

        File storageFile = getDataFilePathForChunkID(chunkID);

        if (storageFile.exists()) {
            throw new DuplicateChunkException("This chunk is already in chunk Storage");
        }

        if (reservationID != null) {
            if (!this.reservationMap.containsValue(reservationID)) {
                throw new InvalidReservationException("The specified reservationID is unrecognized");
            }
            if (reservationID.getRemainingSpace() < chunkStream.available()) {
                throw new InsufficientSpaceException("The specified reservationID does not have enough remaining space for the chunk being added");
            }
            if (!reservationID.isValid()) {
                this.reservationMap.remove(((FSReservationID) reservationID).resID);
                throw new InvalidReservationException("The specified reservationID is not valid");
            }
        }

        // Create the parent directories so that we can successfully save this file.
        if (!storageFile.getParentFile().exists() && !storageFile.getParentFile().mkdirs()) {
            throw new IOException("Unable to save the Chunk");
        }

        long bytesCopied = Files.copy(chunkStream, storageFile.toPath());
        this.storageUsed += bytesCopied;

        if (reservationID != null) {
            ((FSReservationID) reservationID).consumeReservationSpace(bytesCopied);
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
    public InputStream getChunkDataStream(final String chunkID) throws NoSuchChunkException {
        File storageFile = getDataFilePathForChunkID(chunkID);

        if (!storageFile.exists()) {
            throw new NoSuchChunkException("Chunk " + chunkID + " does not exist.");
        }

        try {
            FileInputStream fis = new FileInputStream(storageFile);

            storageFile.setLastModified(System.currentTimeMillis()) ;

            return fis;
        } catch (FileNotFoundException e) {
            _log.info("Requested chunk not found!");
            throw new NoSuchChunkException("Chunk " + chunkID + " does not exist.");
        }
    }

    public byte[] getChunkData(final String chunkID) throws NoSuchChunkException, IOException {
        InputStream data = getChunkDataStream(chunkID);
        return IOUtils.toByteArray(data);
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

        long bytesRemoved = storageFile.length();

        boolean deleted = storageFile.delete();
        if(deleted) {
            this.storageUsed -= bytesRemoved;

            // Walk up the tree removing empty directories
            File parentDir = storageFile.getParentFile();
            boolean done = parentDir.equals(chunkStorageDirectory);
            while (!done) {
                String[] filesInDir = parentDir.list();
                if (filesInDir != null && filesInDir.length == 0) {
                    if (parentDir.delete()) {
                        parentDir = parentDir.getParentFile();
                        done = parentDir.equals(chunkStorageDirectory);
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
        }
        return deleted;
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

        // The available storage is more than we are asking for, so return true immediately.
        long availableStorage = this.getAvailableStorage();
        if ( neededAvailableSpace < availableStorage) {
            return true ;
        }

        // We are being asked for more space than we have quota for, so return false immediately.
        if ( neededAvailableSpace > this.storageQuota ) {
            return false ;
        }

        Set<File> candidateChunks = getChunkFiles( this.chunkStorageDirectory, new Comparator<File>(){

            @Override
            public int compare(File o1, File o2) {
                int chrono = (int)(o1.lastModified() - o2.lastModified()) ;
                if ( chrono == 0 ) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return chrono;
                }
            }
        });

        long spaceToBeFreed = neededAvailableSpace - availableStorage ;

        for ( File file : candidateChunks ) {
            if ( _chunkInfoManager == null || _chunkInfoManager.canPurgeChunk(file.getName()) ) {
                long fileSize = file.length();
                if ( file.delete() ) {
                    spaceToBeFreed -= fileSize;
                    this.storageUsed -= fileSize;
                    if ( spaceToBeFreed <= 0 ){
                        break ;
                    }
                }
            }
        }

        return spaceToBeFreed <= 0 ;
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
    public ReservationID reserveStorageSpace(long spaceToReserve) throws InsufficientSpaceException {

        // Check to make sure that we have enough space left.
        if (getAvailableStorage() < spaceToReserve) {
            throw new InsufficientSpaceException("Unable to reserve the requested amount of storage.");
        }

        // Generate a new ID for this Reservation, then create the ReservationID object.
        String resID = UUID.randomUUID().toString();
        FSReservationID reservationID = new FSReservationID(resID, spaceToReserve, this.reservationInactivityTimeout);

        // Save the reservation ID in the internal map for future reference.
        this.reservationMap.put(resID, reservationID);

        return reservationID;
    }

    /**
     * Releases a reservation for storage space.  The space previously held by this reservation is released back into
     * the general storage pool
     *
     * @param reservationID The ID of the reservation that is being released.
     *
     * @throws InvalidReservationException If the reservation ID is not recognized by the chunk storage.
     */
    @Override
    public void releaseSpaceReservation(ReservationID reservationID)
            throws InvalidReservationException {
        if (!(reservationID instanceof FSReservationID)) {
            throw new InvalidReservationException("Unrecognized ReservationID");
        }

        FSReservationID fsReservationID = (FSReservationID) reservationID;

        if (!this.reservationMap.containsValue(fsReservationID)) {
            throw new InvalidReservationException("The specified reservationID is unrecognized");
        }

        // Remove the reservation from the internal Map
        this.reservationMap.remove(fsReservationID.resID);

        // Set the expiration time to the distant pass to invalidate it.
        fsReservationID.expirationTime = 0;
    }


    // -------- Accessor Methods --------

    /**
     * Returns the directory where chunks are being stored.
     *
     * @return The directory where chunks are being stored.
     */
    public File getChunkStorageDirectory() {
        return chunkStorageDirectory;
    }


    /**
     * Sets the directory where chunks will be stored.
     *
     * @param chunkStorageDirectory The directory where chunks are to be stored.
     */
    public void setChunkStorageDirectory(File chunkStorageDirectory) {
        this.chunkStorageDirectory = chunkStorageDirectory;
    }

    public void setChunkInfoManager(ChunkInfoManager chunkInfoManager) {
        _chunkInfoManager = chunkInfoManager;
    }

    /**
     * Sets the Storage Quota for the File System Chunk Storage.  This informs the Chunk Storage how much data, in
     * bytes, it is allowed to store.  Attempts to store more than this amount of data will result in the system
     * attempting to purge data to make room for the storage attempt, or the add failing if enough room cannot be made.
     *
     * @param storageQuota The amount of storage, in bytes, that the Chunk Storage is alloed to consume.
     */
    public void setStorageQuota(long storageQuota) {
        this.storageQuota = storageQuota;
    }


    /**
     * Sets the length of time a reservation can be inactive before it is marked as inactive.
     *
     * @param reservationInactivityTimeout The length of time, in milliseconds, before a reservation with no activity is
     *                                     marked as inactive.
     */
    public void setReservationInactivityTimeout(long reservationInactivityTimeout) {
        this.reservationInactivityTimeout = reservationInactivityTimeout;
    }

    /**
     * Returns the total amount of storage available from the chunk storage.  Generally speaking, the chunk storage
     * instance will not be able to store more than this amount of data.
     *
     * @return The total number of bytes of storage available from the chunk storage.
     */
    @Override
    public long getStorageQuota() {
        return this.storageQuota;
    }

    /**
     * Returns the amount of unused storage available from the chunk storage.  This amount is the total amount of
     * storage managed by the chunk storage minus the total size of the chunks currently stored in it minus the total
     * space remaining in the active reservations.
     *
     * @return The number of bytes of unused storage available from the chunk storage.
     */
    @Override
    public long getAvailableStorage() {
        long available = this.storageQuota;
        available -= this.storageUsed;
        Iterator<FSReservationID> iterator = this.reservationMap.values().iterator();
        while (iterator.hasNext()) {
            ReservationID reservationID = iterator.next();
            if (reservationID.isValid()) {
                available -= reservationID.getRemainingSpace();
            } else {
                iterator.remove();
            }
        }
        return available;
    }


    /**
     * Returns the amount of storage currently used by the Chunk Storage.
     *
     * @return The amount of storage, in bytes, currently consumed by the Chunk Storage.
     */
    public long getStorageUsed() {
        return storageUsed;
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


    /**
     * Updates the variable containing the amount of storage used based on the actual storage used on disk.
     */
    void updateUsedStorage() {
        this.storageUsed = calculateUsedStorage(this.chunkStorageDirectory);
    }


    /**
     * Clears all of the reservations in the File System Chunk Storage.
     * <p>
     * This method is only used for testing purposes.
     */
    void clearReservations() {
        this.reservationMap.clear();
    }


    /**
     * Calculates and returns the total amount of storage used by the chunks stored in the Chunk Storage.
     */
    private long calculateUsedStorage(File root) {
        long storageUsed = 0;
        File[] children = root.listFiles();

        if (children != null) {
            for (File child : children) {
                if (child.isFile()) {
                    storageUsed += child.length();
                } else if (child.isDirectory()) {
                    storageUsed += calculateUsedStorage(child);
                }
            }
        }

        return storageUsed;
    }

    private Set<File> getChunkFiles(File root, Comparator<File> comparator) {
        TreeSet<File> chunkFiles = new TreeSet<>(comparator);

        File[] children = root.listFiles();

        if (children != null) {
            for (File child : children) {
                if (child.isFile()) {
                    chunkFiles.add(child) ;
                } else if (child.isDirectory()) {
                    Set<File> descendants = getChunkFiles(child, comparator);
                    chunkFiles.addAll(descendants);
                }
            }
        }

        return chunkFiles;
    }

    // -------- Inner Reservation ID Class --------

    public static class FSReservationID implements ReservationID {

        private final String resID;
        private final long spaceReserved;
        private final long inactivityTimeout;
        private long spaceRemaining;
        private long expirationTime;

        /**
         * Creates a new FSReservationID object.  The reservation ID will contain the identification String assigned to
         * this reservation, the amount of space reserverd, and the inactivity timeout of the reservation.  From this,
         * the reservation will calculate the amount of space reamining on the reservation, and the time at which the
         * reservation expires.
         *
         * @param resID             The ID assigned to the reservation.
         * @param spaceReserved     The amount of space, in bytes, reserved with this reservation.
         * @param inactivityTimeout The inactivity time, in milliseconds, after which the reservation becomes invalid.
         */
        public FSReservationID(String resID, long spaceReserved, long inactivityTimeout) {
            this.resID = resID;
            this.spaceReserved = spaceReserved;
            this.inactivityTimeout = inactivityTimeout;

            this.spaceRemaining = this.spaceReserved;
            this.expirationTime = System.currentTimeMillis() + inactivityTimeout;
        }

        /**
         * Returns the current validity of this reservation.  A reservation is valid if and only if, it has not expired
         * and it has reserved storage space remaining.
         *
         * @return True if the reservation is valid.  False if the reservation is not valid.
         */
        @Override
        public boolean isValid() {
            boolean isValid = true;

            isValid = (System.currentTimeMillis() <= this.expirationTime) &&
                    (this.spaceRemaining > 0);
            return isValid;
        }

        /**
         * Returns the time when this reservation will expire.  This time may change as the reservation is used.
         * Expiration is based on elapsed time since last use of the reservation.  If a reservation sits idle, it will
         * expire faster than one that is being actively used to store data.
         *
         * @return The time when this reservation is currently set to expire, in milliseconds since the epoch.
         */
        @Override
        public long getExpirationTime() {
            return this.expirationTime;
        }

        /**
         * Returns the amount of storage space this reservation reserved.
         *
         * @return The amount of storage space reserved by this reservation.
         */
        @Override
        public long getReservedSpace() {
            return this.spaceReserved;
        }

        /**
         * Returns the amount of reserved space remaining in this reservation.  This amount is decremented everytime a
         * chunk is added tot he chunk storage against this reservation.
         *
         * @return The amount of reserved storage space remaining.
         */
        @Override
        public long getRemainingSpace() {
            return this.spaceRemaining;
        }

        /**
         * Notifies the Reservation that space has been consumed.  This will decrement the remaining space by the amount
         * consumed, and reset the expiration time.
         *
         * @param spaceConsumed
         */
        protected void consumeReservationSpace(long spaceConsumed) {
            if (this.spaceRemaining > spaceConsumed) {
                this.spaceRemaining -= spaceConsumed;
            } else {
                // We have consumed all remaining space, so set it to 0.
                this.spaceRemaining = 0;
            }
            this.expirationTime = System.currentTimeMillis() + this.inactivityTimeout;
        }

        @Override
        public String toString() {
            return "FSReservationID{" +
                    "resID='" + resID + '\'' +
                    ", spaceReserved=" + spaceReserved +
                    ", inactivityTimeout=" + inactivityTimeout +
                    ", spaceRemaining=" + spaceRemaining +
                    ", expirationTime=" + expirationTime +
                    '}';
        }
    }
}
