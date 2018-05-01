package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.exceptions.CommsListenerNotStartedException;
import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.exceptions.InvalidMessageIDException;
import io.topiacoin.chunks.exceptions.UnknownMessageTypeException;
import io.topiacoin.chunks.intf.ProtocolCommsHandler;
import io.topiacoin.chunks.intf.ProtocolCommsResponseHandler;
import io.topiacoin.chunks.intf.ProtocolCommsService;
import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ErrorProtocolResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.ProtocolMessageFactory;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.model.MemberNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPProtocolCommsService implements ProtocolCommsService {
	private static final Log _log = LogFactory.getLog(TCPProtocolCommsService.class);
	private static final long _unusedConnectionCloseThresholdMillis = 15000;
	private final KeyPair _chunkTransferKeyPair;
	private int _listenerPort;
	Thread _listenerThread;
	TCPListenerRunnable _listenerRunnable;
	private Throwable _listenerRunnableThrowable = null;

	final Map<MessageID, SocketAddress> _messageAddresses = new HashMap<>();
	final Map<SocketAddress, ProtocolConnectionState> _connections = new HashMap<>();
	private final Map<MessageID, ProtocolCommsResponseHandler> _messageSpecificHandlers = new HashMap<>();
	private final Map<MessageID, Long> _messageSendTimes = new HashMap<>();

	private ProtocolMessageFactory _messageFactory;
	AtomicInteger _messageIdTracker = new AtomicInteger();
	private ProtocolCommsHandler _handler = null;
	private long _timeoutMs = 30000;

	TCPProtocolCommsService(int port, KeyPair chunkTransferKeyPair) throws IOException {
		_listenerPort = port;
		_chunkTransferKeyPair = chunkTransferKeyPair;
		_listenerRunnable = new TCPListenerRunnable(port);
		_messageFactory = new ProtocolMessageFactory();
	}

	@Override public MessageID sendMessage(MemberNode targetNode, ProtocolMessage message, ProtocolCommsResponseHandler handler)
			throws InvalidKeyException, IOException, InvalidMessageException, CommsListenerNotStartedException {
		if (message.isRequest()) {
			if (message.isValid()) {
				if (_listenerThread != null && _listenerThread.isAlive()) {
					InetSocketAddress addr = new InetSocketAddress(targetNode.getHostname(), targetNode.getPort());
					MessageID messageID = new MessageID(_messageIdTracker.getAndIncrement(), addr);
					_messageAddresses.put(messageID, addr);
					if (handler != null) {
						_messageSpecificHandlers.put(messageID, handler);
					}
					synchronized (_messageSendTimes) {
						_messageSendTimes.put(messageID, System.currentTimeMillis());
					}

					ProtocolConnectionState state = _connections.get(addr);
					if (state == null) {
						try {
							state = new ProtocolConnectionState(addr, messageID, targetNode.getPublicKey());
						} catch (InvalidKeySpecException e) {
							throw new InvalidKeyException("Public Key Data was invalid", e);
						}
					} else {
						state.addMessageID(messageID);
					}
					_connections.put(addr, state);

					ByteBuffer data = null;
					try {
						data = encryptAndFrameMessage(message, messageID, state);
					} catch (UnknownMessageTypeException e) {
						throw new InvalidMessageException("Cannot send message", e);
					}
					state.addWriteBuffer(data);
					_listenerRunnable.wakeup();
					return messageID;
				} else {
					throw new CommsListenerNotStartedException("Listener must be started before sending messages");
				}
			} else {
				throw new InvalidMessageException("Message not valid, will not send");
			}
		} else {
			throw new InvalidMessageException("Cannot send non-request type message as a request");
		}
	}

	ByteBuffer encryptAndFrameMessage(ProtocolMessage message, MessageID messageID, ProtocolConnectionState state)
			throws InvalidKeyException, UnknownMessageTypeException {
		//a fully framed Message consists of the following
		//messageType, a byte
		//messageId, an int
		//Transfer Public Key Length, an int (which is set to 0 if no Transfer Public Key is provided,
		//Transfer Public Key, bytes, if provided
		//Data Length, an int, the length of the encrypted message data.
		Byte messageType = _messageFactory.getMessageByteIdentifier(message);
		int transferPubKeyLength = state.getMyPublicKey() == null ? 0 : state.getMyPublicKey().length;
		ByteBuffer data = message.toBytes();
		//Error Messages cannot be reliably encrypted, so we don't encrypt them. Be sure to not leak secret data in the error messages.
		boolean shouldEncryptMessage = !(message instanceof ErrorProtocolResponse);
		if (shouldEncryptMessage) {
			try {
				_log.debug("Encrypting: " + (data.remaining() > 5000 ? "<a lot of data> " : DatatypeConverter.printHexBinary(message.toBytes().array())));
				_log.debug("With Key: " + DatatypeConverter.printHexBinary(state.getMessageKey().getEncoded()));
				byte[] encrypted = CryptoUtils.encryptWithSecretKey(data.array(), state.getMessageKey(), null);
				ByteBuffer toReturn = ByteBuffer.allocate(1 + Integer.BYTES + Integer.BYTES + transferPubKeyLength + Integer.BYTES + encrypted.length);
				toReturn.put(messageType);
				toReturn.putInt(messageID.getId());
				toReturn.putInt(transferPubKeyLength);
				if (transferPubKeyLength > 0) {
					toReturn.put(state.getMyPublicKey());
				}
				toReturn.putInt(encrypted.length).put(encrypted);
				_log.debug("Encrypted: " + (encrypted.length > 5000 ? "<a lot of data>" : DatatypeConverter.printHexBinary(encrypted)));
				toReturn.flip();
				return toReturn;
			} catch (CryptographicException e) {
				throw new RuntimeException("Crypto failure", e);
			}
		} else {
			_log.debug("NOT Encrypting Error");
			int dataLength = data.array().length;
			ByteBuffer toReturn = ByteBuffer.allocate(1 + Integer.BYTES + Integer.BYTES + Integer.BYTES + dataLength);
			data.flip();
			toReturn.put(messageType);
			toReturn.putInt(messageID.getId());
			toReturn.putInt(0);
			toReturn.putInt(dataLength);
			toReturn.put(data);
			toReturn.flip();
			return toReturn;
		}
	}

	private ProtocolMessage decryptAndReconstituteMessage(byte messageType, ByteBuffer messageData, SecretKey responseKey)
			throws InvalidKeyException, InvalidMessageException {
		try {
			byte[] messageArray = new byte[messageData.remaining()];
			messageData.get(messageArray);
			ByteBuffer decryptedBuffer;
			if (!_messageFactory.isError(messageType)) {
				_log.debug("Decrypting: " + (messageArray.length > 5000 ? "<a lot of data>" : DatatypeConverter.printHexBinary(messageArray)));
				_log.debug("With Key: " + DatatypeConverter.printHexBinary(responseKey.getEncoded()));
				byte[] decrypted = CryptoUtils.decryptWithSecretKey(messageArray, responseKey, null);
				_log.debug("Decrypted: " + (decrypted.length > 5000 ? "<a lot of data>" : DatatypeConverter.printHexBinary(decrypted)));
				decryptedBuffer = ByteBuffer.wrap(decrypted);
			} else {
				_log.debug("NOT Decrypting Error");
				decryptedBuffer = ByteBuffer.wrap(messageArray);
			}
			return _messageFactory.getMessage(messageType, decryptedBuffer);
		} catch (CryptographicException | UnknownMessageTypeException e) {
			throw new InvalidMessageException("Failed to parse message", e);
		}
	}

	@Override public void reply(ProtocolMessage message, MessageID messageID) throws InvalidMessageException, CommsListenerNotStartedException, InvalidMessageIDException {
		if (!message.isRequest()) {
			if (message.isValid()) {
				if (_listenerThread != null &&_listenerThread.isAlive()) {
					SocketAddress addr = _messageAddresses.get(messageID);
					if (addr != null) {
						ProtocolConnectionState state = _connections.get(addr);
						if (state != null) {
							try {
								if (!(message instanceof ErrorProtocolResponse)) {
									state.buildMessageKey(_chunkTransferKeyPair);
								}
								ByteBuffer data = encryptAndFrameMessage(message, messageID, state);
								state.addWriteBuffer(data);
								state.removeMessageID(messageID);
								_messageAddresses.remove(messageID);
								_listenerRunnable.wakeup();
							} catch (InvalidKeyException e) {
								throw new IllegalStateException("Cannot reply to message " + messageID + " because of crypto errors", e);
							} catch (UnknownMessageTypeException e) {
								throw new InvalidMessageException("Cannot reply with this message", e);
							}
						} else {
							throw new InvalidMessageIDException("Could not find connection for MessageID");
						}
					} else {
						throw new InvalidMessageIDException("Could not find address for MessageID");
					}
				} else {
					throw new CommsListenerNotStartedException("Listener must be started before sending messages");
				}
			} else {
				throw new InvalidMessageException("Message not valid, will not send");
			}
		} else {
			throw new InvalidMessageException("Cannot send non-response type message as a response");
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
		if(_listenerThread != null) {
			throw new IllegalStateException("Cannot Start Listener - it's already started");
		}
		Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
			@Override public void uncaughtException(Thread t, Throwable e) {
				_handler.error(e);
				setListenerRunnableThrowable(e);
			}
		};
		_listenerThread = new Thread(_listenerRunnable, "ProtocolComms:" + _listenerPort);
		_listenerThread.setDaemon(true);
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
			try {
				_listenerThread.join(3000);
				if (_listenerThread.isAlive()) {
					_listenerThread.interrupt();
				}
			} catch (InterruptedException e) {
				//NOP
			}
		}
		_listenerThread = null;
		_listenerRunnableThrowable = null;
		_messageAddresses.clear();
		_messageSpecificHandlers.clear();
		synchronized (_messageSendTimes) {
			_messageSendTimes.clear();
		}
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

	@Override public void setTimeout(int timeout, TimeUnit unit) {
		_timeoutMs = unit.toMillis(timeout);
	}

	SocketChannel getConnectionForMessageID(MessageID messageID) {
		ProtocolConnectionState state = _connections.get(messageID.getAddress());
		return state == null ? null : state.getSocketChannel();
	}

	class TCPListenerRunnable implements Runnable {
		final InetSocketAddress _hostAddress;
		Selector _selector;
		ServerSocketChannel _serverSocket;
		private boolean isRunning;

		TCPListenerRunnable(int port) throws IOException {
			_hostAddress = new InetSocketAddress("127.0.0.1", port);
		}

		void wakeup() {
			if(_selector != null) {
				_selector.wakeup();
			} else {
				throw new IllegalStateException("Cannot wake stopped runnable");
			}
		}

		@Override public void run() {
			try {
				isRunning = true;
				_selector = Selector.open();
				_serverSocket = ServerSocketChannel.open();
				_serverSocket.bind(_hostAddress);
				_serverSocket.configureBlocking(false);
				_serverSocket.register(_selector, _serverSocket.validOps(), null);
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				List<MessageID> timeouts = new LinkedList<>();
				while (isRunning) {
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
													readBuffer.get(); //We read off the 'messageType' byte but ignore its value
													readBuffer.getInt(); //Then the messageID, it's value is also ignored for now
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
													int messageIDInt = packetBuffer.getInt(); //Then the messageID
													MessageID messageID = new MessageID(messageIDInt, sc.getRemoteAddress());
													int transferPubKeyLength = packetBuffer.getInt(); //Then, the length of the publicKey that may or may not be attached
													//If I'm receiving a first request, I should get it this way
													byte[] transferPubKey = new byte[transferPubKeyLength];
													packetBuffer.get(transferPubKey); //They've sent a public key. We need to do a Diffie.
													if (_messageFactory.isRequest(messageType)) {
														//buildResponseKey
														connection.setTheirPublicKey(transferPubKey);
														connection.requestReceived(messageID);
														_messageAddresses.put(messageID, sc.getRemoteAddress());
														if (_chunkTransferKeyPair != null) {
															connection.requestReceived(messageID);
															connection.buildMessageKey(_chunkTransferKeyPair);
														} else {
															_log.warn("Got a request, but I have no chunk transfer keypair - sending error response");
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
														ProtocolCommsResponseHandler handler = _messageSpecificHandlers.remove(messageID);
														_messageSendTimes.remove(messageID);
														if (handler != null) {
															handler.responseReceived(message, messageID);
														} else {
															_handler.responseReceived(message, messageID);
														}
													}
												}
											}
										}
										readBuffer.compact();
									}
								} else if (key.isWritable()) {
									SocketAddress address = ((SocketChannel) key.channel()).getRemoteAddress();
									ProtocolConnectionState connection = _connections.get(address);
									connection.write();
								} else {
									_log.error("Invalid selection key");
								}
							}
							selectionKeyIterator.remove();
						}
					}
					synchronized (_messageSendTimes) {
						Iterator<MessageID> messageIDIterator = _messageSendTimes.keySet().iterator();
						while (isRunning && messageIDIterator.hasNext()) {
							MessageID messageID = messageIDIterator.next();
							if (System.currentTimeMillis() > _messageSendTimes.get(messageID) + _timeoutMs) {
								timeouts.add(messageID);
								ProtocolCommsResponseHandler handler = _messageSpecificHandlers.remove(messageID);
								messageIDIterator.remove();
								if (handler != null) {
									handler.error("Request Timed out", false, messageID);
								} else {
									_handler.error("Request Timed out", false, messageID);
								}
							}
						}
					}
					//Register All socket channels for write that have things that need to be written
					Iterator<SocketAddress> addresses = _connections.keySet().iterator();
					while (isRunning && addresses.hasNext()) {
						SocketAddress address = addresses.next();
						ProtocolConnectionState state = _connections.get(address);
						for (MessageID tMid : timeouts) {
							state.removeMessageID(tMid);
						}
						state.registerForPendingWrites(_selector);
						if (!state.hasMessageIDs() && !state.hasWriteBuffers()
								&& state.getLastUsedTime() + _unusedConnectionCloseThresholdMillis < System.currentTimeMillis()) {
							if (state.getSocketChannel() != null) {
								state.getSocketChannel().close();
							}
							addresses.remove();
						}
					}
					timeouts.clear();
				}
			} catch (Throwable e) {
				_log.error("", e);
				Thread t = Thread.currentThread();
				t.getUncaughtExceptionHandler().uncaughtException(t, e);
			} finally {
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

		void stop() {
			isRunning = false;
			wakeup();
		}
	}
}
