package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.intf.AbstractProtocolTest;
import io.topiacoin.chunks.intf.ProtocolCommsService;
import io.topiacoin.chunks.model.MessageID;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;

public class TCPTest extends AbstractProtocolTest {

	@Override protected ProtocolCommsService getProtocolCommsService(int port, KeyPair transferKeyPair) throws IOException {
		return new TCPProtocolCommsService(port, transferKeyPair);
	}

	@Override
	protected SocketChannel getConnectionForMessageID(ProtocolCommsService service, MessageID id) {
		return ((TCPProtocolCommsService) service).getConnectionForMessageID(id);
	}
}
