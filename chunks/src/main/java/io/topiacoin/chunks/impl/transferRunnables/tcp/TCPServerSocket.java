package io.topiacoin.chunks.impl.transferRunnables.tcp;

import io.topiacoin.chunks.intf.ProtocolServerSocket;
import io.topiacoin.chunks.intf.ProtocolSocket;

import java.io.IOException;
import java.net.ServerSocket;

public class TCPServerSocket implements ProtocolServerSocket {
	private ServerSocket _serverSocket;

	public TCPServerSocket(int port) throws IOException {
		_serverSocket = new ServerSocket(port);
	}

	@Override public void close() throws IOException {
		_serverSocket.close();
	}

	@Override public ProtocolSocket accept() throws IOException {
		return new TCPSocket(_serverSocket.accept());
	}
}
