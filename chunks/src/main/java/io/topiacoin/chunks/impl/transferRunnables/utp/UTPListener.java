package io.topiacoin.chunks.impl.transferRunnables.utp;

import io.topiacoin.chunks.intf.ProtocolListener;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class UTPListener extends ProtocolListener {

	public UTPListener(int port, PublicKey pubKey, PrivateKey privKey) {
		super(port, pubKey, privKey);
	}

	//Starts the UTP listener unless it is already running.
	public void start() throws IOException {
		if (_serverSocket == null) {
			_serverSocket = new UTPServerSocket(_port);
			_threadName = "utp_listener_thread";
			super.start();
		}
	}
}
