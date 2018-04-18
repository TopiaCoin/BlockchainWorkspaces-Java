package io.topiacoin.chunks.model.protocol;

import io.topiacoin.chunks.model.MessageID;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProtocolConnectionState {

	private SocketChannel _channel;
	private List<ByteBuffer> _writeBuffers = new ArrayList<>();
	private ByteBuffer _packetBuffer = null;
	private List<MessageID> _messageIDs = new ArrayList<>();
	private long _lastUseTime = System.currentTimeMillis();
	private byte[] _theirPublicKey;
	private byte[] _myPublicKey = new byte[0];
	private SecretKey _messageKey;
	private boolean _isResponse = false;
	private KeyPair _requestKeyPair = null;

	public ProtocolConnectionState(SocketChannel channel, Selector selector) throws IOException {
		_channel = channel;
		_channel.configureBlocking(false);
		_channel.register(selector, SelectionKey.OP_READ);
	}

	public ProtocolConnectionState(SocketAddress address, MessageID messageID, byte[] theirPublicKey) throws IOException, InvalidKeySpecException, InvalidKeyException {
		openConnection(address);
		_messageIDs.add(messageID);
		_theirPublicKey = theirPublicKey;
		try {
			KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
			userKeyGen.initialize(571);
			_requestKeyPair = userKeyGen.genKeyPair();
			_myPublicKey = _requestKeyPair.getPublic().getEncoded();
			buildMessageKey(_requestKeyPair);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failure", e);
		}
	}

	public void reconnectIfNecessary(SocketAddress address) throws IOException {
		if (!_channel.isConnected()) {
			openConnection(address);
		}
	}

	private void openConnection(SocketAddress address) throws IOException {
		_channel = SocketChannel.open(address);
		_channel.configureBlocking(false);
	}

	public byte[] getTheirPublicKey() {
		return _theirPublicKey;
	}

	public void setTheirPublicKey(byte[] key) {
		_theirPublicKey = key;
	}

	public byte[] getMyPublicKey() {
		return _myPublicKey;
	}

	public SecretKey getMessageKey() {
		return _messageKey;
	}

	public void addWriteBuffer(ByteBuffer data, boolean isResponse) {
		if (data.hasRemaining()) {
			_writeBuffers.add(data);
			_lastUseTime = System.currentTimeMillis();
			_isResponse = isResponse;
		}
	}

	public void registerForPendingWrites(Selector selector) throws ClosedChannelException {
		if (!_writeBuffers.isEmpty()) {
			_channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			_lastUseTime = System.currentTimeMillis();
		} else {
			_channel.register(selector, SelectionKey.OP_READ);
		}
	}

	public void write() throws IOException {
		while (!_writeBuffers.isEmpty()) {
			ByteBuffer wb = _writeBuffers.get(0);
			if (wb.hasRemaining()) {
				_channel.write(wb);
				break;
			} else {
				_writeBuffers.remove(0);
			}
		}
	}

	public ByteBuffer read(ByteBuffer readBuffer) {
		//This is basically the way ByteBuffer.put(ByteBuffer) works, but without the ability to throw BufferOverflowExceptions
		//I don't need a BufferOverflow - if there's more data to be read, the readBuffer will be compacted and the loop will go on
		int n = Math.min(_packetBuffer.remaining(), readBuffer.remaining());
		for (int i = 0; i < n; i++) {
			_packetBuffer.put(readBuffer.get());
		}
		if(_packetBuffer.hasRemaining()) {
			return null;
		} else {
			ByteBuffer toReturn = _packetBuffer;
			_packetBuffer = null;
			return toReturn;
		}
	}

	public void allocatePacketBuffer(int size) {
		_packetBuffer = ByteBuffer.allocate(size);
	}

	public boolean hasPacketBuffer() {
		return _packetBuffer != null;
	}

	/*private SecretKey buildRequestKey(byte[] theirPubKeyData, MessageID messageID) throws InvalidKeySpecException {
		KeyPair requestKeyPair = _messageRequestKeypairs.get(messageID);
		if (theirPubKeyData == null) {
			throw new InvalidKeySpecException("Keydata is null");
		}
		try {
			KeyFactory kf = KeyFactory.getInstance("EC");
			X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(theirPubKeyData);
			PublicKey theirPubKey = kf.generatePublic(pkSpec);
			return buildMessageKey(theirPubKey, requestKeyPair);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("", e);
		}
	}

	private SecretKey buildResponseKey(byte[] requestersPublicKeyData) throws InvalidKeySpecException {
		if (requestersPublicKeyData == null) {
			throw new InvalidKeySpecException("Keydata is null");
		}
		try {
			KeyFactory kf = KeyFactory.getInstance("EC");
			X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(requestersPublicKeyData);
			PublicKey requestersPublicKey = kf.generatePublic(pkSpec);
			return buildMessageKey(requestersPublicKey, _chunkTransferKeyPair);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("", e);
		}
	}

	private SecretKey buildMessageKey(PublicKey pubKey, KeyPair myKeyPair) {
		try {
			KeyAgreement ka = KeyAgreement.getInstance("ECDH");
			System.out.println("My PubKey: " + DatatypeConverter.printHexBinary(myKeyPair.getPublic().getEncoded()));
			System.out.println("Their PubKey: " + DatatypeConverter.printHexBinary(pubKey.getEncoded()));
			ka.init(myKeyPair.getPrivate());
			ka.doPhase(pubKey, true);

			byte[] sharedSecret = ka.generateSecret();
			System.out.println("Shared Secret: " + DatatypeConverter.printHexBinary(sharedSecret));
			MessageDigest hash = MessageDigest.getInstance("SHA-256");
			hash.update(sharedSecret);
			// Simple deterministic ordering
			List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(myKeyPair.getPublic().getEncoded()), ByteBuffer.wrap(pubKey.getEncoded()));
			Collections.sort(keys);
			hash.update(keys.get(0));
			hash.update(keys.get(1));
			//We must now reduce the size of this keyData to 128 bits (16 bytes) due to U.S. Govt regulations regarding maximum Keylength.
			byte[] derivedKeyData = Arrays.copyOf(hash.digest(), 16);
			return new SecretKeySpec(derivedKeyData, "AES");
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException("", e);
		}
	}*/

	public void buildMessageKey(KeyPair myKeyPair) throws InvalidKeySpecException, InvalidKeyException {
		try {
			if(_theirPublicKey == null) {
				throw new InvalidKeySpecException("Public Key Null");
			}
			KeyFactory kf = KeyFactory.getInstance("EC");
			X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(_theirPublicKey);
			PublicKey theirPubKey = kf.generatePublic(pkSpec);
			KeyAgreement ka = KeyAgreement.getInstance("ECDH");
			System.out.println("My PubKey: " + DatatypeConverter.printHexBinary(myKeyPair.getPublic().getEncoded()));
			System.out.println("Their PubKey: " + DatatypeConverter.printHexBinary(theirPubKey.getEncoded()));
			ka.init(myKeyPair.getPrivate());
			ka.doPhase(theirPubKey, true);

			byte[] sharedSecret = ka.generateSecret();
			System.out.println("Shared Secret: " + DatatypeConverter.printHexBinary(sharedSecret));
			MessageDigest hash = MessageDigest.getInstance("SHA-256");
			hash.update(sharedSecret);
			// Simple deterministic ordering
			List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(myKeyPair.getPublic().getEncoded()), ByteBuffer.wrap(theirPubKey.getEncoded()));
			Collections.sort(keys);
			hash.update(keys.get(0));
			hash.update(keys.get(1));
			//We must now reduce the size of this keyData to 128 bits (16 bytes) due to U.S. Govt regulations regarding maximum Keylength.
			byte[] derivedKeyData = Arrays.copyOf(hash.digest(), 16);
			_messageKey = new SecretKeySpec(derivedKeyData, "AES");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("", e);
		}
	}

	public void requestReceived(MessageID messageID) {
		_messageIDs.add(messageID);
	}

	public void responseReceived(MessageID messageID) {
		_messageIDs.remove(messageID);
	}

	public SocketChannel getSocketChannel() {
		return _channel;
	}

	public long getLastUsedTime() {
		return _lastUseTime;
	}

	public boolean hasMessageIDs() {
		return !_messageIDs.isEmpty();
	}

	public boolean hasWriteBuffers() {
		return !_writeBuffers.isEmpty();
	}

	public void removeMessageID(MessageID messageID) {
		_messageIDs.remove(messageID);
	}
}