package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.model.protocol.HaveChunksProtocolResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
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
			@Override public void requestReceived(ProtocolMessage request, int i) {
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
		};
		userAservice.setHandler(handlerA);
		ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, int messageID) {
				assertTrue("Message wrong type", request instanceof QueryChunksProtocolRequest);
				QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
				assertEquals("request_type wrong", "QUERY_CHUNKS", message.getMessageType());
				assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
				assertEquals("userID wrong", "userA", message.getUserID());
				lock.countDown();
				ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
				try {
					userBservice.reply(resp, messageID);
				} catch (SignatureException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				}
			}

			@Override public void responseReceived(ProtocolMessage response) {
				//nop
			}
			@Override
			public void error(Throwable t) {

			}
		};
		userBservice.setHandler(handlerB);
		userAservice.start();
		userBservice.start();

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
			@Override public void requestReceived(ProtocolMessage request, int i) {
			}

			@Override public void responseReceived(ProtocolMessage response) {
				HaveChunksProtocolResponse message = (HaveChunksProtocolResponse) response;
				lock.countDown();
				//Ok, this would be non-standard, but it's not technically invalid, so it should work...I think
				ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
				try {
					userAservice.sendMessage("127.0.0.1", 5556, null, userBAuthToken, testMessage);
				} catch (InvalidKeyException | InvalidMessageException | IOException e) {
					e.printStackTrace();
					fail("Didn't expect an Exception");
				}
			}
			@Override
			public void error(Throwable t) {

			}
		};
		userAservice.setHandler(handlerA);
		ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, int messageID) {
				QueryChunksProtocolRequest message = (QueryChunksProtocolRequest) request;
				lock.countDown();
				ProtocolMessage resp = new HaveChunksProtocolResponse(testChunks, "userB");
				try {
					userBservice.reply(resp, messageID);
				} catch (SignatureException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				}
			}

			@Override public void responseReceived(ProtocolMessage response) {
				//nop
			}
			@Override
			public void error(Throwable t) {

			}
		};
		userBservice.setHandler(handlerB);
		userAservice.start();
		userBservice.start();

		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		userAservice.sendMessage("127.0.0.1", 5556, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
		assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testRetrieveChunkRequestResponse() {
		fail("Not implemented");
	}

	//Here are the negative tests - I'm just gonna run through and do as many as I can think of. This'll be fun

	@Test
	public void testInitCommsNegative() throws Exception {
		try {
			getProtocolCommsService(-100, null);
			Assert.fail("Expected an IllegalArgumentExceptionn");
		} catch(IllegalArgumentException e) {
			//Good, that's what's supposed to happen
		}
		try {
			getProtocolCommsService(Integer.MAX_VALUE, null);
			Assert.fail("Expected an IllegalArgumentExceptionn");
		} catch(IllegalArgumentException e) {
			//Good, that's what's supposed to happen
		}
		try {
			ProtocolCommsService serviceWithNoHandler = getProtocolCommsService(8888, null);
			serviceWithNoHandler.start();
			Assert.fail("Expected an IllegalStateException");
		} catch(IllegalStateException e) {
			//Good, that's what's supposed to happen
		}
		try {
			ProtocolCommsService service = getProtocolCommsService(8888, null);
			service.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, int mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response) {

				}
				@Override
				public void error(Throwable t) {

				}
			});
			service.start();
			ProtocolCommsService serviceOnSamePort = getProtocolCommsService(8888, null);
			serviceOnSamePort.setHandler(new ProtocolCommsHandler() {
				@Override public void requestReceived(ProtocolMessage request, int mesesageID) {

				}

				@Override public void responseReceived(ProtocolMessage response) {

				}
				@Override
				public void error(Throwable t) {

				}
			});
			serviceOnSamePort.start();
			Assert.fail("Expected a FailedToStartCommsListenerException");
		} catch(FailedToStartCommsListenerException e) {
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
	public void testSendMessageToWrongAddress() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		ProtocolCommsService service = getProtocolCommsService(9999, null);
		service.setHandler(new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, int mesesageID) {

			}

			@Override public void responseReceived(ProtocolMessage response) {

			}

			@Override
			public void error(Throwable t) {
			}
		});
		service.start();
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		try {
			service.sendMessage("127.0.0.1", 9998, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
			fail("Expected a ConnectException");
		} catch(ConnectException ex) {
			//Good
		}
	}

	@Test
	public void testSendQueryChunksRequestWithNoPublicKeyOrBadAuthToken() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userBChunkTransferKeyPair = userKeyGen.genKeyPair();
		String userBAuthToken = "If this test doesn't pass within 15 minutes, I'm legally allowed to leave";
		ProtocolCommsService service = getProtocolCommsService(10000, null);
		service.setHandler(new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, int mesesageID) {

			}

			@Override public void responseReceived(ProtocolMessage response) {

			}
			@Override
			public void error(Throwable t) {
			}
		});
		service.start();
		ProtocolCommsService userBService = getProtocolCommsService(10001, null);
		userBService.setHandler(new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request, int mesesageID) {

			}

			@Override public void responseReceived(ProtocolMessage response) {

			}
			@Override
			public void error(Throwable t) {

			}
		});
		userBService.start();
		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		try {
			service.sendMessage("127.0.0.1", 10001, null, userBAuthToken, testMessage);
			fail("Expected a InvalidKeyException");
		} catch(InvalidKeyException ex) {
			//Good
		}
		try {
			service.sendMessage("127.0.0.1", 10001, null, userBAuthToken, testMessage);
			fail("Expected a InvalidKeyException");
		} catch(InvalidKeyException ex) {
			//Good
		}
		try {
			service.sendMessage("127.0.0.1", 10001, userBChunkTransferKeyPair.getPublic().getEncoded(), "potato", testMessage);
			fail("Expected an Exception...I think? Do we want the protocol to be responsible for validating the Auth key, or do we want that a level up?");
		} catch(InvalidKeyException ex) {
			//Good
		}
	}
}
