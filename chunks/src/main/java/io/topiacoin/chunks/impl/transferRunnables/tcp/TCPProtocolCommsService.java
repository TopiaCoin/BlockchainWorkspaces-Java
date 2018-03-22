package io.topiacoin.chunks.impl.transferRunnables.tcp;

import io.topiacoin.chunks.exceptions.InvalidSignatureException;
import io.topiacoin.chunks.intf.ProtocolCommsHandler;
import io.topiacoin.chunks.intf.ProtocolCommsService;
import io.topiacoin.chunks.model.protocol.ProtocolDataMessage;
import io.topiacoin.chunks.model.protocol.ProtocolJsonRequest;
import io.topiacoin.chunks.model.protocol.ProtocolJsonResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TCPProtocolCommsService implements ProtocolCommsService {
	private PublicKey _publicKey;
	private PrivateKey _privateKey;
	private Thread _listenerThread;
	private TCPListenerRunnable _listenerRunnable;
	private Map<SocketChannel, ByteBuffer> _packetReadBuffers = new HashMap<>();
	private Map<SocketChannel, ArrayList<ByteBuffer>> _writeBuffers = new HashMap<>();
	private Map<SocketAddress, Integer> _messageIDs = new HashMap<>();
	private Map<Integer, SocketAddress> _replyAddresses = new HashMap<>();
	private Map<Integer, SocketChannel> _connections = new HashMap<>();
	private Map<SocketAddress, Integer> _requestResponseSplits = new HashMap<>();
	private int _messageIdTracker = 0;
	private ProtocolCommsHandler _handler = null;

	public TCPProtocolCommsService(int port, PublicKey publicKey, PrivateKey privateKey) throws IOException {
		_publicKey = publicKey;
		_privateKey = privateKey;
		_listenerRunnable = new TCPListenerRunnable(port);
		_listenerThread = new Thread(_listenerRunnable);
		_listenerThread.setDaemon(true);
	}

	@Override public int sendMessage(String location, int port, ProtocolMessage message)
			throws InvalidSignatureException, SignatureException, InvalidKeyException, IOException {
		if (message instanceof ProtocolJsonRequest) {
			InetSocketAddress addr = new InetSocketAddress(location, port);
			SocketChannel sc = null;
			Integer messageID = _messageIDs.get(addr);
			if (messageID != null) {
				sc = _connections.get(messageID);
				if (sc == null || !sc.isConnected()) {
					removeMessageID(messageID);
					messageID = null;
				}
			}
			if (messageID == null) {
				messageID = _messageIdTracker++;
				sc = SocketChannel.open(addr);
				sc.configureBlocking(false);
				addMessageID(messageID, addr, sc);
			}
			ByteBuffer data = message.encodeMessage(_privateKey, messageID);
			ArrayList<ByteBuffer> dataList = _writeBuffers.get(sc);
			dataList = dataList == null ? new ArrayList<ByteBuffer>() : dataList;
			dataList.add(data);
			incrementRequestResponseSplit(addr);
			_writeBuffers.put(sc, dataList);
			_listenerRunnable.wakeupSelector();
			return messageID;
		} else {
			throw new UnsupportedOperationException("Cannot send non-request type message as a request");
		}
	}

	private int incrementRequestResponseSplit(SocketAddress addr) {
		Integer rrSplit = _requestResponseSplits.get(addr);
		rrSplit = (rrSplit == null) ? 0 : rrSplit;
		_requestResponseSplits.put(addr, ++rrSplit);
		return rrSplit;
	}

	private int decrementRequestResponseSplit(SocketAddress addr) {
		Integer rrSplit = _requestResponseSplits.get(addr);
		rrSplit = (rrSplit == null) ? 1 : rrSplit;
		_requestResponseSplits.put(addr, --rrSplit);
		return rrSplit;
	}

	private void addMessageID(int messageID, SocketAddress addr, SocketChannel channel) {
		_messageIDs.put(addr, messageID);
		_replyAddresses.put(messageID, addr);
		_connections.put(messageID, channel);
	}

	private void removeMessageID(Integer messageID) {
		if (messageID != null) {
			SocketChannel channel = _connections.remove(messageID);
			if (channel != null && channel.isConnected()) {
				try {
					channel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			_requestResponseSplits.remove(messageID);
			_replyAddresses.remove(messageID);
		}
	}

	@Override public void reply(ProtocolMessage message, int messageID) throws SignatureException, InvalidKeyException {
		if (message instanceof ProtocolJsonResponse || message instanceof ProtocolDataMessage) {
			SocketAddress addr = _replyAddresses.get(messageID);
			if (addr != null) {
				SocketChannel sc = _connections.get(messageID);
				if (sc == null || !sc.isConnected()) {
					try {
						sc.close();
					} catch (IOException e) {
						//nop
					}
					try {
						sc = SocketChannel.open(addr);
						sc.configureBlocking(false);
						_connections.put(messageID, sc);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				ByteBuffer data = message.encodeMessage(_privateKey, messageID);
				ArrayList<ByteBuffer> dataList = _writeBuffers.get(sc);
				dataList = dataList == null ? new ArrayList<ByteBuffer>() : dataList;
				dataList.add(data);
				_writeBuffers.put(sc, dataList);
				_listenerRunnable.wakeupSelector();
				decrementRequestResponseSplit(addr);
			} else {
				throw new IllegalStateException("Cannot reply for message " + messageID + " because I could not find a reply address");
			}
		} else {
			throw new UnsupportedOperationException("Cannot send non-response type message as a response");
		}
	}

	@Override public void setHandler(ProtocolCommsHandler handler) {
		_handler = handler;
	}

	public void start() {
		if (_handler == null) {
			throw new IllegalStateException("Cannot start without a message handler");
		}
		_listenerThread.start();
	}

	public void stop() {
		if (_listenerRunnable != null) {
			_listenerRunnable.stop();
		}
		if (_listenerThread != null) {
			_listenerThread.interrupt();
		}
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
								ByteBuffer readBuffer = ByteBuffer.allocate(100);
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
											int size = ProtocolMessage.getMessageLength(readBuffer);
											if (size == -1) {
												readBufferIsUsable = false;
											} else {
												packetBuffer = ByteBuffer.allocate(size);
											}
										}
										if (readBufferIsUsable) {
											_packetReadBuffers.remove(sc);
											packetBuffer.put(readBuffer);
											if (!packetBuffer.hasRemaining()) {
												try {
													packetBuffer.flip();
													ProtocolMessage message = ProtocolMessage.decodeMessage(packetBuffer, _publicKey);
													if(message instanceof ProtocolJsonRequest) {
														incrementRequestResponseSplit(sc.getRemoteAddress());
														addMessageID(message.getMessageID(), sc.getRemoteAddress(), sc);
														_handler.requestReceived(message);
													} else {
														decrementRequestResponseSplit(sc.getRemoteAddress());
														_handler.responseReceived(message);
													}
												} catch (InvalidSignatureException e) {
													e.printStackTrace();
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
			} catch (IOException e) {
				e.printStackTrace();
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
