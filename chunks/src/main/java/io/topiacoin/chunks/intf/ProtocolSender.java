package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.InvalidSignatureException;
import io.topiacoin.chunks.impl.transferRunnables.tcp.TCPSocket;
import io.topiacoin.chunks.model.protocol.ProtocolJsonMessage;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.util.NotificationCenter;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

public abstract class ProtocolSender {
	private PublicKey _pubKey;
	private PrivateKey _privKey;
	private static final int TIMEOUT_IN_MS=30000; //30s
	private NotificationCenter _notificationCenter = NotificationCenter.defaultCenter();

	public ProtocolSender(PublicKey pubKey, PrivateKey privKey) {
		_pubKey = pubKey;
		_privKey = privKey;
	}

	public void sendMessage(String location, int port, ProtocolMessage message)
			throws IOException, SignatureException, InvalidKeyException, InvalidSignatureException {
		InetAddress addr = InetAddress.getByName(location);
		ProtocolSocket socket = getSocket(addr, port);
		socket.getOutputStream().write(message.encodeMessage(_privKey));

		socket.getOutputStream().flush();
		socket.shutdownOutput();
		//Wait for a response
		for(int i = 0; i < TIMEOUT_IN_MS / 100; i++) {
			byte[] data = IOUtils.toByteArray(socket.getInputStream());
			System.out.println("Read " + data.length + " bytes off the socket");
			if(data.length > 0) {
				ProtocolMessage resp = ProtocolMessage.decodeMessage(data, _pubKey);
				Map<String, Object> info = new HashMap<String, Object>();
				info.put("message", message);
				String userId = ((ProtocolJsonMessage)message).getUserID();
				_notificationCenter.postNotification("ProtocolMessageReceived", userId, info);
				break;
			} else {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new IOException("", e);
				}
			}
		}
		socket.close();
	}

	public abstract ProtocolSocket getSocket(InetAddress addr, int port) throws IOException;
}
