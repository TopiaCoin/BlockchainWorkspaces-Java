package io.topiacoin.chunks.impl.transferRunnables.tcp;

import io.topiacoin.chunks.intf.ProtocolSender;
import io.topiacoin.chunks.intf.ProtocolSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

public class TCPSender extends ProtocolSender {

	public TCPSender(PublicKey pubKey, PrivateKey privKey) {
		super(pubKey, privKey);
	}

	@Override public ProtocolSocket getSocket(InetAddress addr, int port) throws IOException {
		return new TCPSocket(addr, port);
	}
}
