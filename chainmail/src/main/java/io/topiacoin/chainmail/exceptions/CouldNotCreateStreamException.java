package io.topiacoin.chainmail.exceptions;

public class CouldNotCreateStreamException extends Exception {
    public CouldNotCreateStreamException() {
    }

    public CouldNotCreateStreamException(String message) {
        super(message);
    }

    public CouldNotCreateStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public CouldNotCreateStreamException(Throwable cause) {
        super(cause);
    }
}
