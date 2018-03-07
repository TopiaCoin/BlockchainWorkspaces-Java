package io.topiacoin.model;

import javax.crypto.SecretKey;
import java.util.Arrays;

public class FileChunk {

    private String chunkID;
    private long index;
    private long cipherTextSize;
    private long clearTextSize;
    private SecretKey chunkKey;
    private byte[] initializationVector;
    private byte[] cipherTextHash;
    private byte[] clearTextHash;
    private String compressionAlgorithm;

    public FileChunk() {
    }

    public FileChunk(String chunkID, long index, long cipherTextSize, long clearTextSize, SecretKey chunkKey, byte[] initializationVector, byte[] cipherTextHash, byte[] clearTextHash, String compressionAlgorithm) {
        this.chunkID = chunkID;
        this.index = index;
        this.cipherTextSize = cipherTextSize;
        this.clearTextSize = clearTextSize;
        this.chunkKey = chunkKey;
        this.initializationVector = initializationVector;
        this.cipherTextHash = cipherTextHash;
        this.clearTextHash = clearTextHash;
        this.compressionAlgorithm = compressionAlgorithm;
    }

    public String getChunkID() {
        return chunkID;
    }

    public void setChunkID(String chunkID) {
        this.chunkID = chunkID;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getCipherTextSize() {
        return cipherTextSize;
    }

    public void setCipherTextSize(long cipherTextSize) {
        this.cipherTextSize = cipherTextSize;
    }

    public long getClearTextSize() {
        return clearTextSize;
    }

    public void setClearTextSize(long clearTextSize) {
        this.clearTextSize = clearTextSize;
    }

    public SecretKey getChunkKey() {
        return chunkKey;
    }

    public void setChunkKey(SecretKey chunkKey) {
        this.chunkKey = chunkKey;
    }

    public byte[] getInitializationVector() {
        return initializationVector;
    }

    public void setInitializationVector(byte[] initializationVector) {
        this.initializationVector = initializationVector;
    }

    public byte[] getCipherTextHash() {
        return cipherTextHash;
    }

    public void setCipherTextHash(byte[] cipherTextHash) {
        this.cipherTextHash = cipherTextHash;
    }

    public byte[] getClearTextHash() {
        return clearTextHash;
    }

    public void setClearTextHash(byte[] clearTextHash) {
        this.clearTextHash = clearTextHash;
    }

    public String getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public void setCompressionAlgorithm(String compressionAlgorithm) {
        this.compressionAlgorithm = compressionAlgorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileChunk fileChunk = (FileChunk) o;

        if (index != fileChunk.index) return false;
        if (cipherTextSize != fileChunk.cipherTextSize) return false;
        if (clearTextSize != fileChunk.clearTextSize) return false;
        if (chunkID != null ? !chunkID.equals(fileChunk.chunkID) : fileChunk.chunkID != null) return false;
        if (chunkKey != null ? !chunkKey.equals(fileChunk.chunkKey) : fileChunk.chunkKey != null) return false;
        if (!Arrays.equals(initializationVector, fileChunk.initializationVector)) return false;
        if (!Arrays.equals(cipherTextHash, fileChunk.cipherTextHash)) return false;
        if (!Arrays.equals(clearTextHash, fileChunk.clearTextHash)) return false;
        return compressionAlgorithm != null ? compressionAlgorithm.equals(fileChunk.compressionAlgorithm) : fileChunk.compressionAlgorithm == null;
    }

    @Override
    public int hashCode() {
        int result = chunkID != null ? chunkID.hashCode() : 0;
        result = 31 * result + (int) (index ^ (index >>> 32));
        result = 31 * result + (int) (cipherTextSize ^ (cipherTextSize >>> 32));
        result = 31 * result + (int) (clearTextSize ^ (clearTextSize >>> 32));
        result = 31 * result + (chunkKey != null ? chunkKey.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(initializationVector);
        result = 31 * result + Arrays.hashCode(cipherTextHash);
        result = 31 * result + Arrays.hashCode(clearTextHash);
        result = 31 * result + (compressionAlgorithm != null ? compressionAlgorithm.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FileChunks{" +
                "chunkID='" + chunkID + '\'' +
                ", index=" + index +
                ", cipherTextSize=" + cipherTextSize +
                ", clearTextSize=" + clearTextSize +
                ", chunkKey=" + chunkKey +
                ", initializationVector=" + Arrays.toString(initializationVector) +
                ", cipherTextHash=" + Arrays.toString(cipherTextHash) +
                ", clearTextHash=" + Arrays.toString(clearTextHash) +
                ", compressionAlgorithm='" + compressionAlgorithm + '\'' +
                '}';
    }
}
