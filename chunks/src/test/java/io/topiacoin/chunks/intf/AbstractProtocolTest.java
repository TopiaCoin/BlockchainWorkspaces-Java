package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.exceptions.InvalidMessageIDException;
import io.topiacoin.chunks.exceptions.UnknownMessageTypeException;
import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ErrorProtocolResponse;
import io.topiacoin.chunks.model.protocol.FetchChunkProtocolRequest;
import io.topiacoin.chunks.model.protocol.GiveChunkProtocolResponse;
import io.topiacoin.chunks.model.protocol.HaveChunksProtocolResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.ProtocolMessageFactory;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolRequest;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.model.MemberNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractProtocolTest {

	protected abstract ProtocolCommsService getProtocolCommsService(int port, KeyPair transferKeyPair) throws IOException;

	protected abstract SocketChannel getConnectionForMessageID(ProtocolCommsService service, MessageID id);

	@Test
	public void testQueryChunksRequestAndResponse() throws Exception {
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userASigningKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userBSigningKeyPair = CryptoUtils.generateECKeyPair();
		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);
		try {
			final String[] testChunks = new String[] { "foo", "bar", "baz" };
			final CountDownLatch lock = new CountDownLatch(2);

			String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
			MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);

			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID i) {
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof HaveChunksProtocolResponse);
					HaveChunksProtocolResponse message = (HaveChunksProtocolResponse) response;
					assertEquals("request_type wrong", "HAVE_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunkIDs(), testChunks));
					assertEquals("userID wrong", "userB", message.getUserID());
					try {
						assertTrue("Message signature verification failed", response.verify(userBSigningKeyPair.getPublic()));
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Message signing key invalid");
					} catch (SignatureException e) {
						fail("Didn't expect a sig exception");
					}
					lock.countDown();
				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					assertTrue("Message wrong type", request instanceof QueryChunksProtocolRequest);
					QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
					assertEquals("request_type wrong", "QUERY_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
					assertEquals("userID wrong", "userA", message.getUserID());
					assertEquals("AuthToken wrong", userBAuthToken, message.getAuthToken());
					try {
						assertTrue("Message signature verification failed", request.verify(userASigningKeyPair.getPublic()));
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Message signing key invalid");
					} catch (SignatureException e) {
						fail("Didn't expect a sig exception");
					}
					lock.countDown();
					ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
					try {
						resp.sign(userBSigningKeyPair.getPrivate());
						userBservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Couldn't sign");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			testMessage.sign(userASigningKeyPair.getPrivate());
			MessageID messageID = userAservice.sendMessage(userBNode, testMessage, convertHandler(handlerA));
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
			boolean connectionOpen = true;
			for (int i = 0; i < 20 && connectionOpen; i++) {
				Thread.sleep(100 * i);
				SocketChannel sc = getConnectionForMessageID(userAservice, messageID);
				connectionOpen = sc != null && sc.isConnected();
			}
			assertTrue("Connection never closed", !connectionOpen);
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testSendMessageWithSpecificHandlerWorks() throws Exception {
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userASigningKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userBSigningKeyPair = CryptoUtils.generateECKeyPair();
		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);
		try {
			final String[] testChunks = new String[] { "foo", "bar", "baz" };
			final CountDownLatch lock = new CountDownLatch(2);

			String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
			MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);

			ProtocolCommsHandler failHandler = new ProtocolCommsHandler() {

				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {
					fail();
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID messageID) {
					fail();
				}

				@Override public void error(Throwable t) {
					fail();
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					fail();
				}
			};

			ProtocolCommsResponseHandler handlerA = new ProtocolCommsResponseHandler() {
				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof HaveChunksProtocolResponse);
					HaveChunksProtocolResponse message = (HaveChunksProtocolResponse) response;
					assertEquals("request_type wrong", "HAVE_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunkIDs(), testChunks));
					assertEquals("userID wrong", "userB", message.getUserID());
					try {
						assertTrue("Message signature verification failed", response.verify(userBSigningKeyPair.getPublic()));
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Message signing key invalid");
					} catch (SignatureException e) {
						fail("Didn't expect a sig exception");
					}
					lock.countDown();
				}

				@Override
				public void error(Throwable t, MessageID messageID) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(failHandler);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					assertTrue("Message wrong type", request instanceof QueryChunksProtocolRequest);
					QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
					assertEquals("request_type wrong", "QUERY_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
					assertEquals("userID wrong", "userA", message.getUserID());
					assertEquals("AuthToken wrong", userBAuthToken, message.getAuthToken());
					try {
						assertTrue("Message signature verification failed", request.verify(userASigningKeyPair.getPublic()));
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Message signing key invalid");
					} catch (SignatureException e) {
						fail("Didn't expect a sig exception");
					}
					lock.countDown();
					ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
					try {
						resp.sign(userBSigningKeyPair.getPrivate());
						userBservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Couldn't sign");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			testMessage.sign(userASigningKeyPair.getPrivate());
			MessageID messageID = userAservice.sendMessage(userBNode, testMessage, handlerA);
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
			boolean connectionOpen = true;
			for (int i = 0; i < 20 && connectionOpen; i++) {
				Thread.sleep(100 * i);
				SocketChannel sc = getConnectionForMessageID(userAservice, messageID);
				connectionOpen = sc != null && sc.isConnected();
			}
			assertTrue("Connection never closed", !connectionOpen);
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testSignAndVerify() throws Exception {
		final KeyPair signingKeypair = CryptoUtils.generateECKeyPair();

		ProtocolMessage testMessage = new QueryChunksProtocolRequest(new String[] { "foo", "bar", "baz" }, "userA", "foo");
		testMessage.sign(signingKeypair.getPrivate());
		assertTrue("Verification failed", testMessage.verify(signingKeypair.getPublic()));
		testMessage = new HaveChunksProtocolResponse(new String[] { "foo", "bar", "baz" }, "userA");
		testMessage.sign(signingKeypair.getPrivate());
		assertTrue("Verification failed", testMessage.verify(signingKeypair.getPublic()));
		testMessage = new FetchChunkProtocolRequest("foo", "userA", "foo");
		testMessage.sign(signingKeypair.getPrivate());
		assertTrue("Verification failed", testMessage.verify(signingKeypair.getPublic()));
		testMessage = new GiveChunkProtocolResponse("foo", new byte[100], "userA");
		testMessage.sign(signingKeypair.getPrivate());
		assertTrue("Verification failed", testMessage.verify(signingKeypair.getPublic()));
		testMessage = new ErrorProtocolResponse("whatever", "userA");
		testMessage.sign(signingKeypair.getPrivate());
		assertTrue("Verification failed", testMessage.verify(signingKeypair.getPublic()));
	}

	@Test
	public void testThatSecondRequestDoesntRequirePublicKeyToBeProvided() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final CountDownLatch lock = new CountDownLatch(4);

		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);

		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);

		try {
			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID i) {
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					lock.countDown();
					//Ok, this would be non-standard, but it's not technically invalid, so it should work...I think
					ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
					try {
						userAservice.sendMessage(userBNode, testMessage, null);
					} catch (InvalidKeyException | InvalidMessageException | IOException | FailedToStartCommsListenerException e) {
						e.printStackTrace();
						fail("Didn't expect an Exception");
					}
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					lock.countDown();
					ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
					try {
						userBservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			userAservice.sendMessage(userBNode, testMessage, null);
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testProtocolMessageSerialization() {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		QueryChunksProtocolRequest queryChunksTest = new QueryChunksProtocolRequest(testChunks, "userA", "foo");
		HaveChunksProtocolResponse haveChunksTest = new HaveChunksProtocolResponse(testChunks, "userB");
		FetchChunkProtocolRequest fetchChunkTest = new FetchChunkProtocolRequest("foo", "userA", "foo");
		byte[] data = new byte[20];
		new Random().nextBytes(data);
		GiveChunkProtocolResponse giveChunkTest = new GiveChunkProtocolResponse("foo", data, "userA");

		QueryChunksProtocolRequest queryChunksActual = new QueryChunksProtocolRequest();
		ByteBuffer b = queryChunksTest.toBytes();
		b.flip();
		queryChunksActual.fromBytes(b);
		HaveChunksProtocolResponse haveChunksActual = new HaveChunksProtocolResponse();
		b = haveChunksTest.toBytes();
		b.flip();
		haveChunksActual.fromBytes(b);
		FetchChunkProtocolRequest fetchChunkActual = new FetchChunkProtocolRequest();
		b = fetchChunkTest.toBytes();
		b.flip();
		fetchChunkActual.fromBytes(b);
		GiveChunkProtocolResponse giveChunkActual = new GiveChunkProtocolResponse();
		b = giveChunkTest.toBytes();
		b.flip();
		giveChunkActual.fromBytes(b);

		assertTrue(queryChunksActual.isValid());
		assertTrue(haveChunksActual.isValid());
		assertTrue(fetchChunkActual.isValid());
		assertTrue(giveChunkActual.isValid());

		assertEquals(queryChunksTest.getUserID(), queryChunksActual.getUserID());
		assertEquals(haveChunksTest.getUserID(), haveChunksActual.getUserID());
		assertEquals(fetchChunkTest.getUserID(), fetchChunkActual.getUserID());
		assertEquals(giveChunkTest.getUserID(), giveChunkActual.getUserID());

		assertEquals(queryChunksTest.getAuthToken(), queryChunksActual.getAuthToken());
		assertEquals(fetchChunkTest.getAuthToken(), fetchChunkActual.getAuthToken());

		assertTrue(Arrays.equals(queryChunksTest.getChunksRequired(), queryChunksActual.getChunksRequired()));
		assertTrue(Arrays.equals(haveChunksTest.getChunkIDs(), haveChunksActual.getChunkIDs()));
		assertEquals(fetchChunkTest.getChunkID(), fetchChunkActual.getChunkID());
		assertEquals(giveChunkTest.getChunkID(), giveChunkActual.getChunkID());

		assertTrue(Arrays.equals(giveChunkTest.getChunkData(), giveChunkActual.getChunkData()));
	}

	@Test
	public void testRetrieveChunkRequestResponse() throws Exception {
		final String[] testChunkIDs = new String[] { "foo" };
		final Map<String, byte[]> testChunks = new HashMap<>();
		Random r = new Random();
		for (String chunk : testChunkIDs) {
			byte[] data = new byte[524288];
			r.nextBytes(data);
			testChunks.put(chunk, data);
		}

		final CountDownLatch lock = new CountDownLatch(testChunks.size() * 2);

		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userASigningKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userBSigningKeyPair = CryptoUtils.generateECKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);

		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);
		try {
			final ArrayList<String> chunksIShouldReceiveRequestsFor = new ArrayList<>(testChunks.keySet());
			final ArrayList<String> chunksIShouldReceiveResponseFor = new ArrayList<>(testChunks.keySet());

			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID i) {
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof GiveChunkProtocolResponse);
					GiveChunkProtocolResponse message = (GiveChunkProtocolResponse) response;
					assertEquals("request_type wrong", "GIVE_CHUNK", message.getMessageType());
					assertEquals("userID wrong", "userB", message.getUserID());
					assertTrue(
							"I'm not expecting to receive a response for "
									+ message.getChunkID(), chunksIShouldReceiveResponseFor.contains(message.getChunkID()));
					chunksIShouldReceiveResponseFor.remove(message.getChunkID());
					byte[] expectedChunkData = testChunks.get(message.getChunkID());
					assertTrue("chunkdata wrong", Arrays.equals(expectedChunkData, message.getChunkData()));
					try {
						assertTrue("Message signature verification failed", response.verify(userBSigningKeyPair.getPublic()));
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Message signing key invalid");
					} catch (SignatureException e) {
						fail("Didn't expect a sig exception");
					}
					lock.countDown();
				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					assertTrue("Message wrong type", request instanceof FetchChunkProtocolRequest);
					FetchChunkProtocolRequest message = (FetchChunkProtocolRequest) request;
					assertEquals("request_type wrong", "REQUEST_CHUNK", message.getMessageType());
					assertTrue(
							"I'm not expecting to receive a request for "
									+ message.getChunkID(), chunksIShouldReceiveRequestsFor.contains(message.getChunkID()));
					assertEquals("userID wrong", "userA", message.getUserID());
					assertEquals("authToken wrong", userBAuthToken, message.getAuthToken());
					try {
						assertTrue("Message signature verification failed", request.verify(userASigningKeyPair.getPublic()));
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Message signing key invalid");
					} catch (SignatureException e) {
						fail("Didn't expect a sig exception");
					}
					chunksIShouldReceiveRequestsFor.remove(message.getChunkID());
					byte[] chunkData = testChunks.get(message.getChunkID());
					lock.countDown();
					ProtocolMessage resp = new GiveChunkProtocolResponse(message.getChunkID(), chunkData, "userB");
					try {
						resp.sign(userBSigningKeyPair.getPrivate());
						userBservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					} catch (InvalidKeyException e) {
						e.printStackTrace();
						fail("Couldn't sign");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			for (String testChunk : testChunks.keySet()) {
				ProtocolMessage testMessage = new FetchChunkProtocolRequest(testChunk, "userA", userBAuthToken);
				testMessage.sign(userASigningKeyPair.getPrivate());
				userAservice.sendMessage(userBNode, testMessage, null);
			}
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testRetrieveMultipleChunksRequestResponse() throws Exception {
		final String[] testChunkIDs = new String[] { "foo", "bar", "baz" };
		final Map<String, byte[]> testChunks = new HashMap<>();
		Random r = new Random();
		for (String chunk : testChunkIDs) {
			byte[] data = new byte[r.nextInt(524288)];
			r.nextBytes(data);
			testChunks.put(chunk, data);
		}

		final CountDownLatch lock = new CountDownLatch(testChunks.size() * 2);

		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);

		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);
		try {
			final ArrayList<String> chunksIShouldReceiveRequestsFor = new ArrayList<>(testChunks.keySet());
			final ArrayList<String> chunksIShouldReceiveResponseFor = new ArrayList<>(testChunks.keySet());

			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID i) {
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof GiveChunkProtocolResponse);
					GiveChunkProtocolResponse message = (GiveChunkProtocolResponse) response;
					assertEquals("request_type wrong", "GIVE_CHUNK", message.getMessageType());
					assertEquals("userID wrong", "userB", message.getUserID());
					assertTrue(
							"I'm not expecting to receive a response for "
									+ message.getChunkID(), chunksIShouldReceiveResponseFor.contains(message.getChunkID()));
					chunksIShouldReceiveResponseFor.remove(message.getChunkID());
					byte[] expectedChunkData = testChunks.get(message.getChunkID());
					assertTrue("chunkdata wrong", Arrays.equals(expectedChunkData, message.getChunkData()));
					lock.countDown();
				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					assertTrue("Message wrong type", request instanceof FetchChunkProtocolRequest);
					FetchChunkProtocolRequest message = (FetchChunkProtocolRequest) request;
					assertEquals("request_type wrong", "REQUEST_CHUNK", message.getMessageType());
					assertTrue(
							"I'm not expecting to receive a request for "
									+ message.getChunkID(), chunksIShouldReceiveRequestsFor.contains(message.getChunkID()));
					assertEquals("userID wrong", "userA", message.getUserID());
					chunksIShouldReceiveRequestsFor.remove(message.getChunkID());
					byte[] chunkData = testChunks.get(message.getChunkID());
					lock.countDown();
					ProtocolMessage resp = new GiveChunkProtocolResponse(message.getChunkID(), chunkData, "userB");
					try {
						userBservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			for (String testChunk : testChunks.keySet()) {
				ProtocolMessage testMessage = new FetchChunkProtocolRequest(testChunk, "userA", userBAuthToken);
				userAservice.sendMessage(userBNode, testMessage, null);
			}
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	//Here are the negative tests - I'm just gonna run through and do as many as I can think of. This'll be fun

	@Test
	public void testInitCommsNegative() throws Exception {
		try {
			getProtocolCommsService(-100, null);
			Assert.fail("Expected an IllegalArgumentExceptionn");
		} catch (IllegalArgumentException e) {
			//Good, that's what's supposed to happen
		}
		try {
			getProtocolCommsService(Integer.MAX_VALUE, null);
			Assert.fail("Expected an IllegalArgumentExceptionn");
		} catch (IllegalArgumentException e) {
			//Good, that's what's supposed to happen
		}
		try {
			ProtocolCommsService serviceWithNoHandler = getProtocolCommsService(7777, null);
			serviceWithNoHandler.startListener();
			Assert.fail("Expected an IllegalStateException");
		} catch (IllegalStateException e) {
			//Good, that's what's supposed to happen
		}
		ProtocolCommsService service = getProtocolCommsService(7777, null);
		try {
			service.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {

				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				}
			});
			service.startListener();
			ProtocolCommsService serviceOnSamePort = getProtocolCommsService(7777, null);
			serviceOnSamePort.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {

				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				}
			});
			serviceOnSamePort.startListener();
			Assert.fail("Expected a FailedToStartCommsListenerException");
		} catch (FailedToStartCommsListenerException e) {
			//Good, that's what's supposed to happen
			assertTrue("Wrong exception type", e.getCause() instanceof BindException);
		} finally {
			service.stop();
		}
	}

	@Test
	public void testSignAndVerifyNegative() throws NoSuchAlgorithmException, CryptographicException {
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(new String[] { "foo", "bar", "baz" }, "userA", "foo");
		final KeyPair signingKeypair = CryptoUtils.generateECKeyPair();
		final KeyPair someOtherKeypair = CryptoUtils.generateECKeyPair();
		try {
			testMessage.sign(null);
			Assert.fail("Expected InvalidKeyException");
		} catch (InvalidKeyException e) {
			//Good
		}
		try {
			testMessage.sign(signingKeypair.getPrivate());
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			Assert.fail("Unexpected Exception");
		}
		try {
			testMessage.verify(null);
			Assert.fail("Expected InvalidKeyException");
		} catch (InvalidKeyException e) {
			//Good
		} catch (SignatureException e) {
			fail("Didn't expect a sig exception");
		}
		try {
			assertFalse("Signature should be bad, but isn't", testMessage.verify(someOtherKeypair.getPublic()));
		} catch (InvalidKeyException e) {
			Assert.fail("Unexpected Exception");
		} catch (SignatureException e) {
			fail("Didn't expect a sig exception");
		}
		QueryChunksProtocolRequest qcpr = (QueryChunksProtocolRequest) testMessage;
		qcpr._signature = new byte[50];
		try {
			qcpr.verify(someOtherKeypair.getPublic());
			fail("Expected verification to fail because Signature is corrupt");
		} catch (InvalidKeyException e) {
			fail("Unexpected Exception");
		} catch (SignatureException e) {
			//Good
		}
	}

	@Test
	public void testMessageFactoryNegative() {
		ProtocolMessageFactory fac = new ProtocolMessageFactory();
		byte b = 0x11;
		try {
			fac.getMessage(b, ByteBuffer.allocate(0));
			fail();
		} catch (UnknownMessageTypeException e) {
			//Good
		}
		ProtocolMessage fauxMessage = new ProtocolMessage() {
			@Override public void sign(PrivateKey signingKey) throws InvalidKeyException {

			}

			@Override public boolean verify(PublicKey senderPublicKey) throws InvalidKeyException, SignatureException {
				return false;
			}

			@Override public ByteBuffer toBytes() {
				return null;
			}

			@Override public void fromBytes(ByteBuffer bytes) {

			}

			@Override public boolean isValid() {
				return false;
			}

			@Override public boolean isRequest() {
				return false;
			}

			@Override public String getType() {
				return "POTATO";
			}
		};
		try {
			fac.getMessageByteIdentifier(fauxMessage);
			fail();
		} catch (UnknownMessageTypeException e) {
			//Good
		}
	}

	@Test
	public void testQueryChunksToRecipientWithNoPublicKey() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final CountDownLatch lock = new CountDownLatch(2);

		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);

		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, null);
		try {
			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID i) {
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof ErrorProtocolResponse);
					ErrorProtocolResponse message = (ErrorProtocolResponse) response;
					assertTrue("Error Message Wrong", message.getErrorMessage().startsWith("503"));
					assertEquals("userID wrong", "userB", message.getUserID());
					lock.countDown();
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					fail("Wasn't expecting requestReceived to fire");
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					lock.countDown();
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			userAservice.sendMessage(userBNode, testMessage, null);
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testSendMessageToWrongAddress() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
		ProtocolCommsService service = getProtocolCommsService(7777, null);
		try {
			service.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {

				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				}
			});
			service.startListener();
			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			try {
				service.sendMessage(userBNode, testMessage, null);
				fail("Expected a ConnectException");
			} catch (ConnectException ex) {
				//Good
			}
		} finally {
			service.stop();
		}
	}

	@Test
	public void testSendMessageorReplyBeforeListenerStartsDoesntWork() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		String userBAuthToken = "The password is password";
		MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
		ProtocolCommsService service = getProtocolCommsService(7777, null);
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
		try {
			service.sendMessage(userBNode, testMessage, null);
			fail("Expected a FailedToStartCommsListenerException");
		} catch (FailedToStartCommsListenerException ex) {
			//Good
		}
		ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userA");
		try {
			service.reply(resp, new MessageID(0, new InetSocketAddress(1234)));
			fail("Expected a FailedToStartCommsListenerException");
		} catch (FailedToStartCommsListenerException ex) {
			//Good
		}
	}

	@Test
	public void testSendWrongKindOfMessagesDoesntWork() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		String userBAuthToken = "The password is password";
		MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);
		ProtocolCommsService service = getProtocolCommsService(7777, null);
		try {
			service.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {

				}

				@Override public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				}
			});
			service.startListener();
			ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userA");
			try {
				service.sendMessage(userBNode, resp, null);
				fail("Expected a InvalidMessageException");
			} catch (InvalidMessageException ex) {
				//Good
			}
			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			try {
				service.reply(testMessage, new MessageID(0, new InetSocketAddress(1234)));
				fail("Expected a InvalidMessageException");
			} catch (InvalidMessageException ex) {
				//Good
			}
			testMessage = new QueryChunksProtocolRequest(null, "userA", userBAuthToken);
			try {
				service.sendMessage(userBNode, testMessage, null);
				fail("Expected a InvalidMessageException");
			} catch (InvalidMessageException ex) {
				//Good
			}
			resp = new HaveChunksProtocolResponse(null, "userA");
			try {
				service.reply(resp, new MessageID(0, new InetSocketAddress(1234)));
				fail("Expected a InvalidMessageException");
			} catch (InvalidMessageException ex) {
				//Good
			}
		} finally {
			service.stop();
		}
	}

	@Test
	public void testSendQueryChunksRequestWithNoPublicKey() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, null, userBAuthToken);
		ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		ProtocolCommsService userBservice = getProtocolCommsService(7778, null);
		try {
			userAservice.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {

				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				}
			});
			userAservice.startListener();
			userBservice.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {

				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				}
			});
			userBservice.startListener();
			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			try {
				userAservice.sendMessage(userBNode, testMessage, null);
				fail("Expected a InvalidKeyException");
			} catch (InvalidKeyException ex) {
				//Good
			}
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testRequestTimeout() throws Exception {
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userASigningKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userBSigningKeyPair = CryptoUtils.generateECKeyPair();
		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);
		try {
			final String[] testChunks = new String[] { "foo", "bar", "baz" };
			final CountDownLatch lock = new CountDownLatch(1);

			String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
			MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);

			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID i) {
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					fail("Wasn't expecting a response - kind of invalidates the test");
				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					assertTrue(message.equalsIgnoreCase("Request Timed out"));
					lock.countDown();
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					//Hey, I received a request - let's do nothing!
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();
			userAservice.setTimeout(5, TimeUnit.SECONDS);
			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			MessageID messageID = userAservice.sendMessage(userBNode, testMessage, null);
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testBothUsersRequestAtTheSameTime() throws Exception {
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final KeyPair userAChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final ProtocolCommsService userAservice = getProtocolCommsService(7777, userAChunkTransferKeyPair);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);
		try {
			final String[] testChunks = new String[] { "foo", "bar", "baz" };
			final CountDownLatch lock = new CountDownLatch(4);

			String userAAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
			String userBAuthToken = "<('.'<) (>'.')>";
			MemberNode userANode = new MemberNode("userA", "127.0.0.1", 7777, userAChunkTransferKeyPair.getPublic().getEncoded(), userAAuthToken);
			MemberNode userBNode = new MemberNode("userB", "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken);

			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					assertTrue("Message wrong type", request instanceof QueryChunksProtocolRequest);
					QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
					assertEquals("request_type wrong", "QUERY_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
					assertEquals("userID wrong", "userB", message.getUserID());
					lock.countDown();
					ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userA");
					try {
						userAservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof HaveChunksProtocolResponse);
					HaveChunksProtocolResponse message = (HaveChunksProtocolResponse) response;
					assertEquals("request_type wrong", "HAVE_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunkIDs(), testChunks));
					assertEquals("userID wrong", "userB", message.getUserID());
					lock.countDown();
				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					assertTrue("Message wrong type", request instanceof QueryChunksProtocolRequest);
					QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
					assertEquals("request_type wrong", "QUERY_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
					assertEquals("userID wrong", "userA", message.getUserID());
					lock.countDown();
					ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
					try {
						userBservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof HaveChunksProtocolResponse);
					HaveChunksProtocolResponse message = (HaveChunksProtocolResponse) response;
					assertEquals("request_type wrong", "HAVE_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunkIDs(), testChunks));
					assertEquals("userID wrong", "userA", message.getUserID());
					lock.countDown();
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken);
			ProtocolMessage testMessage2 = new QueryChunksProtocolRequest(testChunks, "userB", userAAuthToken);
			MessageID messageID1 = userAservice.sendMessage(userBNode, testMessage, convertHandler(handlerA));
			MessageID messageID2 = userBservice.sendMessage(userANode, testMessage2, convertHandler(handlerB));
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
			boolean connectionOpen = true;
			for (int i = 0; i < 20 && connectionOpen; i++) {
				Thread.sleep(100 * i);
				SocketChannel sc = getConnectionForMessageID(userAservice, messageID1);
				SocketChannel sc2 = getConnectionForMessageID(userBservice, messageID2);
				connectionOpen = sc != null && sc.isConnected() && sc2 != null && sc2.isConnected();
			}
			assertTrue("Connection never closed", !connectionOpen);
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testSendPartialMessage() throws Exception {
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);
		try {
			final String[] testChunks = new String[] { "foo", "bar", "baz" };
			final CountDownLatch lock = new CountDownLatch(2);

			String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";

			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID i) {
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof HaveChunksProtocolResponse);
					HaveChunksProtocolResponse message = (HaveChunksProtocolResponse) response;
					assertEquals("request_type wrong", "HAVE_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunkIDs(), testChunks));
					assertEquals("userID wrong", "userB", message.getUserID());
					lock.countDown();
				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					assertTrue("Message wrong type", request instanceof QueryChunksProtocolRequest);
					QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
					assertEquals("request_type wrong", "QUERY_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
					assertEquals("userID wrong", "userA", message.getUserID());
					lock.countDown();
					ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
					try {
						userBservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			ProtocolMessage[] testMessages = new ProtocolMessage[] { new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken) };
			MessageID messageID = sendMessagenBytesAtATime(userAservice, 100, "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), testMessages)[0];
			assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
			boolean connectionOpen = true;
			for (int i = 0; i < 20 && connectionOpen; i++) {
				Thread.sleep(100 * i);
				SocketChannel sc = getConnectionForMessageID(userAservice, messageID);
				connectionOpen = sc != null && sc.isConnected();
			}
			assertTrue("Connection never closed", !connectionOpen);
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	@Test
	public void testSendPartialMessagesBackToBackLikeSomeSortOfNightmareScenario() throws Exception {
		final KeyPair userBChunkTransferKeyPair = CryptoUtils.generateECKeyPair();
		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);
		try {
			final String[] testChunks = new String[] { "foo", "bar", "baz" };
			final CountDownLatch lock = new CountDownLatch(4);

			String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";

			ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID i) {
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					assertTrue("Message wrong type", response instanceof HaveChunksProtocolResponse);
					HaveChunksProtocolResponse message = (HaveChunksProtocolResponse) response;
					assertEquals("request_type wrong", "HAVE_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunkIDs(), testChunks));
					assertEquals("userID wrong", "userB", message.getUserID());
					lock.countDown();
				}

				@Override
				public void error(Throwable t) {
				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userA");
					try {
						userAservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userAservice.setHandler(handlerA);
			ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
					assertTrue("Message wrong type", request instanceof QueryChunksProtocolRequest);
					QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
					assertEquals("request_type wrong", "QUERY_CHUNKS", message.getMessageType());
					assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
					assertEquals("userID wrong", "userA", message.getUserID());
					lock.countDown();
					ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
					try {
						userBservice.reply(resp, messageID);
					} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
						e.printStackTrace();
						fail("Couldn't reply");
					}
				}

				@Override public void responseReceived(ProtocolMessage response, MessageID mid) {
					//nop
				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
					ProtocolMessage error = new ErrorProtocolResponse(message, "userB");
					try {
						userBservice.reply(error, messageId);
					} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
						e.printStackTrace();
					}
				}
			};
			userBservice.setHandler(handlerB);
			userAservice.startListener();
			userBservice.startListener();

			ProtocolMessage[] testMessages = new ProtocolMessage[] { new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken),
					new QueryChunksProtocolRequest(testChunks, "userA", userBAuthToken) };
			MessageID[] messageIDs = sendMessagenBytesAtATime(userAservice, 100, "127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), testMessages);
			assertTrue("Message never received", lock.await(20, TimeUnit.SECONDS));
			boolean connectionOpen = true;
			for (int i = 0; i < 20 && connectionOpen; i++) {
				Thread.sleep(100 * i);
				SocketChannel sc = getConnectionForMessageID(userAservice, messageIDs[0]);
				SocketChannel sc2 = getConnectionForMessageID(userAservice, messageIDs[1]);
				connectionOpen = sc != null && sc.isConnected() && sc2 != null && sc2.isConnected();
			}
			assertTrue("Connection never closed", !connectionOpen);
		} finally {
			userAservice.stop();
			userBservice.stop();
		}
	}

	protected void putAsMuchAsPossible(ByteBuffer srcBuffer, ByteBuffer readBuffer) {
		//This is basically the way ByteBuffer.put(ByteBuffer) works, but without the ability to throw BufferOverflowExceptions
		//I don't need a BufferOverflow - if there's more data to be read, the readBuffer will be compacted and the loop will go on
		int n = Math.min(srcBuffer.remaining(), readBuffer.remaining());
		for (int i = 0; i < n; i++) {
			srcBuffer.put(readBuffer.get());
		}
	}

	private ProtocolCommsResponseHandler convertHandler(ProtocolCommsHandler h) {
		return new ProtocolCommsResponseHandler() {
			@Override public void responseReceived(ProtocolMessage response, MessageID messageID) {
				h.responseReceived(response, messageID);
			}

			@Override public void error(Throwable t, MessageID messageID) {
				h.error(t);
			}

			@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				h.error(message, shouldReply, messageId);
			}
		};
	}

	/**
	 * The easiest way to implement this is to
	 * 1) Have it throw NotImplementedException
	 * 2) Get all of your other tests to pass - at the very least, the mainline tests
	 * 3) Copy your code out of sendMessage, modify variable names as appropriate
	 * 4) Modify the code as slightly as possible to make it send the bytes slowly
	 */
	protected abstract MessageID[] sendMessagenBytesAtATime(ProtocolCommsService commsService, int bytesAtATime, String location, int port, byte[] transferPublicKey, ProtocolMessage[] messages) throws FailedToStartCommsListenerException, InvalidMessageException, InvalidKeyException, IOException, UnknownMessageTypeException;
}
