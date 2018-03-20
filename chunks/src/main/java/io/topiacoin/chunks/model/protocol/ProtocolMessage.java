package io.topiacoin.chunks.model.protocol;

import io.topiacoin.chunks.exceptions.InvalidSignatureException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

public abstract class ProtocolMessage {

	public byte[] encodeMessage(PrivateKey privateKey) throws SignatureException, InvalidKeyException {
		byte type;
		byte[] data;
		if(this instanceof ProtocolJsonMessage) {
			type = 0x01;
			data = ((ProtocolJsonMessage)this).toBytes(privateKey);
		} else if(this instanceof ProtocolDataMessage) {
			type = 0x02;
			data = ((ProtocolDataMessage)this).toBytes(privateKey);
		} else {
			return null;
		}
		int length = data.length;
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + 1 + length);
		buffer.put(type);
		buffer.putInt(length);
		buffer.put(data);
		buffer.flip();
		return buffer.array();
	}

	public static ProtocolMessage decodeMessage(byte[] message, PublicKey publicKey) throws IOException, InvalidSignatureException {
		int frameMetaLength = 1 + Integer.BYTES;
		if(message != null && message.length > frameMetaLength) {
			byte type = message[0];
			ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
			lengthBuffer.put(message, 1, Integer.BYTES);
			lengthBuffer.flip();
			int length = lengthBuffer.getInt();
			if(message.length - frameMetaLength < length) {
				return null;
			}
			System.out.println("Decoding Message. Type: " + Byte.toString(type) + ", length: " + length);
			ByteBuffer dataBuffer = ByteBuffer.allocate(message.length - frameMetaLength);
			dataBuffer.put(message, frameMetaLength, message.length - frameMetaLength);
			dataBuffer.flip();
			byte[] data = dataBuffer.array();
			if(type == 0x01) {
				return ProtocolJsonMessage.fromBytes(data, publicKey);
			} else if(type == 0x02) {
				return new ProtocolDataMessage(data);
			}
		}
		return null;
	}
}
