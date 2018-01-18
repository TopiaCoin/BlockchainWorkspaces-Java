package io.topiacoin.chainmail.exceptions;

public class ChainNotRunningException extends Exception {
    public ChainNotRunningException() {
    }

    public ChainNotRunningException(String message) {
        super(message);
    }

    public ChainNotRunningException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChainNotRunningException(Throwable cause) {
        super(cause);
    }
}
