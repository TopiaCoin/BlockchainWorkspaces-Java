package io.topiacoin.chunks.model.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class GiveChunkProtocolResponse implements ProtocolMessage {
	private String _userID;
	private String _chunkId;
	private byte[] _chunkdata;
	private byte[] _signature = null;
	private String _authToken;
	private String _messageType = "GIVE_CHUNK";

	public GiveChunkProtocolResponse(String chunkId, byte[] data, String userID) {
		_chunkId = chunkId;
		_chunkdata = data;
		_userID = userID;
	}

	public GiveChunkProtocolResponse() {
		this(null, null, null);
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
		// chunk ID length, an int
		// chunk ID, a UTF-8 String whose bytelength is chunk0 length
		// chunk data length, an int
		// chunk data, an array of bytes
		// signature length, an int
		// signature bytes, an array of bytes whose length is signature length
		// authToken length, an int
		// authToken, a UTF-8 String whose bytelength is authToken length
		// So that's 3 + chunk count ints...oh whatever, just look at the code.
		//Figure out how large of a byte buffer we need to allocate:
		int toAlloc = 0;
		toAlloc += Integer.BYTES; //UserID length
		toAlloc += _userID.getBytes(Charset.forName("UTF-8")).length; //userID
		toAlloc += Integer.BYTES; //chunk ID length
		toAlloc += _chunkId.getBytes(Charset.forName("UTF-8")).length; //chunkID string length
		toAlloc += Integer.BYTES; //chunk data length
		toAlloc += _chunkdata.length; //chunk data bytes
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
		byte[] chunkIDBytes = _chunkId.getBytes(Charset.forName("UTF-8"));
		toReturn.putInt(chunkIDBytes.length); //chunk ID length
		toReturn.put(chunkIDBytes); //chunk ID
		toReturn.putInt(_chunkdata.length); //chunk data length
		toReturn.put(_chunkdata); //chunk data
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
		int chunkIDLength = bytes.getInt();
		byte[] chunkIDBytes = new byte[chunkIDLength];
		bytes.get(chunkIDBytes);
		_chunkId = new String(chunkIDBytes, Charset.forName("UTF-8"));
		int chunkDataLength = bytes.getInt();
		_chunkdata = new byte[chunkDataLength];
		bytes.get(_chunkdata);
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
		return _userID != null && _chunkId != null && _chunkId.length() > 0 && _chunkdata != null && _chunkdata.length > 0;
	}

	@Override public boolean isRequest() {
		return false;
	}

	@Override public String getType() {
		return _messageType;
	}

	public String getMessageType() {
		return _messageType;
	}

	public String getChunkID() {
		return _chunkId;
	}

	public String getUserID() {
		return _userID;
	}

	public byte[] getChunkData() {
		return _chunkdata;
	}
}
