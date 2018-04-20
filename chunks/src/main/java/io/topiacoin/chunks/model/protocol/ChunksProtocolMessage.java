package io.topiacoin.chunks.model.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class ChunksProtocolMessage implements ProtocolMessage {
	String _userID;
	String[] _chunks;
	public byte[] _signature = null;
	private String _authToken;
	private boolean _isRequest;
	private String _messageType;

	ChunksProtocolMessage(String[] chunksRequired, String userID, String authToken, boolean isRequest, String messageType) {
		_chunks = chunksRequired;
		_userID = userID;
		_authToken = authToken;
		_isRequest = isRequest;
		_messageType = messageType;
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

	@Override public boolean verify(PublicKey senderPublicKey) throws InvalidKeyException, SignatureException {
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
			sig.update(messageData);
			return sig.verify(_signature);
		}
	}

	@Override public ByteBuffer toBytes() {
		return toBytes(true);
	}

	private ByteBuffer toBytes(boolean includeSig) {
		//The message is the
		// userID length, an int
		// userID, a UTF-8 String whose bytelength is userID length
		// chunk count, an int
		// chunk0 ID length, an int
		// chunk0 ID, a UTF-8 String whose bytelength is chunk0 length
		// chunk1 ID length, an int
		// chunk1 ID, a UTF-8 String whose bytelength is chunk1 length
		// ...
		// chunkn (where n is chunk count -1) ID length, an int
		// chunkn (where n is chunk count -1) ID, a UTF-8 String whose bytelength is chunkn length
		// signature length, an int
		// signature bytes, an array of bytes whose length is signature length
		// authToken length, an int
		// authToken, a UTF-8 String whose bytelength is authToken length
		// So that's 3 + chunk count ints...oh whatever, just look at the code.
		//Figure out how large of a byte buffer we need to allocate:
		int toAlloc = 0;
		toAlloc += Integer.BYTES; //UserID length
		toAlloc += _userID.getBytes(Charset.forName("UTF-8")).length; //userID
		toAlloc += Integer.BYTES; //chunk count
		for(String chunk : _chunks) {
			toAlloc += Integer.BYTES; //every chunkx length
			toAlloc += chunk.getBytes(Charset.forName("UTF-8")).length; //Every chunkx ID length
		}
		toAlloc += Integer.BYTES; //signature length
		if(includeSig) {
			toAlloc += _signature == null ? 0 : _signature.length; //signature bytes
		}
		toAlloc += Integer.BYTES; //authToken length
		if(_authToken != null) {
			toAlloc += _authToken.getBytes(Charset.forName("UTF-8")).length; //authToken
		}

		//There, now allocate the buffer and put the stuff in
		ByteBuffer toReturn = ByteBuffer.allocate(toAlloc);
		byte[] userIDBytes = _userID.getBytes(Charset.forName("UTF-8"));
		toReturn.putInt(userIDBytes.length); //UserID length
		toReturn.put(userIDBytes); //UserID
		toReturn.putInt(_chunks.length); //Chunk count
		for(String chunk : _chunks) {
			byte[] chunkBytes = chunk.getBytes(Charset.forName("UTF-8"));
			toReturn.putInt(chunkBytes.length); //chunkX ID length
			toReturn.put(chunkBytes); //chunkX ID
		}
		toReturn.putInt(_signature == null ? 0 : _signature.length); //signature length
		if(includeSig && _signature != null) {
			toReturn.put(_signature); //signature bytes
		}
		if(_authToken != null) {
			byte[] authTokenBytes = _authToken.getBytes(Charset.forName("UTF-8"));
			toReturn.putInt(authTokenBytes.length);
			toReturn.put(authTokenBytes);
		} else {
			toReturn.putInt(0);
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
		int chunkCount = bytes.getInt();
		_chunks = new String[chunkCount];
		for(int i = 0; i < _chunks.length; i++) {
			int chunkxLength = bytes.getInt();
			byte[] chunkxBytes = new byte[chunkxLength];
			bytes.get(chunkxBytes);
			_chunks[i] = new String(chunkxBytes, Charset.forName("UTF-8"));
		}
		int signatureLength = bytes.getInt();
		if(signatureLength > 0) {
			_signature = new byte[signatureLength];
			bytes.get(_signature);
		}
		int authTokenLength = bytes.getInt();
		if(authTokenLength > 0) {
			byte[] authTokenBytes = new byte[authTokenLength];
			bytes.get(authTokenBytes);
			_authToken = new String(authTokenBytes, Charset.forName("UTF-8"));
		}
	}

	@Override public boolean isValid() {
		return _userID != null && _chunks != null && _chunks.length > 0 && _authToken != null && !_authToken.isEmpty();
	}

	@Override public boolean isRequest() {
		return _isRequest;
	}

	@Override public String getType() {
		return _messageType;
	}

	public String getAuthToken() {
		return _authToken;
	}

	public String getMessageType() {
		return _messageType;
	}

	String[] getChunks() {
		return _chunks;
	}

	public String getUserID() {
		return _userID;
	}
}
