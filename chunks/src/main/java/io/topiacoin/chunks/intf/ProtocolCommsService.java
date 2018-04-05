package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.exceptions.InvalidSignatureException;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;

public interface ProtocolCommsService {

	public int sendMessage(String location, int port, byte[] transferPublicKey, String authToken, ProtocolMessage message)
			throws InvalidKeyException, IOException, InvalidMessageException;

	public void reply(ProtocolMessage message, int messageID) throws SignatureException, InvalidKeyException;

	public void setHandler(ProtocolCommsHandler handler);

	public void start() throws FailedToStartCommsListenerException;

	public void stop();


}
