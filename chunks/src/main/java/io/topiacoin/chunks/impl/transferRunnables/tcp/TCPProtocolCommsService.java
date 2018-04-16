package io.topiacoin.chunks.impl.transferRunnables.tcp;

import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.exceptions.InvalidMessageIDException;
import io.topiacoin.chunks.intf.ProtocolCommsHandler;
import io.topiacoin.chunks.intf.ProtocolCommsService;
import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ErrorProtocolResponse;
import io.topiacoin.chunks.model.protocol.FetchChunkProtocolRequest;
import io.topiacoin.chunks.model.protocol.GiveChunkProtocolResponse;
import io.topiacoin.chunks.model.protocol.HaveChunksProtocolResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolRequest;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TCPProtocolCommsService implements ProtocolCommsService {
	private KeyPair _chunkTransferKeyPair;
	private Thread _listenerThread;
	private TCPListenerRunnable _listenerRunnable;
	private Throwable _listenerRunnableThrowable = null;
	private Map<SocketAddress, SocketChannel> _connections = new HashMap<>();
	private Map<SocketChannel, ByteBuffer> _packetReadBuffers = new HashMap<>();
	private Map<SocketChannel, ArrayList<ByteBuffer>> _writeBuffers = new HashMap<>();
	private Map<SocketAddress, Integer> _messageIDs = new HashMap<>();
	private Map<SocketAddress, byte[]> _transferPublicKeys = new HashMap<>();
	private Map<MessageID, byte[]> _requestorPublicKeys = new HashMap<>();
	private Map<MessageID, byte[]> _messageAuthKeys = new HashMap<>();
	private Map<MessageID, KeyPair> _messageRequestKeypairs = new HashMap<>();
	private Map<MessageID, SocketChannel> _replyConnections = new HashMap<>();
	private Map<MessageID, Integer> _requestResponseSplits = new HashMap<>();
	private Map<String, Byte> _messageTypes = new HashMap<>();
	private int _messageIdTracker = 0;
	private ProtocolCommsHandler _handler = null;

	public TCPProtocolCommsService(int port, KeyPair chunkTransferKeyPair) throws IOException {
		_chunkTransferKeyPair = chunkTransferKeyPair;
		_listenerRunnable = new TCPListenerRunnable(port);
		_listenerThread = new Thread(_listenerRunnable, "ProtocolComms:" + port);
		_listenerThread.setDaemon(true);
		byte b = 0x01;
		_messageTypes.put("QUERY_CHUNKS", b);
		b = 0x02;
		_messageTypes.put("HAVE_CHUNKS", b);
		b = 0x03;
		_messageTypes.put("REQUEST_CHUNK", b);
		b = 0x04;
		_messageTypes.put("GIVE_CHUNK", b);
		b = 0x09;
		_messageTypes.put("ERROR", b);
	}

	@Override public MessageID sendMessage(String location, int port, byte[] transferPublicKey, String authToken, ProtocolMessage message)
			throws InvalidKeyException, IOException, InvalidMessageException, FailedToStartCommsListenerException {
		if (!message.isRequest()) {
			throw new InvalidMessageException("Cannot send non-request type message as a request");
		}
		if (!message.isValid()) {
			throw new InvalidMessageException("Message not valid, will not send");
		}
		if (!_listenerThread.isAlive()) {
			throw new FailedToStartCommsListenerException("Listener must be started before sending messages");
		}
		InetSocketAddress addr = new InetSocketAddress(location, port);
		MessageID messageID = new MessageID(_messageIdTracker++, addr);
		SocketChannel sc = _connections.get(addr);
		if(sc != null && !sc.isConnected()) {
			sc.close();
			_connections.remove(addr);
			sc = null;
		}
		if(sc == null) {
			sc = SocketChannel.open(addr);
			sc.configureBlocking(false);
			_connections.put(addr, sc);
		}
		_replyConnections.put(messageID, sc);

		boolean sendPubKey = false;
		KeyPair requestKeyPair = _messageRequestKeypairs.get(messageID);
		if (requestKeyPair == null) {
			requestKeyPair = generateRequestKeyPair(messageID);
			sendPubKey = true;
		}
		SecretKey requestKey;
		if (transferPublicKey == null) {
			transferPublicKey = _transferPublicKeys.get(addr);
		}
		try {
			requestKey = buildRequestKey(transferPublicKey, messageID);
		} catch (InvalidKeySpecException e) {
			throw new InvalidKeyException("Public Key Data was invalid", e);
		}
		_transferPublicKeys.put(addr, transferPublicKey);
		ByteBuffer data = encryptAndFrameMessage(message, requestKey, messageID, sendPubKey ? requestKeyPair.getPublic().getEncoded() : null);
		data.flip();
		ArrayList<ByteBuffer> dataList = _writeBuffers.get(sc);
		dataList = dataList == null ? new ArrayList<ByteBuffer>() : dataList;
		dataList.add(data);
		incrementRequestResponseSplit(messageID);
		_writeBuffers.put(sc, dataList);
		_listenerRunnable.wakeupSelector();
		return messageID;
	}

	private KeyPair generateRequestKeyPair(MessageID messageID) {
		try {
			KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
			userKeyGen.initialize(571);
			KeyPair keyPair = userKeyGen.genKeyPair();
			_messageRequestKeypairs.put(messageID, keyPair);
			return keyPair;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failure", e);
		}
	}

	private SecretKey buildRequestKey(byte[] theirPubKeyData, MessageID messageID) throws InvalidKeySpecException {
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
	}

	private ByteBuffer encryptAndFrameMessage(ProtocolMessage message, SecretKey requestKey, MessageID messageID, byte[] transferPubKey)
			throws InvalidKeyException {
		//a fully framed Message consists of the following
		//messageType, a byte
		//messageId, an int
		//Transfer Public Key Length, an int (which is set to 0 if no Transfer Public Key is provided,
		//Transfer Public Key, bytes, if provided
		//Data Length, an int, the length of the encrypted message data.
		Byte messageType = _messageTypes.get(message.getType());
		if (messageType == null) {
			throw new RuntimeException("Failed to frame message - unknown message type '" + message.getType() + "'");
		}
		int transferPubKeyLength = transferPubKey == null ? 0 : transferPubKey.length;
		ByteBuffer data = message.toBytes();
		if (!(message instanceof ErrorProtocolResponse)) {
			try {
				System.out.println("Encrypting: " + (data.remaining() > 5000 ? "<a lot of data> " : DatatypeConverter.printHexBinary(message.toBytes().array())));
				System.out.println("With Key: " + DatatypeConverter.printHexBinary(requestKey.getEncoded()));
				Cipher cipher = Cipher.getInstance("AES");
				cipher.init(Cipher.ENCRYPT_MODE, requestKey);
				byte[] encrypted = cipher.doFinal(data.array());
				ByteBuffer toReturn = ByteBuffer.allocate(1 + Integer.BYTES + Integer.BYTES + transferPubKeyLength + Integer.BYTES + encrypted.length);
				toReturn.put(messageType).putInt(messageID.getId()).putInt(transferPubKeyLength);
				if (transferPubKeyLength > 0) {
					toReturn.put(transferPubKey);
				}
				toReturn.putInt(encrypted.length).put(encrypted);
				System.out.println("Encrypted: " + (encrypted.length > 5000 ? "<a lot of data>" :DatatypeConverter.printHexBinary(encrypted)));
				return toReturn;
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
				throw new RuntimeException("Crypto failure", e);
			}
		} else {
			System.out.println("NOT Encrypting Error");
			int dataLength = data.array().length;
			ByteBuffer toReturn = ByteBuffer.allocate(1 + Integer.BYTES + Integer.BYTES + Integer.BYTES + dataLength);
			data.flip();
			toReturn.put(messageType).putInt(messageID.getId()).putInt(0).putInt(dataLength).put(data);
			return toReturn;
		}
	}

	private ProtocolMessage decryptAndReconstituteMessage(byte messageType, ByteBuffer messageData, SecretKey responseKey)
			throws InvalidKeyException, InvalidMessageException {
		try {
			byte[] messageArray = new byte[messageData.remaining()];
			messageData.get(messageArray);
			ByteBuffer decryptedBuffer;
			if (messageType != _messageTypes.get("ERROR")) {
				if (messageArray.length % 16 != 0) {
					throw new InvalidMessageException("Encrypted Message Data length is not a multiple of 16, and is therefore invalid");
				}
				System.out.println("Decrypting: " + (messageArray.length > 5000 ? "<a lot of data>" : DatatypeConverter.printHexBinary(messageArray)));
				System.out.println("With Key: " + DatatypeConverter.printHexBinary(responseKey.getEncoded()));
				Cipher cipher = Cipher.getInstance("AES");
				cipher.init(Cipher.DECRYPT_MODE, responseKey);
				byte[] decrypted = cipher.doFinal(messageArray);
				System.out.println("Decrypted: " + (decrypted.length > 5000 ? "<a lot of data>" : DatatypeConverter.printHexBinary(decrypted)));
				decryptedBuffer = ByteBuffer.wrap(decrypted);
			} else {
				System.out.println("NOT Decrypting Error");
				decryptedBuffer = ByteBuffer.wrap(messageArray);
			}
			ProtocolMessage toReturn;
			if (messageType == 0x01) {
				toReturn = new QueryChunksProtocolRequest();
			} else if (messageType == 0x02) {
				toReturn = new HaveChunksProtocolResponse();
			} else if (messageType == 0x03) {
				toReturn = new FetchChunkProtocolRequest();
			} else if (messageType == 0x04) {
				toReturn = new GiveChunkProtocolResponse();
			} else if (messageType == 0x09) {
				toReturn = new ErrorProtocolResponse();
			} else {
				throw new InvalidMessageException("Unknown message type");
			}
			toReturn.fromBytes(decryptedBuffer);
			return toReturn;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			throw new RuntimeException("Crypto failure", e);
		}
	}

	private int incrementRequestResponseSplit(MessageID messageID) {
		Integer rrSplit = _requestResponseSplits.get(messageID);
		rrSplit = (rrSplit == null) ? 0 : rrSplit;
		_requestResponseSplits.put(messageID, ++rrSplit);
		return rrSplit;
	}

	private int decrementRequestResponseSplit(MessageID messageID) {
		Integer rrSplit = _requestResponseSplits.get(messageID);
		rrSplit = (rrSplit == null) ? 1 : rrSplit;
		_requestResponseSplits.put(messageID, --rrSplit);
		return rrSplit;
	}

	@Override public void reply(ProtocolMessage message, MessageID messageID) throws InvalidMessageException, FailedToStartCommsListenerException, InvalidMessageIDException {
		if (message.isRequest()) {
			throw new InvalidMessageException("Cannot send non-response type message as a response");
		}
		if (!message.isValid()) {
			throw new InvalidMessageException("Message not valid, will not send");
		}
		if (!_listenerThread.isAlive()) {
			throw new FailedToStartCommsListenerException("Listener must be started before sending messages");
		}
		SocketChannel sc = _replyConnections.get(messageID);
		if (sc != null && sc.isConnected()) {
			byte[] transferPublicKey = _requestorPublicKeys.get(messageID);
			SecretKey requestKey = null;
			if (!(message instanceof ErrorProtocolResponse)) {
				try {
					requestKey = buildResponseKey(transferPublicKey);
				} catch (InvalidKeySpecException e) {
					throw new IllegalStateException("Cannot reply to message " + messageID + " because the cached publicKey is invalid", e);
				}
			}
			ByteBuffer data = null;
			try {
				data = encryptAndFrameMessage(message, requestKey, messageID, null);
			} catch (InvalidKeyException e) {
				throw new IllegalStateException("Cannot reply to message " + messageID + " because of crypto errors", e);
			}
			data.flip();
			ArrayList<ByteBuffer> dataList = _writeBuffers.get(sc);
			dataList = dataList == null ? new ArrayList<ByteBuffer>() : dataList;
			dataList.add(data);
			_writeBuffers.put(sc, dataList);
			_listenerRunnable.wakeupSelector();
			decrementRequestResponseSplit(messageID);
		} else {
			throw new InvalidMessageIDException("Cannot reply for message " + messageID + " because I could not find a reply channel");
		}
	}

	@Override public void setHandler(ProtocolCommsHandler handler) {
		_handler = handler;
	}

	private void setListenerRunnableThrowable(Throwable t) {
		_listenerRunnableThrowable = t;
	}

	public void startListener() throws FailedToStartCommsListenerException {
		if (_handler == null) {
			throw new IllegalStateException("Cannot startListener without a message handler");
		}
		Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
			@Override public void uncaughtException(Thread t, Throwable e) {
				_handler.error(e);
				setListenerRunnableThrowable(e);
			}
		};
		_listenerThread.setUncaughtExceptionHandler(h);
		_listenerThread.start();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {

		}
		if (_listenerRunnableThrowable != null) {
			Throwable t = _listenerRunnableThrowable;
			stopListener();
			throw new FailedToStartCommsListenerException("Comms listener failed to startListener", t);
		}
	}

	public void stopListener() {
		if (_listenerRunnable != null) {
			_listenerRunnable.stop();
		}
		if (_listenerThread != null) {
			_listenerThread.interrupt();
		}
		_listenerRunnableThrowable = null;
	}

	@Override public SocketChannel getConnectionForMessageID(int messageID) {
		return _connections.get(messageID);
	}

	private class TCPListenerRunnable implements Runnable {
		final InetSocketAddress _hostAddress;
		final Selector _selector;
		ServerSocketChannel _serverSocket;
		private boolean run = true;

		TCPListenerRunnable(int port) throws IOException {
			_hostAddress = new InetSocketAddress("127.0.0.1", port);
			_selector = Selector.open();
		}

		public void wakeupSelector() {
			_selector.wakeup();
		}

		@Override public void run() {
			try {
				_serverSocket = ServerSocketChannel.open();
				_serverSocket.bind(_hostAddress);
				_serverSocket.configureBlocking(false);
				_serverSocket.register(_selector, _serverSocket.validOps(), null);

				while (run) {
					if (_selector.select() > 0) {
						final Iterator<SelectionKey> selectionKeyIterator;
						synchronized (_selector) {
							selectionKeyIterator = _selector.selectedKeys().iterator();
						}
						Thread.yield();
						while (selectionKeyIterator.hasNext()) {
							final SelectionKey key = selectionKeyIterator.next();
							if (key.isAcceptable()) {
								SocketChannel channel = _serverSocket.accept();
								channel.configureBlocking(false);
								channel.register(_selector, SelectionKey.OP_READ);
							} else if (key.isReadable()) {
								SocketChannel sc = (SocketChannel) key.channel();
								ByteBuffer readBuffer = ByteBuffer.allocate(1024);
								int bytesRead = sc.read(readBuffer);
								if (bytesRead == -1) {
									//Connection lost
									_packetReadBuffers.remove(sc);
								} else if (bytesRead > 0) {
									readBuffer.flip();
									while (readBuffer.hasRemaining()) {
										ByteBuffer packetBuffer = _packetReadBuffers.get(sc);
										boolean readBufferIsUsable = true;
										if (packetBuffer == null) {
											//Determine the size of the message.
											readBuffer.mark(); //First, we mark the buffer so we can reset it at the end
											try {
												int msgType = readBuffer.get(); //We read off the 'messageType' byte but ignore its value
												int msgId = readBuffer.getInt(); //Then the messageID, it's value is also ignored for now
												int transferPubKeyLength = readBuffer.getInt(); //Then, the length of the publicKey that may or may not be attached - we need this
												readBuffer.get(new byte[transferPubKeyLength]); //We'll read past the publicKey if it exists - we don't need its value right now.
												int dataLength = readBuffer.getInt(); //Finally, we'll read the data length int - if we've made it this far, we can allocate the buffer.
												packetBuffer = ByteBuffer.allocate(
														1 + Integer.BYTES + Integer.BYTES + transferPubKeyLength + Integer.BYTES + dataLength);
											} catch (BufferUnderflowException ex) {
												//Ok, we couldn't read off enough data to determine how big to make the packetBuffer, so we'll try again next time.
												readBufferIsUsable = false;
											} finally {
												readBuffer.reset(); //We always reset the buffer.
											}
										}
										if (readBufferIsUsable) {
											_packetReadBuffers.remove(sc);
											//This is basically the way ByteBuffer.put(ByteBuffer) works, but without the ability to throw BufferOverflowExceptions
											//I don't need a BufferOverflow - if there's more data to be read, the readBuffer will be compacted and the loop will go on
											int n = Math.min(packetBuffer.remaining(), readBuffer.remaining());
											for (int i = 0; i < n; i++) {
												packetBuffer.put(readBuffer.get());
											}
											if (!packetBuffer.hasRemaining()) {
												packetBuffer.flip();
												//Read the full message
												byte messageType = packetBuffer.get(); //We read off the 'messageType' byte
												int messageIDInt = packetBuffer.getInt(); //Then the messageID
												MessageID messageID = new MessageID(messageIDInt, sc.getRemoteAddress());
												int transferPubKeyLength = packetBuffer.getInt(); //Then, the length of the publicKey that may or may not be attached
												byte[] transferPubKey = _requestorPublicKeys.get(messageID); //If I'm receiving a 2-n request, I can get it this way
												if (transferPubKeyLength > 0) {
													//If I'm receiving a first request, I should get it this way
													transferPubKey = new byte[transferPubKeyLength];
													packetBuffer.get(transferPubKey); //They've sent a public key. We need to do a Diffie.
													_requestorPublicKeys.put(messageID, transferPubKey);
												}
												SecretKey messageKey;
												try {
													if (transferPubKey == null) {
														//I'm receiving a response
														decrementRequestResponseSplit(messageID);
														transferPubKey = _transferPublicKeys.get(sc.getRemoteAddress());
														//Build a request-type MessageKey
														messageKey = buildRequestKey(transferPubKey, messageID);
													} else {
														//I'm receiving a request, build a response-type MessageKey
														incrementRequestResponseSplit(messageID);
														_replyConnections.put(messageID, sc);
														if (_chunkTransferKeyPair != null) {
															messageKey = buildResponseKey(transferPubKey);
														} else {
															System.out.println("Got a request, but I have no chunk transfer keypair - sending error response");
															_handler.error("503: Cannot serve requests", true, messageID);
															readBuffer.compact();
															break;
														}
													}
												} catch (InvalidKeySpecException e) {
													throw new RuntimeException("", e);
												}
												ProtocolMessage message;
												try {
													packetBuffer.getInt(); //Read off the data length int, which we don't need because we've already sorted that out.
													message = decryptAndReconstituteMessage(messageType, packetBuffer, messageKey);
												} catch (InvalidMessageException | InvalidKeyException e) {
													throw new RuntimeException("", e);
												}
												if (message.isRequest()) {
													_handler.requestReceived(message, messageID);
												} else {
													_handler.responseReceived(message);
												}
											} else {
												_packetReadBuffers.put(sc, packetBuffer);
											}
										}
									}
									readBuffer.compact();
								}
							} else if (key.isWritable()) {
								SocketChannel sc = (SocketChannel) key.channel();
								ArrayList<ByteBuffer> writeBuffers = _writeBuffers.remove(sc);
								ByteBuffer toWrite = null;
								if (writeBuffers == null || writeBuffers.isEmpty()) {
									sc.register(_selector, SelectionKey.OP_READ);
									continue;
								}
								do {
									toWrite = writeBuffers.isEmpty() ? null : writeBuffers.remove(0);
								} while (toWrite != null && !toWrite.hasRemaining());
								if (toWrite == null) {
									key.interestOps(SelectionKey.OP_READ);
								} else {
									sc.write(toWrite);
									if (toWrite.hasRemaining()) {
										writeBuffers.add(0, toWrite);
									}
									if (!writeBuffers.isEmpty()) {
										_writeBuffers.put(sc, writeBuffers);
									} else {
										key.interestOps(SelectionKey.OP_READ);
									}
								}
							} else {
								System.out.println("Invalid selection key");
							}
							selectionKeyIterator.remove();
						}
					}
					//Register All socket channels for write that have things that need to be written
					Set<SocketChannel> channelsWithPendingWrites = _writeBuffers.keySet();
					for (SocketChannel writeChannel : channelsWithPendingWrites) {
						writeChannel.register(_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
				Thread t = Thread.currentThread();
				t.getUncaughtExceptionHandler().uncaughtException(t, e);
			} finally {
				stop();
			}
		}

		void stop() {
			run = false;
			if (_selector != null && _selector.isOpen()) {
				try {
					_selector.close();
				} catch (IOException e) {
					//NOP
				}
			}
			if (_serverSocket != null && _serverSocket.isOpen()) {
				try {
					_serverSocket.close();
				} catch (IOException e) {
					//NOP
				}
			}
		}
	}
}
