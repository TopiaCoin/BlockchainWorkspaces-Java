package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.InvalidSignatureException;
import io.topiacoin.chunks.model.protocol.ProtocolDataMessage;
import io.topiacoin.chunks.model.protocol.ProtocolJsonMessage;
import io.topiacoin.chunks.model.protocol.ProtocolJsonRequest;
import io.topiacoin.chunks.model.protocol.ProtocolJsonResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolJsonRequest;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolJsonResponse;
import io.topiacoin.util.NotificationCenter;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

public abstract class ProtocolListener {
	private PublicKey _pubKey;
	private PrivateKey _privKey;
	private NotificationCenter _notificationCenter = NotificationCenter.defaultCenter();
	private Thread _listenerThread = null;
	private ProtocolSocket _socket = null;
	protected ProtocolServerSocket _serverSocket = null;
	protected int _port;
	protected String _threadName = "protocol_listener_thread";

	public ProtocolListener(int port, PublicKey pubKey, PrivateKey privKey) {
		_port = port;
		_pubKey = pubKey;
		_privKey = privKey;
	}

	protected void start() throws IOException {
		_listenerThread = new Thread(new ListenerRunnable(), _threadName);
		_listenerThread.start();
	}

	public void stop() {
		if(_socket != null) {
			try {
				_socket.close();
			} catch (IOException e) {
				//NOP
			}
		}
		if(_serverSocket != null) {
			try {
				_serverSocket.close();
			} catch (IOException e) {
				//NOP
			}
		}
	}

	private class ListenerRunnable implements Runnable {
		boolean run = true;
		@Override public void run() {
			try {
				while(run) {
					_socket = _serverSocket.accept();
					System.out.println("Got a socket");
					if(_socket == null || _socket.getInputStream() == null) {
						System.out.println("...but it's malformed");
						continue;
					}
					byte[] data = IOUtils.toByteArray(_socket.getInputStream());
					System.out.println("Read " + data.length + " bytes off the socket");
					if(data.length == 0) {
						continue;
					}
					try {
						ProtocolMessage message = ProtocolMessage.decodeMessage(data, _pubKey);
						if(message instanceof QueryChunksProtocolJsonRequest) {
							String[] chunks = new String[]{"foo", "bar"};
							ProtocolMessage resp = new QueryChunksProtocolJsonResponse(chunks, "userId", "nonce", _pubKey);
							_socket.getOutputStream().write(resp.encodeMessage(_privKey));
							_socket.getOutputStream().flush();
							_socket.getOutputStream().close();
						} else if(message instanceof ProtocolJsonResponse) {
							Map<String, Object> info = new HashMap<String, Object>();
							info.put("message", message);
							String userId = ((ProtocolJsonMessage)message).getUserID();
							_notificationCenter.postNotification("ProtocolMessageReceived", userId, info);
						} else if(message instanceof ProtocolDataMessage) {

						}
					} catch (InvalidSignatureException e) {
						e.printStackTrace();
					} catch (SignatureException e) {
						e.printStackTrace();
					} catch (InvalidKeyException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
