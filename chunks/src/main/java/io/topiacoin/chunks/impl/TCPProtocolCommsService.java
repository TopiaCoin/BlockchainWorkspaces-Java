package io.topiacoin.chunks.impl;

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
import io.topiacoin.chunks.model.protocol.ProtocolConnectionState;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolRequest;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
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
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TCPProtocolCommsService implements ProtocolCommsService {
	private static final long _unusedConnectionCloseThresholdMillis = 15000;
	private KeyPair _chunkTransferKeyPair;
	Thread _listenerThread;
	TCPListenerRunnable _listenerRunnable;
	private Throwable _listenerRunnableThrowable = null;

	Map<MessageID, SocketAddress> _messageAddresses = new HashMap<>();
	Map<SocketAddress, ProtocolConnectionState> _connections = new HashMap<>();

	private Map<String, Byte> _messageTypes = new HashMap<>();
	int _messageIdTracker = 0;
	private ProtocolCommsHandler _handler = null;

	TCPProtocolCommsService(int port, KeyPair chunkTransferKeyPair) throws IOException {
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
		_messageAddresses.put(messageID, addr);
		ProtocolConnectionState state = _connections.get(addr);
		if (state == null) {
			try {
				state = new ProtocolConnectionState(addr, messageID, transferPublicKey);
			} catch (InvalidKeySpecException e) {
				throw new InvalidKeyException("Public Key Data was invalid", e);
			}
		} else {
			state.addMessageID(messageID);
		}
		state.reconnectIfNecessary(addr);
		_connections.put(addr, state);

		ByteBuffer data = encryptAndFrameMessage(message, messageID, state);
		data.flip();
		state.addWriteBuffer(data, false);
		_listenerRunnable.wakeupSelector();
		return messageID;
	}

	ByteBuffer encryptAndFrameMessage(ProtocolMessage message, MessageID messageID, ProtocolConnectionState state)
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
		int transferPubKeyLength = state.getMyPublicKey() == null ? 0 : state.getMyPublicKey().length;
		ByteBuffer data = message.toBytes();
		if (!(message instanceof ErrorProtocolResponse)) {
			try {
				System.out.println(
						"Encrypting: " + (data.remaining() > 5000 ? "<a lot of data> " : DatatypeConverter.printHexBinary(message.toBytes().array())));
				System.out.println("With Key: " + DatatypeConverter.printHexBinary(state.getMessageKey().getEncoded()));
				Cipher cipher = Cipher.getInstance("AES");
				cipher.init(Cipher.ENCRYPT_MODE, state.getMessageKey());
				byte[] encrypted = cipher.doFinal(data.array());
				ByteBuffer toReturn = ByteBuffer.allocate(1 + Integer.BYTES + Integer.BYTES + transferPubKeyLength + Integer.BYTES + encrypted.length);
				toReturn.put(messageType).putInt(messageID.getId()).putInt(transferPubKeyLength);
				if (transferPubKeyLength > 0) {
					toReturn.put(state.getMyPublicKey());
				}
				toReturn.putInt(encrypted.length).put(encrypted);
				System.out.println("Encrypted: " + (encrypted.length > 5000 ? "<a lot of data>" : DatatypeConverter.printHexBinary(encrypted)));
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
		SocketAddress addr = _messageAddresses.get(messageID);
		if (addr != null) {
			ProtocolConnectionState state = _connections.get(addr);
			if (state != null) {
				try {
					if (!(message instanceof ErrorProtocolResponse)) {
						state.buildMessageKey(_chunkTransferKeyPair);
					}
					ByteBuffer data = encryptAndFrameMessage(message, messageID, state);
					data.flip();
					state.addWriteBuffer(data, true);
					state.removeMessageID(messageID);
					_messageAddresses.remove(messageID);
					_listenerRunnable.wakeupSelector();
				} catch (InvalidKeySpecException e) {
					throw new IllegalStateException("Cannot reply to message " + messageID + " because the cached publicKey is invalid", e);
				} catch (InvalidKeyException e) {
					throw new IllegalStateException("Cannot reply to message " + messageID + " because of crypto errors", e);
				}
			} else {
				throw new InvalidMessageIDException("Could not find connection for MessageID");
			}
		} else {
			throw new InvalidMessageIDException("Could not find address for MessageID");
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
			//NOP
		}
		if (_listenerRunnableThrowable != null) {
			Throwable t = _listenerRunnableThrowable;
			stop();
			throw new FailedToStartCommsListenerException("Comms listener failed to startListener", t);
		}
	}

	public void stop() {
		if (_listenerRunnable != null) {
			_listenerRunnable.stop();
		}
		if (_listenerThread != null) {
			_listenerThread.interrupt();
		}
		_listenerRunnableThrowable = null;
		_messageAddresses.clear();
		for (SocketAddress address : _connections.keySet()) {
			ProtocolConnectionState state = _connections.get(address);
			try {
				state.getSocketChannel().close();
			} catch (IOException e) {
				//NOP
			}
		}
		_connections.clear();
	}

	SocketChannel getConnectionForMessageID(MessageID messageID) {
		ProtocolConnectionState state = _connections.get(messageID.getAddress());
		return state == null ? null : state.getSocketChannel();
	}

	protected class TCPListenerRunnable implements Runnable {
		final InetSocketAddress _hostAddress;
		final Selector _selector;
		ServerSocketChannel _serverSocket;
		private boolean run = true;

		TCPListenerRunnable(int port) throws IOException {
			_hostAddress = new InetSocketAddress("127.0.0.1", port);
			_selector = Selector.open();
		}

		void wakeupSelector() {
			_selector.wakeup();
		}

		@Override public void run() {
			try {
				_serverSocket = ServerSocketChannel.open();
				_serverSocket.bind(_hostAddress);
				_serverSocket.configureBlocking(false);
				_serverSocket.register(_selector, _serverSocket.validOps(), null);
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);

				while (run) {
					if (_selector.select(1000) > 0) {
						final Iterator<SelectionKey> selectionKeyIterator;
						synchronized (_selector) {
							selectionKeyIterator = _selector.selectedKeys().iterator();
						}
						Thread.yield();
						while (selectionKeyIterator.hasNext()) {
							final SelectionKey key = selectionKeyIterator.next();
							if (key.isValid()) {
								if (key.isAcceptable()) {
									SocketChannel channel = _serverSocket.accept();
									_connections.put(channel.getRemoteAddress(), new ProtocolConnectionState(channel, _selector));
								} else if (key.isReadable()) {
									SocketAddress address = ((SocketChannel) key.channel()).getRemoteAddress();
									ProtocolConnectionState connection = _connections.get(address);
									SocketChannel sc = (SocketChannel) key.channel();
									int bytesRead = -1;
									if (sc.isConnected()) {
										bytesRead = sc.read(readBuffer);
									}
									if (bytesRead == -1) {
										//Connection lost
										sc.close();
										_connections.remove(address);
									} else if (bytesRead > 0) {
										readBuffer.flip();
										boolean readBufferIsUsable = true;
										while (readBuffer.hasRemaining() && readBufferIsUsable) {
											if (!connection.hasPacketBuffer()) {
												//Determine the size of the message.
												readBuffer.mark(); //First, we mark the buffer so we can reset it at the end
												try {
													int msgType = readBuffer.get(); //We read off the 'messageType' byte but ignore its value
													int msgId = readBuffer.getInt(); //Then the messageID, it's value is also ignored for now
													int transferPubKeyLength = readBuffer.getInt(); //Then, the length of the publicKey that may or may not be attached - we need this
													readBuffer.get(new byte[transferPubKeyLength]); //We'll read past the publicKey if it exists - we don't need its value right now.
													int dataLength = readBuffer.getInt(); //Finally, we'll read the data length int - if we've made it this far, we can allocate the buffer.
													connection.allocatePacketBuffer(
															1 + Integer.BYTES + Integer.BYTES + transferPubKeyLength + Integer.BYTES + dataLength);
												} catch (BufferUnderflowException ex) {
													//Ok, we couldn't read off enough data to determine how big to make the packetBuffer, so we'll try again next time.
													readBufferIsUsable = false;
												} finally {
													readBuffer.reset(); //We always reset the buffer.
												}
											}
											if (connection.hasPacketBuffer()) { //If it doesn't have one at this point, there wasn't enough data to determine the correct size for the packet buffer, try again next time.
												ByteBuffer packetBuffer = connection.read(readBuffer); //Returns a buffer only if it's full
												if (packetBuffer != null) {
													packetBuffer.flip();
													//Read the full message
													byte messageType = packetBuffer.get(); //We read off the 'messageType' byte
													boolean isRequest = messageType == 0x01 || messageType == 0x03;
													int messageIDInt = packetBuffer.getInt(); //Then the messageID
													MessageID messageID = new MessageID(messageIDInt, sc.getRemoteAddress());
													int transferPubKeyLength = packetBuffer.getInt(); //Then, the length of the publicKey that may or may not be attached
													//If I'm receiving a first request, I should get it this way
													byte[] transferPubKey = new byte[transferPubKeyLength];
													packetBuffer.get(transferPubKey); //They've sent a public key. We need to do a Diffie.
													if (isRequest) {
														//buildResponseKey
														connection.setTheirPublicKey(transferPubKey);
														connection.requestReceived(messageID);
														_messageAddresses.put(messageID, sc.getRemoteAddress());
														if (_chunkTransferKeyPair != null) {
															connection.requestReceived(messageID);
															connection.buildMessageKey(_chunkTransferKeyPair);
														} else {
															System.out.println("Got a request, but I have no chunk transfer keypair - sending error response");
															_handler.error("503: Cannot serve requests", true, messageID);
															break;
														}
													} else {
														//The messageKey should have already been constructed from when I made the request
														connection.responseReceived(messageID);
														_messageAddresses.remove(messageID);
													}
													ProtocolMessage message;
													try {
														packetBuffer.getInt(); //Read off the data length int, which we don't need because we've already sorted that out.
														message = decryptAndReconstituteMessage(messageType, packetBuffer, connection.getMessageKey());
													} catch (InvalidMessageException | InvalidKeyException e) {
														throw new RuntimeException("", e);
													}
													if (message.isRequest()) {
														_handler.requestReceived(message, messageID);
													} else {
														_handler.responseReceived(message);
													}
												}
											}
										}
										readBuffer.compact();
									}
								} else if (key.isWritable()) {
									ProtocolConnectionState connection = _connections.get(((SocketChannel) key.channel()).getRemoteAddress());
									connection.write();
								} else {
									System.out.println("Invalid selection key");
								}
							}
							selectionKeyIterator.remove();
						}
					}
					//Register All socket channels for write that have things that need to be written
					Iterator<SocketAddress> addresses = _connections.keySet().iterator();
					while (addresses.hasNext()) {
						SocketAddress address = addresses.next();
						ProtocolConnectionState state = _connections.get(address);
						state.registerForPendingWrites(_selector);
						if (!state.hasMessageIDs() && !state.hasWriteBuffers()
								&& state.getLastUsedTime() + _unusedConnectionCloseThresholdMillis < System.currentTimeMillis()) {
							if (state.getSocketChannel() != null) {
								state.getSocketChannel().close();
							}
							addresses.remove();
						}
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
