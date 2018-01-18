package io.topiacoin.core.exceptions;

public class ChunkNotFoundException extends Exception {
    public ChunkNotFoundException() {
    }

    public ChunkNotFoundException(String message) {
        super(message);
    }

    public ChunkNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChunkNotFoundException(Throwable cause) {
        super(cause);
    }
}
