package io.topiacoin.chunks.impl.transferRunnables.utp;

import io.topiacoin.chunks.intf.ProtocolSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

public class UTPProtocolSocket implements ProtocolSocket {
	private UTPSocket _socket;

	public UTPProtocolSocket(InetAddress addr, int port) throws IOException {
		_socket = new UTPSocket(addr, port);
	}

	UTPProtocolSocket(UTPSocket sock) throws IOException {
		_socket = sock;
	}

	@Override public OutputStream getOutputStream() throws IOException {
		return _socket.getOutputStream();
	}

	@Override public void close() throws IOException {
		_socket.close();
	}

	@Override public InputStream getInputStream() throws IOException {
		return _socket.getInputStream();
	}

	@Override public void shutdownOutput() throws IOException {
		throw new UnsupportedOperationException("This UTP thing doesn't work");
	}
}
