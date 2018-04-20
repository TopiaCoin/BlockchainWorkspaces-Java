package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.intf.AbstractProtocolTest;
import io.topiacoin.chunks.intf.ProtocolCommsService;
import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ProtocolConnectionState;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TCPTest extends AbstractProtocolTest {

	@Override protected ProtocolCommsService getProtocolCommsService(int port, KeyPair transferKeyPair) throws IOException {
		return new TCPProtocolCommsService(port, transferKeyPair);
	}

	@Override
	protected SocketChannel getConnectionForMessageID(ProtocolCommsService service, MessageID id) {
		return ((TCPProtocolCommsService) service).getConnectionForMessageID(id);
	}

	@Override protected MessageID[] sendMessagenBytesAtATime(ProtocolCommsService commsInterface, int bytesAtATime, String location, int port, byte[] transferPublicKey, ProtocolMessage[] messages) throws FailedToStartCommsListenerException, InvalidMessageException, InvalidKeyException, IOException {
		TCPProtocolCommsService commsService = null;
		if(commsInterface instanceof TCPProtocolCommsService) {
			commsService = (TCPProtocolCommsService) commsInterface;
		} else {
			throw new UnsupportedOperationException("ProtocolCommsService must be an instance of TCPProtocolCommsService");
		}
		for(ProtocolMessage message : messages) {
			if (!message.isRequest()) {
				throw new InvalidMessageException("Cannot send non-request type message as a request");
			}
			if (!message.isValid()) {
				throw new InvalidMessageException("Message not valid, will not send");
			}
		}
		if (!commsService._listenerThread.isAlive()) {
			throw new FailedToStartCommsListenerException("Listener must be started before sending messages");
		}
		InetSocketAddress addr = new InetSocketAddress(location, port);
		ProtocolConnectionState state = commsService._connections.get(addr);
		List<ByteBuffer> datas = new ArrayList<>();
		List<MessageID> messageIDs = new ArrayList<>();
		int totalSize = 0;
		for(ProtocolMessage message : messages) {
			MessageID messageID = new MessageID(commsService._messageIdTracker++, addr);
			messageIDs.add(messageID);
			commsService._messageAddresses.put(messageID, addr);
			if (state == null) {
				try {
					state = new ProtocolConnectionState(addr, messageID, transferPublicKey);
				} catch (InvalidKeySpecException e) {
					throw new InvalidKeyException("Public Key Data was invalid", e);
				}
			} else {
				state.addMessageID(messageID);
			}
			commsService._connections.put(addr, state);

			ByteBuffer data = commsService.encryptAndFrameMessage(message, messageID, state);
			data.flip();
			totalSize += data.remaining();
			datas.add(data);
		}
		ByteBuffer totalData = ByteBuffer.allocate(totalSize);
		for(ByteBuffer data : datas) {
			totalData.put(data);
		}
		totalData.flip();
		while (totalData.hasRemaining()) {
			ByteBuffer partialData = ByteBuffer.allocate(Math.min(bytesAtATime, totalData.remaining()));
			putAsMuchAsPossible(partialData, totalData);
			partialData.flip();
			state.addWriteBuffer(partialData);
			commsService._listenerRunnable.wakeupSelector();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				//NOP
			}
		}
		return messageIDs.toArray(new MessageID[0]);
	}
}
