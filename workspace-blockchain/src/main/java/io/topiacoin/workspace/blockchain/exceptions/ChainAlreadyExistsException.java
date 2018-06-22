package io.topiacoin.workspace.blockchain.exceptions;

public class ChainAlreadyExistsException extends Exception {
	public ChainAlreadyExistsException() {
	}

	public ChainAlreadyExistsException(String message) {
		super(message);
	}

	public ChainAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChainAlreadyExistsException(Throwable cause) {
		super(cause);
	}
}
