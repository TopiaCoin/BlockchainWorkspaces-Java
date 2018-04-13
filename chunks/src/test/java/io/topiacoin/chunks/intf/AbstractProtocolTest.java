package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.exceptions.InvalidMessageIDException;
import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ErrorProtocolResponse;
import io.topiacoin.chunks.model.protocol.GiveChunkProtocolResponse;
import io.topiacoin.chunks.model.protocol.HaveChunksProtocolResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolRequest;
import io.topiacoin.chunks.model.protocol.FetchChunkProtocolRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
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

	@Test
	public void testQueryChunksRequestAndResponse() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final CountDownLatch lock = new CountDownLatch(2);

		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";

		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);

		ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, MessageID i) {
			}

			@Override public void responseReceived(ProtocolMessage response) {
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

			@Override public void responseReceived(ProtocolMessage response) {
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

		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		userAservice.sendMessage("127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
		assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
		fail("Check if connection is closed not implemented");
	}

	@Test
	public void testSignAndVerify() throws Exception {
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(new String[] { "foo", "bar", "baz" }, "userA");
		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair signingKeypair = userKeyGen.genKeyPair();
		testMessage.sign(signingKeypair.getPrivate());
		assertTrue("Verification failed", testMessage.verify(signingKeypair.getPublic()));
	}

	@Test
	public void testThatSecondRequestDoesntRequirePublicKeyToBeProvided() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final CountDownLatch lock = new CountDownLatch(4);

		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		final String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";

		final ProtocolCommsService userAservice = getProtocolCommsService(5555, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(5556, userBChunkTransferKeyPair);

		ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, MessageID i) {
			}

			@Override public void responseReceived(ProtocolMessage response) {
				HaveChunksProtocolResponse message = (HaveChunksProtocolResponse) response;
				lock.countDown();
				//Ok, this would be non-standard, but it's not technically invalid, so it should work...I think
				ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
				try {
					userAservice.sendMessage("127.0.0.1", 5556, null, userBAuthToken, testMessage);
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
				QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
				lock.countDown();
				ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
				try {
					userBservice.reply(resp, messageID);
				} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
					e.printStackTrace();
					fail("Couldn't reply");
				}
			}

			@Override public void responseReceived(ProtocolMessage response) {
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

		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		userAservice.sendMessage("127.0.0.1", 5556, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
		assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testProtocolMessageSerialization() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		QueryChunksProtocolRequest queryChunksTest = new QueryChunksProtocolRequest(testChunks, "userA");
		HaveChunksProtocolResponse haveChunksTest = new HaveChunksProtocolResponse(testChunks, "userB");
		FetchChunkProtocolRequest fetchChunkTest = new FetchChunkProtocolRequest("foo", "userA");
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

		assertTrue(Arrays.equals(queryChunksTest.getChunksRequired(), queryChunksActual.getChunksRequired()));
		assertTrue(Arrays.equals(haveChunksTest.getChunkIDs(), haveChunksActual.getChunkIDs()));
		assertEquals(fetchChunkTest.getChunkID(), fetchChunkActual.getChunkID());
		assertEquals(giveChunkTest.getChunkID(), giveChunkActual.getChunkID());

		assertTrue(Arrays.equals(giveChunkTest.getChunkData(), giveChunkActual.getChunkData()));
	}

	@Test
	public void testRetrieveChunkRequestResponse() throws Exception {
		final String[] testChunkIDs = new String[] { "foo", "bar", "baz" };
		final Map<String, byte[]> testChunks = new HashMap<String, byte[]>();
		Random r = new Random();
		for(String chunk : testChunkIDs) {
			byte[] data = new byte[r.nextInt(524288)];
			r.nextBytes(data);
			testChunks.put(chunk, data);
		}

		final CountDownLatch lock = new CountDownLatch(testChunks.size() * 2);

		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";

		final ProtocolCommsService userAservice = getProtocolCommsService(7777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(7778, userBChunkTransferKeyPair);

		final ArrayList<String> chunksIShouldReceiveRequestsFor = new ArrayList<String>();
		final ArrayList<String> chunksIShouldReceiveResponseFor = new ArrayList<String>();
		chunksIShouldReceiveRequestsFor.addAll(testChunks.keySet());
		chunksIShouldReceiveResponseFor.addAll(testChunks.keySet());

		ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, MessageID i) {
			}

			@Override public void responseReceived(ProtocolMessage response) {
				assertTrue("Message wrong type", response instanceof GiveChunkProtocolResponse);
				GiveChunkProtocolResponse message = (GiveChunkProtocolResponse) response;
				assertEquals("request_type wrong", "GIVE_CHUNK", message.getMessageType());
				assertEquals("userID wrong", "userB", message.getUserID());
				assertTrue("I'm not expecting to receive a response for " + message.getChunkID(), chunksIShouldReceiveResponseFor.contains(message.getChunkID()));
				chunksIShouldReceiveResponseFor.remove(message.getChunkID());
				byte[] expectedChunkData = testChunks.get(message.getChunkID());
				assertEquals("chunkdata wrong", expectedChunkData, message.getChunkData());
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
				assertTrue("I'm not expecting to receive a request for " + message.getChunkID(), chunksIShouldReceiveRequestsFor.contains(message.getChunkID()));
				assertEquals("userID wrong", "userA", message.getUserID());
				chunksIShouldReceiveRequestsFor.remove(message.getChunkID());
				byte[] chunkData = testChunks.get(message.getChunkID());
				lock.countDown();
				ProtocolMessage resp = new GiveChunkProtocolResponse(message.getUserID(), chunkData, "userB");
				try {
					userBservice.reply(resp, messageID);
				} catch (FailedToStartCommsListenerException | InvalidMessageException | InvalidMessageIDException e) {
					e.printStackTrace();
					fail("Couldn't reply");
				}
			}

			@Override public void responseReceived(ProtocolMessage response) {
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

		for(String testChunk : testChunks.keySet()) {
			ProtocolMessage testMessage = new FetchChunkProtocolRequest(testChunk, "userA");
			userAservice.sendMessage("127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
		}
		assertTrue("Message never received", lock.await(1000, TimeUnit.SECONDS));
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
			ProtocolCommsService serviceWithNoHandler = getProtocolCommsService(8888, null);
			serviceWithNoHandler.startListener();
			Assert.fail("Expected an IllegalStateException");
		} catch (IllegalStateException e) {
			//Good, that's what's supposed to happen
		}
		try {
			ProtocolCommsService service = getProtocolCommsService(8888, null);
			service.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response) {

				}

				@Override
				public void error(Throwable t) {

				}

				@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				}
			});
			service.startListener();
			ProtocolCommsService serviceOnSamePort = getProtocolCommsService(8888, null);
			serviceOnSamePort.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response) {

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
		}
	}

	@Test
	public void testSignAndVerifyNegative() throws NoSuchAlgorithmException {
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(new String[] { "foo", "bar", "baz" }, "userA");
		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair signingKeypair = userKeyGen.genKeyPair();
		final KeyPair someOtherKeypair = userKeyGen.genKeyPair();
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
		}
		try {
			assertFalse("Signature should be bad, but isn't", testMessage.verify(someOtherKeypair.getPublic()));
		} catch (InvalidKeyException e) {
			Assert.fail("Unexpected Exception");
		}
	}

	@Test
	public void testQueryChunksToRecipientWithNoPublicKey() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final CountDownLatch lock = new CountDownLatch(2);

		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";

		final ProtocolCommsService userAservice = getProtocolCommsService(11777, null);
		final ProtocolCommsService userBservice = getProtocolCommsService(11778, null);

		ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, MessageID i) {
			}

			@Override public void responseReceived(ProtocolMessage response) {
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

			@Override public void responseReceived(ProtocolMessage response) {
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

		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		userAservice.sendMessage("127.0.0.1", 11778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
		assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testSendMessageToWrongAddress() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		ProtocolCommsService service = getProtocolCommsService(9999, null);
		service.setHandler(new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

			}

			@Override public void responseReceived(ProtocolMessage response) {

			}

			@Override
			public void error(Throwable t) {
			}

			@Override public void error(String message, boolean shouldReply, MessageID messageId) {
			}
		});
		service.startListener();
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		try {
			service.sendMessage("127.0.0.1", 9998, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
			fail("Expected a ConnectException");
		} catch (ConnectException ex) {
			//Good
		}
	}

	@Test
	public void testSendMessageorReplyBeforeListenerStartsDoesntWork() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "The password is password";
		ProtocolCommsService service = getProtocolCommsService(4444, null);
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		try {
			service.sendMessage("127.0.0.1", 9998, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
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
		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "The password is password";
		ProtocolCommsService service = getProtocolCommsService(4444, null);
		service.setHandler(new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

			}

			@Override public void responseReceived(ProtocolMessage response) {

			}

			@Override public void error(Throwable t) {

			}

			@Override public void error(String message, boolean shouldReply, MessageID messageId) {
			}
		});
		service.startListener();
		ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userA");
		try {
			service.sendMessage("127.0.0.1", 9998, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, resp);
			fail("Expected a InvalidMessageException");
		} catch (InvalidMessageException ex) {
			//Good
		}
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		try {
			service.reply(testMessage, new MessageID(0, new InetSocketAddress(1234)));
			fail("Expected a InvalidMessageException");
		} catch (InvalidMessageException ex) {
			//Good
		}
	}

	@Test
	public void testSendQueryChunksRequestWithNoPublicKey() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		ProtocolCommsService service = getProtocolCommsService(10000, null);
		service.setHandler(new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

			}

			@Override public void responseReceived(ProtocolMessage response) {

			}

			@Override
			public void error(Throwable t) {
			}

			@Override public void error(String message, boolean shouldReply, MessageID messageId) {
			}
		});
		service.startListener();
		ProtocolCommsService userBService = getProtocolCommsService(10001, null);
		userBService.setHandler(new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, MessageID mesesageID) {

			}

			@Override public void responseReceived(ProtocolMessage response) {

			}

			@Override
			public void error(Throwable t) {

			}

			@Override public void error(String message, boolean shouldReply, MessageID messageId) {
			}
		});
		userBService.startListener();
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		try {
			service.sendMessage("127.0.0.1", 10001, null, userBAuthToken, testMessage);
			fail("Expected a InvalidKeyException");
		} catch (InvalidKeyException ex) {
			//Good
		}
		try {
			service.sendMessage("127.0.0.1", 10001, null, userBAuthToken, testMessage);
			fail("Expected a InvalidKeyException");
		} catch (InvalidKeyException ex) {
			//Good
		}
	}
}
