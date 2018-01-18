package io.topiacoin.chainmail.exceptions;

public class FailedToStartChainException extends Exception {
    public FailedToStartChainException() {
    }

    public FailedToStartChainException(String message) {
        super(message);
    }

    public FailedToStartChainException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedToStartChainException(Throwable cause) {
        super(cause);
    }
}
