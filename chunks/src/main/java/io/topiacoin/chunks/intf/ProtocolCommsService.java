package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.InvalidSignatureException;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

public interface ProtocolCommsService {

	public int sendMessage(String location, int port, ProtocolMessage message)
			throws InvalidSignatureException, SignatureException, InvalidKeyException, IOException;

	public void reply(ProtocolMessage message, int messageID) throws SignatureException, InvalidKeyException;

	public void setHandler(ProtocolCommsHandler handler);

	public void start();

	public void stop();


}
