package io.topiacoin.chunks.model.protocol;

import io.topiacoin.chunks.exceptions.InvalidSignatureException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;

public abstract class ProtocolMessage {
	private static final int FRAME_META_LENGTH = (Integer.BYTES * 2) + 1;
	private int _messageID = -1;

	public ByteBuffer encodeMessage(PrivateKey privateKey, int messageID) throws SignatureException, InvalidKeyException {
		byte type;
		byte[] data;
		if (this instanceof ProtocolJsonRequest) {
			type = 0x01;
			data = ((ProtocolJsonMessage) this).toBytes(privateKey);
		} else if (this instanceof ProtocolJsonResponse) {
			type = 0x02;
			data = ((ProtocolJsonMessage) this).toBytes(privateKey);
		} else if (this instanceof ProtocolDataMessage) {
			type = 0x03;
			data = ((ProtocolDataMessage) this).toBytes(privateKey);
		} else {
			return null;
		}
		int length = data.length;

		ByteBuffer buffer = ByteBuffer.allocate(FRAME_META_LENGTH + length);
		buffer.put(type);
		buffer.putInt(messageID);
		buffer.putInt(length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

	public static ProtocolMessage decodeMessage(ByteBuffer message, PublicKey publicKey) throws IOException, InvalidSignatureException {
		byte type = message.get();
		int messageID = message.getInt();
		int length = message.getInt();
		System.out.println("Decoding Message. Type: " + Byte.toString(type) + ", id: " + messageID + ", length: " + length);
		byte[] data = new byte[message.remaining()];
		message.get(data);
		ProtocolMessage pm = null;
		if (type == 0x01 || type == 0x02) {
			pm = ProtocolJsonMessage.fromBytes(data, publicKey);
		} else if (type == 0x03) {
			pm = new ProtocolDataMessage(data);
		}
		pm.setMessageID(messageID);
		return pm;
	}

	public static boolean containsFullMessage(ByteBuffer buffer) {
		if (buffer == null || !buffer.hasRemaining() || buffer.remaining() <= FRAME_META_LENGTH) {
			return false;
		} else {
			buffer.mark();
			byte ignored = buffer.get();
			int length = buffer.getInt();
			boolean toReturn = length <= buffer.remaining();
			buffer.reset();
			return toReturn;
		}
	}

	public static int getMessageLength(ByteBuffer buffer) {
		if (!buffer.hasRemaining() || buffer.remaining() <= FRAME_META_LENGTH) {
			return -1;
		} else {
			buffer.mark();
			byte ignored = buffer.get();
			int ignoredInt = buffer.getInt();
			int length = buffer.getInt();
			buffer.reset();
			return length + FRAME_META_LENGTH;
		}
	}

	private void setMessageID(int id) {
		_messageID = id;
	}

	public int getMessageID() {
		return _messageID;
	}
}
