package io.topiacoin.core.exceptions;

public class UnableToCreateUserException extends Exception{

    public UnableToCreateUserException() {
    }

    public UnableToCreateUserException(String message) {
        super(message);
    }

    public UnableToCreateUserException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnableToCreateUserException(Throwable cause) {
        super(cause);
    }
}
