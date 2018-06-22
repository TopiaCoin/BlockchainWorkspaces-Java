package io.topiacoin.workspace.blockchain.exceptions;

public class NoSuchChainException extends Exception {
	public NoSuchChainException() {
	}

	public NoSuchChainException(String message) {
		super(message);
	}

	public NoSuchChainException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoSuchChainException(Throwable cause) {
		super(cause);
	}
}
