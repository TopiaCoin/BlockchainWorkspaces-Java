package io.topiacoin.chunks.impl.transferRunnables.tcp;

import io.topiacoin.chunks.intf.ProtocolListener;
import io.topiacoin.chunks.intf.ProtocolServerSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.PrivateKey;
import java.security.PublicKey;

public class TCPListener extends ProtocolListener {

	public TCPListener(int port, PublicKey pubKey, PrivateKey privKey) {
		super(port, pubKey, privKey);
	}

	//Starts the TCP listener unless it is already running.
	public void start() throws IOException {
		if (_serverSocket == null) {
			_serverSocket = new TCPServerSocket(_port);
			_threadName = "tcp_listener_thread";
			super.start();
		}
	}
}
