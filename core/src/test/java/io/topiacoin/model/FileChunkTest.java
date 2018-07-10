package io.topiacoin.model;

import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.*;

public class FileChunkTest {

    @Test
    public void testDefaultConstructor() throws Exception {

        FileChunk fileChunk = new FileChunk();

        assertNull(fileChunk.getChunkID());
        assertEquals(0, fileChunk.getIndex());
        assertEquals(0, fileChunk.getCipherTextSize());
        assertEquals(0, fileChunk.getClearTextSize());
        assertNull(fileChunk.getInitializationVector());
        assertNull(fileChunk.getChunkKey());
        assertNull(fileChunk.getCipherTextHash());
        assertNull(fileChunk.getClearTextHash());
        assertNull(fileChunk.getCompressionAlgorithm());
    }

    @Test
    public void testConstructor() throws Exception {

        byte[] keyBytes = new byte[16];

        String chunkID = "foo-bar-baz-fizz-buzz";
        long index = 1;
        long cipherTextSize = 654321;
        long clearTextSize = 1234567;
        SecretKey chunkKey = new SecretKeySpec(keyBytes, "AES");
        byte[] iv = new byte[16];
        String cipherTextHash = "SHA-256:AAAAAAAAAAAAAAAA";
        String clearTextHash = "SHA-256:AAAAAAAAAAAAAAAA";
        String compressionAlgorithm = "GZIP";

        FileChunk fileChunk = new FileChunk(chunkID, index, cipherTextSize, clearTextSize, chunkKey, iv, cipherTextHash, clearTextHash, compressionAlgorithm);

        assertEquals(chunkID, fileChunk.getChunkID());
        assertEquals(index, fileChunk.getIndex());
        assertEquals(cipherTextSize, fileChunk.getCipherTextSize());
        assertEquals(clearTextSize, fileChunk.getClearTextSize());
        assertEquals(chunkKey, fileChunk.getChunkKey());
        assertEquals(iv, fileChunk.getInitializationVector());
        assertEquals(cipherTextHash, fileChunk.getCipherTextHash());
        assertEquals(clearTextHash, fileChunk.getClearTextHash());
        assertEquals(compressionAlgorithm, fileChunk.getCompressionAlgorithm());
    }


    @Test
    public void testBasicAccessors() throws Exception {

        byte[] keyBytes = new byte[16];

        String chunkID = "foo-bar-baz-fizz-buzz";
        long index = 1;
        long cipherTextSize = 654321;
        long clearTextSize = 1234567;
        SecretKey chunkKey = new SecretKeySpec(keyBytes, "AES");
        byte[] iv = new byte[16];
        String cipherTextHash = "SHA-256:AAAAAAAAAAAAAAAA";
        String clearTextHash = "SHA-256:AAAAAAAAAAAAAAAA";
        String compressionAlgorithm = "GZIP";

        FileChunk fileChunk = new FileChunk();

        // Check ChunkID Accessors
        assertNull(fileChunk.getChunkID());
        fileChunk.setChunkID(chunkID);
        assertEquals(chunkID, fileChunk.getChunkID());
        fileChunk.setChunkID(null);
        assertNull(fileChunk.getChunkID());

        // Check Index Accessors
        assertEquals(0, fileChunk.getIndex());
        fileChunk.setIndex(index);
        assertEquals(index, fileChunk.getIndex());
        fileChunk.setIndex(0);
        assertEquals(0, fileChunk.getIndex());

        // Check Cipher Text Size Accessors
        assertEquals(0, fileChunk.getCipherTextSize());
        fileChunk.setCipherTextSize(cipherTextSize);
        assertEquals(cipherTextSize, fileChunk.getCipherTextSize());
        fileChunk.setCipherTextSize(0);
        assertEquals(0, fileChunk.getCipherTextSize());

        // Check Clear Text Size Accessors
        assertEquals(0, fileChunk.getClearTextSize());
        fileChunk.setClearTextSize(clearTextSize);
        assertEquals(clearTextSize, fileChunk.getClearTextSize());
        fileChunk.setClearTextSize(0);
        assertEquals(0, fileChunk.getClearTextSize());

        // Check Initialization Vector Accessors
        assertNull(fileChunk.getInitializationVector());
        fileChunk.setInitializationVector(iv);
        assertEquals(iv, fileChunk.getInitializationVector());
        fileChunk.setInitializationVector(null);
        assertNull(fileChunk.getInitializationVector());

        // Check Chunk Key Accessors
        assertNull(fileChunk.getChunkKey());
        fileChunk.setChunkKey(chunkKey);
        assertEquals(chunkKey, fileChunk.getChunkKey());
        fileChunk.setChunkKey(null);
        assertNull(fileChunk.getChunkKey());

        // Check Cipher Text Hash Accessors
        assertNull(fileChunk.getCipherTextHash());
        fileChunk.setCipherTextHash(cipherTextHash);
        assertEquals(cipherTextHash, fileChunk.getCipherTextHash());
        fileChunk.setCipherTextHash(null);
        assertNull(fileChunk.getCipherTextHash());

        // Check Clear Text Hash Accessors
        assertNull(fileChunk.getClearTextHash());
        fileChunk.setClearTextHash(clearTextHash);
        assertEquals(clearTextHash, fileChunk.getClearTextHash());
        fileChunk.setClearTextHash(null);
        assertNull(fileChunk.getClearTextHash());

        // Check Compression Algorithm Accessors
        assertNull(fileChunk.getCompressionAlgorithm());
        fileChunk.setCompressionAlgorithm(compressionAlgorithm);
        assertEquals(compressionAlgorithm, fileChunk.getCompressionAlgorithm());
        fileChunk.setCompressionAlgorithm(null);
        assertNull(fileChunk.getCompressionAlgorithm());

    }


    @Test
    public void testEqualsAndHashCode() throws Exception {

        byte[] keyBytes = new byte[16];

        String chunkID = "foo-bar-baz-fizz-buzz";
        long index = 1;
        long cipherTextSize = 654321;
        long clearTextSize = 1234567;
        SecretKey chunkKey = new SecretKeySpec(keyBytes, "AES");
        byte[] iv = new byte[16];
        String cipherTextHash = "SHA-256:AAAAAAAAAAAAAAAA";
        String clearTextHash = "SHA-256:AAAAAAAAAAAAAAAA";
        String compressionAlgorithm = "GZIP";

        FileChunk fileChunk1 = new FileChunk(chunkID, index, cipherTextSize, clearTextSize, chunkKey, iv, cipherTextHash, clearTextHash, compressionAlgorithm);
        FileChunk fileChunk2 = new FileChunk(chunkID, index, cipherTextSize, clearTextSize, chunkKey, iv, cipherTextHash, clearTextHash, compressionAlgorithm);

        assertEquals(fileChunk1, fileChunk1);
        assertEquals(fileChunk2, fileChunk2);
        assertEquals(fileChunk1, fileChunk2);
        assertEquals(fileChunk2, fileChunk1);

        assertEquals(fileChunk1.hashCode(), fileChunk2.hashCode());

    }


    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {

        FileChunk fileChunk1 = new FileChunk();
        FileChunk fileChunk2 = new FileChunk();

        assertEquals(fileChunk1, fileChunk1);
        assertEquals(fileChunk2, fileChunk2);
        assertEquals(fileChunk1, fileChunk2);
        assertEquals(fileChunk2, fileChunk1);

        assertEquals(fileChunk1.hashCode(), fileChunk2.hashCode());

    }


}
