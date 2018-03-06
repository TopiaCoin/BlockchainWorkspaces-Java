package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import javafx.scene.shape.Mesh;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

import static junit.framework.TestCase.*;

public abstract class AbstractChunkStorageTest {

    protected abstract ChunkStorage getChunkStorage();

    protected abstract void emptyChunkStorage(ChunkStorage chunkStorage);

    @Test
    public void testInstantiatingChunkStorage() throws Exception {

        ChunkStorage chunkStorage = getChunkStorage();

        assertNotNull(chunkStorage);
    }

    @Test
    public void testAddingGettingAndRemovingChunk() throws Exception {
        ChunkStorage chunkStorage = getChunkStorage();
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
        InputStream fetchStream = chunkStorage.getChunkData(chunkID);
        byte[] fetchedData = new byte[chunkData.length];
        int bytesRead = fetchStream.read(fetchedData);
        assertEquals(chunkData.length, bytesRead);
        assertTrue(Arrays.equals(chunkData, fetchedData));

        // Remove the chunk from chunk storage and verify result
        boolean removed = chunkStorage.removeChunk(chunkID);
        assertTrue(removed);

        // Verify that the chunk storage no longer has the chunk
        hasChunk = chunkStorage.hasChunk(chunkID);
        assertFalse(hasChunk);

        // Try to fetch the remove chunk and verify it fails.
        try {
            chunkStorage.getChunkData(chunkID);
            fail("Expected Chunk to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
    }


    @Test
    public void testAddingGettingAndRemovingMultipleChunk() throws Exception {
        ChunkStorage chunkStorage = getChunkStorage();
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
            chunkStorage.getChunkData(chunkID1);
            fail("Expected Chunk 1 to not be found");
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
        InputStream fetchStream = chunkStorage.getChunkData(chunkID1);
        byte[] fetchedData = new byte[chunkData1.length];
        int bytesRead = fetchStream.read(fetchedData);
        assertEquals(chunkData1.length, bytesRead);
        assertTrue(Arrays.equals(chunkData1, fetchedData));

        // Attempt to fetch Chunk 2 and verify it fails
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
        fetchStream = chunkStorage.getChunkData(chunkID1);
        fetchedData = new byte[chunkData1.length];
        bytesRead = fetchStream.read(fetchedData);
        assertEquals(chunkData1.length, bytesRead);
        assertTrue(Arrays.equals(chunkData1, fetchedData));

        // Fetch the stored chunk 2 and verify that the right data is returned
        fetchStream = chunkStorage.getChunkData(chunkID2);
        fetchedData = new byte[chunkData2.length];
        bytesRead = fetchStream.read(fetchedData);
        assertEquals(chunkData2.length, bytesRead);
        assertTrue(Arrays.equals(chunkData2, fetchedData));

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
            chunkStorage.getChunkData(chunkID1);
            fail("Expected Chunk to not be found");
        } catch (NoSuchChunkException e) {
            // NOOP - Expected Exception
        }
    }


    @Test
    public void testAddingDuplicateChunk() throws Exception {
        ChunkStorage chunkStorage = getChunkStorage();
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


    // -------- Private Methods --------

    private String sha256(byte[] data) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(data);

        String shaString = Hex.encodeHexString(hash);

        return shaString;
    }
}
