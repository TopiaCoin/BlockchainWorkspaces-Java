package io.topiacoin.chunks.exceptions;

public class InsufficientSpaceException extends Exception {
    /**
     * Constructs a new throwable with the specified detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     * <p>
     * <p>The {@link #fillInStackTrace()} method is called to initialize the stack trace data in the newly created
     * throwable.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     */
    public InsufficientSpaceException(String message) {
        super(message);
    }

    /**
     * Constructs a new throwable with the specified detail message and cause.  <p>Note that the detail message associated
     * with {@code cause} is <i>not</i> automatically incorporated in this throwable's detail message.
     * <p>
     * <p>The {@link #fillInStackTrace()} method is called to initialize the stack trace data in the newly created
     * throwable.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code null}
     *                value is permitted, and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.4
     */
    public InsufficientSpaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
