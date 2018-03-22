package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.impl.transferRunnables.tcp.TCPProtocolCommsService;
import io.topiacoin.chunks.intf.AbstractProtocolTest;
import io.topiacoin.chunks.intf.ProtocolCommsService;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class TCPTest extends AbstractProtocolTest {

	@Override protected ProtocolCommsService getProtocolCommsService(int port, PublicKey pubKey, PrivateKey privKey) throws IOException {
		return new TCPProtocolCommsService(port, pubKey, privKey);
	}
}
