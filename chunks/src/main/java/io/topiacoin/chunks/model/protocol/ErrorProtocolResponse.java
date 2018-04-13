package io.topiacoin.chunks.model.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class ErrorProtocolResponse implements ProtocolMessage {
	private String _userID;
	private String _errorMessage;
	private byte[] _signature = null;
	private final boolean _isRequest = false;
	private final String _messageType = "ERROR";

	public ErrorProtocolResponse(String errorMessage, String userID) {
		_errorMessage = errorMessage;
		_userID = userID;
	}

	public ErrorProtocolResponse() {
		this(null, null);
	}

	@Override public void sign(PrivateKey signingKey) throws InvalidKeyException {
		ByteBuffer msgBytes = toBytes(false);
		Signature sig;
		try {
			sig = Signature.getInstance("SHA1withECDSA");
			sig.initSign(signingKey);
			sig.update(msgBytes);
			_signature = sig.sign();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed to init signature", e);
		} catch (SignatureException e) {
			throw new RuntimeException("Failed to sign", e);
		}
	}

	@Override public boolean verify(PublicKey senderPublicKey) throws InvalidKeyException {
		if(_signature == null) {
			return false;
		} else {
			ByteBuffer messageData = toBytes(false);
			Signature sig;
			try {
				sig = Signature.getInstance("SHA1withECDSA");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("Failed to init signature", e);
			}
			sig.initVerify(senderPublicKey);
			try {
				sig.update(messageData);
				return sig.verify(_signature);
			} catch (SignatureException e) {
				throw new RuntimeException("Failed to verify", e);
			}
		}
	}

	@Override public ByteBuffer toBytes() {
		return toBytes(true);
	}

	private ByteBuffer toBytes(boolean includeSig) {
		//The message is the
		// userID length, an int
		// userID, a UTF-8 String whose bytelength is userID length
		// error message length, an int
		// error message, a UTF-8 String whose bytelength is error message length
		// signature length, an int
		// signature bytes, an array of bytes whose length is signature length
		//Figure out how large of a byte buffer we need to allocate:
		int toAlloc = 0;
		toAlloc += Integer.BYTES; //UserID length
		toAlloc += _userID.getBytes(Charset.forName("UTF-8")).length; //userID
		toAlloc += Integer.BYTES; //error message length
		toAlloc += _errorMessage.getBytes(Charset.forName("UTF-8")).length; //error message
		toAlloc += Integer.BYTES; //signature length
		if(includeSig) {
			toAlloc += _signature == null ? 0 : _signature.length; //signature bytes
		}

		//There, now allocate the buffer and put the stuff in
		ByteBuffer toReturn = ByteBuffer.allocate(toAlloc);
		byte[] userIDBytes = _userID.getBytes(Charset.forName("UTF-8"));
		toReturn.putInt(userIDBytes.length); //UserID length
		toReturn.put(userIDBytes); //UserID
		byte[] errorMsgBytes = _errorMessage.getBytes(Charset.forName("UTF-8"));
		toReturn.putInt(errorMsgBytes.length); //UserID length
		toReturn.put(errorMsgBytes); //UserID
		toReturn.putInt(_signature == null ? 0 : _signature.length); //signature length
		if(includeSig && _signature != null) {
			toReturn.put(_signature); //signature bytes
		}
		return toReturn;
	}


	@Override public void fromBytes(ByteBuffer bytes) {
		int userIdLength = bytes.getInt();
		if(userIdLength > 0) {
			byte[] userIDBytes = new byte[userIdLength];
			bytes.get(userIDBytes);
			_userID = new String(userIDBytes, Charset.forName("UTF-8"));
		}
		int errorLength = bytes.getInt();
		if(errorLength > 0) {
			byte[] errorBytes = new byte[errorLength];
			bytes.get(errorBytes);
			_errorMessage = new String(errorBytes, Charset.forName("UTF-8"));
		}
		int signatureLength = bytes.getInt();
		if(signatureLength > 0) {
			_signature = new byte[signatureLength];
			bytes.get(_signature);
		}
	}

	@Override public boolean isValid() {
		return _userID != null && _errorMessage != null && !_errorMessage.isEmpty();
	}

	@Override public boolean isRequest() {
		return _isRequest;
	}

	@Override public String getType() {
		return _messageType;
	}

	public String getUserID() {
		return _userID;
	}

	public String getErrorMessage() {
		return _errorMessage;
	}
}
