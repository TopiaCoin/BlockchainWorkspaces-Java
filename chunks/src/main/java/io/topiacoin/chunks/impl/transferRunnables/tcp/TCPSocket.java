package io.topiacoin.chunks.impl.transferRunnables.tcp;

import io.topiacoin.chunks.intf.ProtocolSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TCPSocket implements ProtocolSocket {
	private Socket _tcpSocket;

	public TCPSocket(InetAddress addr, int port) throws IOException {
		_tcpSocket = new Socket(addr, port);
	}

	TCPSocket(Socket sock) {
		_tcpSocket = sock;
	}

	@Override public OutputStream getOutputStream() throws IOException {
		return _tcpSocket.getOutputStream();
	}

	@Override public void close() throws IOException {
		_tcpSocket.close();
	}

	@Override public InputStream getInputStream() throws IOException {
		return _tcpSocket.getInputStream();
	}

	@Override public void shutdownOutput() throws IOException {
		_tcpSocket.shutdownOutput();
	}
}
