package io.topiacoin.chunks.model.protocol;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

public class ProtocolDataMessage extends ProtocolMessage {
	private byte[] data;

	public ProtocolDataMessage(byte[] dat) {
		data = dat;
	}

	public byte[] toBytes(PrivateKey privKey) throws SignatureException, InvalidKeyException {
		return data;
	}
}
