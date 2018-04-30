package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.InvalidReservationException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.impl.FileSystemChunkStorage;
import io.topiacoin.chunks.impl.InMemoryChunkInfoManager;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

import static junit.framework.TestCase.*;

public abstract class AbstractChunkStorageTest {

    protected abstract ChunkStorage getChunkStorage(long quota, long inactivityTimeout, ChunkInfoManager chunkInfoManager);

    protected abstract void emptyChunkStorage(ChunkStorage chunkStorage);

    @Test
    public void testInstantiatingChunkStorage() throws Exception {
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();

        ChunkStorage chunkStorage = getChunkStorage(65536, 10000, chunkInfoManager);

        assertNotNull(chunkStorage);
    }

    @Test
    public void testAddingGettingAndRemovingChunk() throws Exception {
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(65536, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        byte[] chunkData = new byte[1024];
        Random random = new Random();
        random.nextBytes(chunkData);
        String chunkID = sha256(chunkData);
        ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunkData);

        // Verify that the chunk does not already exist in the Chunk Storage
        boolean hasChunk = chunkStorage.hasChunk(chunkID);
        assertFalse(hasChunk);

        // Try to fetch the non-existent chunk and verify it fails.
        try {
            chunkStorage.getChunkDataStream(chunkID);
            fail("Expected Chunk to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
        try {
            chunkStorage.getChunkData(chunkID);
            fail("Expected Chunk to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }

        // Add the chunk to the chunk storage
        chunkStorage.addChunk(chunkID, chunkStream, null, true);

        // Verify that the chunk storage has the chunk we just stored
        hasChunk = chunkStorage.hasChunk(chunkID);
        assertTrue(hasChunk);

        // Fetch the stored chunk and verify that the right data is returned
        InputStream fetchStream = chunkStorage.getChunkDataStream(chunkID);
        byte[] fetchedData = new byte[chunkData.length];
        int bytesRead = fetchStream.read(fetchedData);
        assertEquals(chunkData.length, bytesRead);
        assertTrue(Arrays.equals(chunkData, fetchedData));
        assertTrue(Arrays.equals(chunkData, chunkStorage.getChunkData(chunkID)));

        fetchStream.close();

        // Remove the chunk from chunk storage and verify result
        boolean removed = chunkStorage.removeChunk(chunkID);
        assertTrue(removed);

        // Verify that the chunk storage no longer has the chunk
        hasChunk = chunkStorage.hasChunk(chunkID);
        assertFalse(hasChunk);

        // Try to fetch the remove chunk and verify it fails.
        try {
            chunkStorage.getChunkDataStream(chunkID);
            fail("Expected Chunk to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
        try {
            chunkStorage.getChunkData(chunkID);
            fail("Expected Chunk to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
    }


    @Test
    public void testAddingGettingAndRemovingMultipleChunk() throws Exception {
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(65536, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        byte[] chunkData1 = new byte[1024];
        Random random = new Random();
        random.nextBytes(chunkData1);
        String chunkID1 = sha256(chunkData1);
        ByteArrayInputStream chunkStream1 = new ByteArrayInputStream(chunkData1);

        byte[] chunkData2 = new byte[1024];
        random.nextBytes(chunkData1);
        String chunkID2 = chunkID1.substring(0, chunkID1.length() - 2) + "ff";
        ByteArrayInputStream chunkStream2 = new ByteArrayInputStream(chunkData2);

        // Verify that the chunk does not already exist in the Chunk Storage
        boolean hasChunk = chunkStorage.hasChunk(chunkID1);
        assertFalse(hasChunk);
        hasChunk = chunkStorage.hasChunk(chunkID2);
        assertFalse(hasChunk);

        // Try to fetch the non-existent chunk and verify it fails.
        try {
            chunkStorage.getChunkDataStream(chunkID1);
            fail("Expected Chunk 1 to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
        try {
            chunkStorage.getChunkData(chunkID1);
            fail("Expected Chunk 1 to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
        try {
            chunkStorage.getChunkDataStream(chunkID2);
            fail("Expected Chunk 2 to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
        try {
            chunkStorage.getChunkData(chunkID2);
            fail("Expected Chunk 2 to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }

        // Add the chunk 1 to the chunk storage
        chunkStorage.addChunk(chunkID1, chunkStream1, null, true);

        // Verify that the chunk storage has the chunk we just stored
        hasChunk = chunkStorage.hasChunk(chunkID1);
        assertTrue(hasChunk);
        hasChunk = chunkStorage.hasChunk(chunkID2);
        assertFalse(hasChunk);

        // Fetch the stored chunk and verify that the right data is returned
        InputStream fetchStream = chunkStorage.getChunkDataStream(chunkID1);
        byte[] fetchedData = new byte[chunkData1.length];
        int bytesRead = fetchStream.read(fetchedData);
        assertEquals(chunkData1.length, bytesRead);
        assertTrue(Arrays.equals(chunkData1, fetchedData));
        assertTrue(Arrays.equals(chunkData1, chunkStorage.getChunkData(chunkID1)));
        fetchStream.close();
        // Attempt to fetch Chunk 2 and verify it fails
        try {
            chunkStorage.getChunkDataStream(chunkID2);
            fail("Expected Chunk 2 to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
        try {
            chunkStorage.getChunkData(chunkID2);
            fail("Expected Chunk 2 to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }

        // Add the chunk 2 to the chunk storage
        chunkStorage.addChunk(chunkID2, chunkStream2, null, true);

        // Verify that the chunk storage has the chunk we just stored
        hasChunk = chunkStorage.hasChunk(chunkID1);
        assertTrue(hasChunk);
        hasChunk = chunkStorage.hasChunk(chunkID2);
        assertTrue(hasChunk);

        // Fetch the stored chunk 1 and verify that the right data is returned
        fetchStream = chunkStorage.getChunkDataStream(chunkID1);
        fetchedData = new byte[chunkData1.length];
        bytesRead = fetchStream.read(fetchedData);
        assertEquals(chunkData1.length, bytesRead);
        assertTrue(Arrays.equals(chunkData1, fetchedData));
        assertTrue(Arrays.equals(chunkData1, chunkStorage.getChunkData(chunkID1)));
        fetchStream.close();

        // Fetch the stored chunk 2 and verify that the right data is returned
        fetchStream = chunkStorage.getChunkDataStream(chunkID2);
        fetchedData = new byte[chunkData2.length];
        bytesRead = fetchStream.read(fetchedData);
        assertEquals(chunkData2.length, bytesRead);
        assertTrue(Arrays.equals(chunkData2, fetchedData));
        assertTrue(Arrays.equals(chunkData2, chunkStorage.getChunkData(chunkID2)));
        fetchStream.close();
        // Remove the chunk from chunk storage and verify result
        boolean removed = chunkStorage.removeChunk(chunkID1);
        assertTrue(removed);

        // Verify that the chunk storage no longer has the chunk
        hasChunk = chunkStorage.hasChunk(chunkID1);
        assertFalse(hasChunk);
        hasChunk = chunkStorage.hasChunk(chunkID2);
        assertTrue(hasChunk);

        // Try to fetch the remove chunk and verify it fails.
        try {
            chunkStorage.getChunkDataStream(chunkID1);
            fail("Expected Chunk to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
        try {
            chunkStorage.getChunkData(chunkID1);
            fail("Expected Chunk to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
    }


    @Test
    public void testAddingDuplicateChunk() throws Exception {
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(65536, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        byte[] chunkData = new byte[1024];
        Random random = new Random();
        random.nextBytes(chunkData);
        String chunkID = sha256(chunkData);
        ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunkData);

        // Add the chunk to the chunk storage
        chunkStorage.addChunk(chunkID, chunkStream, null, true);

        // Try to add the chunk again and verify it fails.
        try {
            chunkStorage.addChunk(chunkID, chunkStream, null, true);
            fail("Expected Chunk to not be found");
        } catch (DuplicateChunkException e) {
            // NOOP - Expected Exception
        }
    }


    @Test
    public void testStorageQuotaAndUsage() throws Exception {
        long quota = 65536;
        long available = quota;
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        byte[] chunkData = new byte[1024];
        Random random = new Random();
        random.nextBytes(chunkData);
        String chunkID = sha256(chunkData);
        ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunkData);

        // Check the storage quota and availability.
        assertEquals("Wrong Storage Quota", quota, chunkStorage.getStorageQuota());
        assertEquals("Wrong Available Storage", available, chunkStorage.getAvailableStorage());

        // Add the chunk to the chunk storage
        chunkStorage.addChunk(chunkID, chunkStream, null, true);
        available -= chunkData.length; // Available space is reduced by the size of the chunk.

        // Check the storage quota and availability.
        assertEquals("Wrong Storage Quota", quota, chunkStorage.getStorageQuota());
        assertEquals("Wrong Available Storage", available, chunkStorage.getAvailableStorage());

        // Remove the chunk from chunk storage
        chunkStorage.removeChunk(chunkID);
        available += chunkData.length; // Available space is increased by the size of the chunk.

        // Check the storage quota and availability.
        assertEquals("Wrong Storage Quota", quota, chunkStorage.getStorageQuota());
        assertEquals("Wrong Available Storage", available, chunkStorage.getAvailableStorage());

        // Reserve storage space
        ReservationID reservationID = chunkStorage.reserveStorageSpace(4096);
        available -= reservationID.getReservedSpace(); // Available space is decreased by the amount of the reservation.

        // Check the storage quota and availability.
        assertEquals("Wrong Storage Quota", quota, chunkStorage.getStorageQuota());
        assertEquals("Wrong Available Storage", available, chunkStorage.getAvailableStorage());

        // Add the chunk to the chunk storage
        chunkStream.reset();
        chunkStorage.addChunk(chunkID, chunkStream, reservationID, true);
        // The space consumed by this chunk is already accounted for in the reservation.

        // Check the storage quota and availability.
        assertEquals("Wrong Storage Quota", quota, chunkStorage.getStorageQuota());
        assertEquals("Wrong Available Storage", available, chunkStorage.getAvailableStorage());

        // Remove the chunk from chunk storage
        chunkStorage.removeChunk(chunkID);
        available += chunkData.length; // Available space is increased by the size of the chunk. The reservation has already accounted for the use of this data.

        // Check the storage quota and availability.
        assertEquals("Wrong Storage Quota", quota, chunkStorage.getStorageQuota());
        assertEquals("Wrong Available Storage", available, chunkStorage.getAvailableStorage());

    }



    @Test
    public void testStorageReservationLimit() throws Exception {
        long quota = 65536;
        long available = quota;
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Reserve some space
        ReservationID reservationID1 = chunkStorage.reserveStorageSpace(40000) ;

        assertTrue ( "Reservation ID should not be valid after release", reservationID1.isValid() ) ;
        assertEquals( 40000, reservationID1.getReservedSpace() ) ;
        assertEquals( 40000, reservationID1.getRemainingSpace() ) ;

        // Try to reserve some more space
        try {
            ReservationID reservationID2 = chunkStorage.reserveStorageSpace(40000);
            fail ( "Expected InsufficientSpaceException not thrown") ;
        } catch ( InsufficientSpaceException e ) {
            // NOOP
        }

        // Release the Reservation
        chunkStorage.releaseSpaceReservation(reservationID1);

        // Verify the reservation is no longer valid
        assertFalse ( "Reservation ID should not be valid after release", reservationID1.isValid() ) ;

        // Reserve some more space
        ReservationID reservationID2 = chunkStorage.reserveStorageSpace(40000) ;

        assertEquals( 40000, reservationID1.getReservedSpace() ) ;
        assertEquals( 40000, reservationID1.getRemainingSpace() ) ;
    }


    @Test
    public void testReservationCountsDataCorrectly() throws Exception {
        long quota = 65536;
        long reservation = 4096;
        long available = reservation;
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        Random random = new Random();
        int dataCount = 5;
        byte[][] chunkData = new byte[dataCount][];
        String[] chunkIDs = new String[dataCount];
        ByteArrayInputStream[] chunkStreams = new ByteArrayInputStream[dataCount];

        for ( int i = 0 ; i < dataCount ; i++ ) {
            chunkData[i] = new byte[1024];
            random.nextBytes(chunkData[i]);
            chunkIDs[i] = sha256(chunkData[i]);
            chunkStreams[i] = new ByteArrayInputStream(chunkData[i]);

        }



        // Reserve some storage
        ReservationID reservationID = chunkStorage.reserveStorageSpace(reservation);

        // Verify the reserved space and available space in the reservation are correct
        assertEquals(reservation, reservationID.getReservedSpace());
        assertEquals(available, reservationID.getRemainingSpace());

        // We expect 4 chunks to be able to be saved against the reservation
        for ( int curIndex = 0 ; curIndex < 4 ; curIndex++ ) {
            // Store some data against the reservation
            chunkStorage.addChunk(chunkIDs[curIndex], chunkStreams[curIndex], reservationID, true);
            available -= chunkData[curIndex].length;

            // Verify the reserved space and available space in the reservation are correct
            assertEquals(reservation, reservationID.getReservedSpace());
            assertEquals(available, reservationID.getRemainingSpace());

        }

        // The 5th chunk exceeds our reservation and cannot be added using the reservation
        try {
            chunkStorage.addChunk(chunkIDs[4], chunkStreams[4], reservationID, true);
            fail ( "Expected InsufficientSpaceException not thrown") ;
        } catch ( InsufficientSpaceException e ) {
            // NOOP - Expected Exception
        }
    }


    @Test
    public void testReleaseReservationWithUnrecognizedReservation() throws Exception {
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(65536, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        ReservationID unrecognizedReservationID = new FileSystemChunkStorage.FSReservationID("fakeID", 100, 10000) ;

        try {
            chunkStorage.releaseSpaceReservation(unrecognizedReservationID);
            fail("The expected InvalidReservationException not thrown");
        } catch ( InvalidReservationException e ) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testReleaseReservationWithWrongKindOfReservation() throws Exception {
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(65536, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        ReservationID unrecognizedReservationID = new MockReservationID();

        try {
            chunkStorage.releaseSpaceReservation(unrecognizedReservationID);
            fail("The expected InvalidReservationException not thrown");
        } catch ( InvalidReservationException e ) {
            // NOOP - Expected Exception
        }
    }


    @Test
    public void testGetAvailableStorageWithExpiredReservation() throws Exception {
        int quota = 65536;
        int spaceToReserve = 1000;

        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 1, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        ReservationID reservationID = chunkStorage.reserveStorageSpace(spaceToReserve) ;

        long availableStorage = chunkStorage.getAvailableStorage() ;

        assertEquals ( quota - spaceToReserve, availableStorage ) ;

        Thread.sleep ( 2 ) ;

        // After the reservation has expired, check available storage again.
        availableStorage = chunkStorage.getAvailableStorage() ;

        assertEquals ( quota, availableStorage ) ;
    }

    @Test
    public void testPurgeStorageWithSufficientSpace() throws Exception {
        long quota = 4096;
        int dataSize = 1000 ;
        int neededAvailableSpace = 64;
        int dataCount = 5;
        long available = quota;

        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        Random random = new Random();
        byte[][] chunkData = new byte[dataCount][];
        String[] chunkIDs = new String[dataCount];
        ByteArrayInputStream[] chunkStreams = new ByteArrayInputStream[dataCount];

        for ( int i = 0 ; i < dataCount ; i++ ) {
            chunkData[i] = new byte[dataSize];
            random.nextBytes(chunkData[i]);
            chunkIDs[i] = sha256(chunkData[i]);
            chunkStreams[i] = new ByteArrayInputStream(chunkData[i]);
        }

        // We expect 4 chunks to be able to be saved against the reservation
        for ( int curIndex = 0 ; curIndex < 4 ; curIndex++ ) {
            // Store some data against the reservation
            chunkStorage.addChunk(chunkIDs[curIndex], chunkStreams[curIndex], null, true);
            available -= chunkData[curIndex].length ;
        }

        assertEquals ( available, chunkStorage.getAvailableStorage()) ;

        boolean purgeSuccessful = chunkStorage.purgeStorage(neededAvailableSpace) ;

        assertTrue ("Purging for space should have succeeded", purgeSuccessful) ;
        assertTrue ( chunkStorage.getAvailableStorage() > neededAvailableSpace ) ;
    }

    @Test
    public void testPurgeStorageWithInsufficientQuota() throws Exception {
        long quota = 1000;
        int neededAvailableSpace = 2000 ;
        long available = quota;

        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        assertEquals ( available, chunkStorage.getAvailableStorage()) ;

        boolean purgeSuccessful = chunkStorage.purgeStorage(neededAvailableSpace) ;

        assertFalse ("Purging for space should have failed", purgeSuccessful) ;
    }

    @Test
    public void testPurgeStorageCanMakeSpace() throws Exception {
        long quota = 4096;
        int dataSize = 1000 ;
        int neededAvailableSpace = 2000;
        int dataCount = 5;
        long available = quota;

        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        Random random = new Random();
        byte[][] chunkData = new byte[dataCount][];
        String[] chunkIDs = new String[dataCount];
        ByteArrayInputStream[] chunkStreams = new ByteArrayInputStream[dataCount];

        for ( int i = 0 ; i < dataCount ; i++ ) {
            chunkData[i] = new byte[dataSize];
            random.nextBytes(chunkData[i]);
            chunkIDs[i] = sha256(chunkData[i]);
            chunkStreams[i] = new ByteArrayInputStream(chunkData[i]);
        }

        // We expect 4 chunks to be able to be saved against the reservation
        for ( int curIndex = 0 ; curIndex < 4 ; curIndex++ ) {
            // Store some data against the reservation
            chunkStorage.addChunk(chunkIDs[curIndex], chunkStreams[curIndex], null, true);
            available -= chunkData[curIndex].length ;
            Thread.sleep(1000) ;
        }

        assertEquals ( available, chunkStorage.getAvailableStorage()) ;

        boolean purgeSuccessful = chunkStorage.purgeStorage(neededAvailableSpace) ;

        assertTrue ("Purging for space should have succeeded", purgeSuccessful) ;
        assertTrue ( chunkStorage.getAvailableStorage() > neededAvailableSpace ) ;
    }

    @Test
    public void testPurgeStorageWithUnpurgeableDataFails() throws Exception {
        long quota = 4096;
        int dataSize = 1000 ;
        int neededAvailableSpace = 2000;
        int dataCount = 5;
        long available = quota;

        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        Random random = new Random();
        byte[][] chunkData = new byte[dataCount][];
        String[] chunkIDs = new String[dataCount];
        ByteArrayInputStream[] chunkStreams = new ByteArrayInputStream[dataCount];

        for ( int i = 0 ; i < dataCount ; i++ ) {
            chunkData[i] = new byte[dataSize];
            random.nextBytes(chunkData[i]);
            chunkIDs[i] = sha256(chunkData[i]);
            chunkStreams[i] = new ByteArrayInputStream(chunkData[i]);
        }

        // We expect 4 chunks to be able to be saved against the reservation
        for ( int curIndex = 0 ; curIndex < 4 ; curIndex++ ) {
            // Store some data against the reservation
            chunkStorage.addChunk(chunkIDs[curIndex], chunkStreams[curIndex], null, false);
            available -= chunkData[curIndex].length ;
            chunkInfoManager.addUnpurgeableChunk(chunkIDs[curIndex]) ;
        }

        assertEquals ( available, chunkStorage.getAvailableStorage()) ;

        boolean purgeSuccessful = chunkStorage.purgeStorage(neededAvailableSpace) ;

        assertFalse ("Purging for space should have failed", purgeSuccessful) ;
        assertEquals ( available, chunkStorage.getAvailableStorage()) ;
    }


    @Test
    public void testAddChunkWithUnrecognizedReservation() throws Exception {
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(65536, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        byte[] chunkData = new byte[1024];
        Random random = new Random();
        random.nextBytes(chunkData);
        String chunkID = sha256(chunkData);
        ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunkData);

        ReservationID unrecognizedReservationID = new FileSystemChunkStorage.FSReservationID("fakeID", 100, 10000) ;

        try {
            chunkStorage.addChunk(chunkID, chunkStream, unrecognizedReservationID, true);
            fail("The expected InvalidReservationException not thrown");
        } catch ( InvalidReservationException e ) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testAddChunkWithWrongKindOfReservation() throws Exception {
        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(65536, 10000, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        byte[] chunkData = new byte[1024];
        Random random = new Random();
        random.nextBytes(chunkData);
        String chunkID = sha256(chunkData);
        ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunkData);

        ReservationID unrecognizedReservationID = new MockReservationID() ;

        try {
            chunkStorage.addChunk(chunkID, chunkStream, unrecognizedReservationID, true);
            fail("The expected InvalidReservationException not thrown");
        } catch ( InvalidReservationException e ) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testAddChunkWithExpiredReservation() throws Exception {
        int quota = 65536;
        int spaceToReserve = 2048;

        InMemoryChunkInfoManager chunkInfoManager = new InMemoryChunkInfoManager();
        ChunkStorage chunkStorage = getChunkStorage(quota, 1, chunkInfoManager);
        emptyChunkStorage(chunkStorage);

        // Create the Test data
        byte[] chunkData = new byte[1024];
        Random random = new Random();
        random.nextBytes(chunkData);
        String chunkID = sha256(chunkData);
        ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunkData);

        ReservationID reservationID = chunkStorage.reserveStorageSpace(spaceToReserve) ;

        Thread.sleep ( 10 ) ;

        assertFalse ( reservationID.isValid() ) ;

        // After the reservation has expired, check available storage again.
        try {
            chunkStorage.addChunk(chunkID, chunkStream, reservationID, true);
            fail ( "Expected InvalidReservationException Not Thrown" ) ;
        } catch ( InvalidReservationException e ) {
            // NOOP - Expected Exception
        }
    }


    // -------- Private Methods --------

    private String sha256(byte[] data) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(data);

        String shaString = Hex.encodeHexString(hash);

        return shaString;
    }


    // -------- Test Reservation ID Class --------

    private static class MockReservationID implements ReservationID {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public long getExpirationTime() {
            return 0;
        }

        @Override
        public long getReservedSpace() {
            return 0;
        }

        @Override
        public long getRemainingSpace() {
            return 0;
        }
    }
}
