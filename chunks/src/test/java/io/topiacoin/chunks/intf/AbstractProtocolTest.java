package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.model.protocol.ProtocolJsonMessage;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolJsonRequest;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolJsonResponse;
import org.junit.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractProtocolTest {

	protected abstract ProtocolCommsService getProtocolCommsService(int port, PublicKey pubKey, PrivateKey privKey) throws IOException;

	@Test
	public void testQueryChunksRequestAndResponse() throws Exception {
		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		final CountDownLatch lock = new CountDownLatch(2);

		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userAKeyPair = userKeyGen.genKeyPair();
		final KeyPair userBKeyPair = userKeyGen.genKeyPair();

		final ProtocolCommsService listenerA = getProtocolCommsService(7777, userBKeyPair.getPublic(), userAKeyPair.getPrivate());
		ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request) {
			}

			@Override public void responseReceived(ProtocolMessage response) {
				assertTrue("Message wrong type", response instanceof QueryChunksProtocolJsonResponse);
				QueryChunksProtocolJsonResponse message = (QueryChunksProtocolJsonResponse) response;
				assertEquals("request_type wrong", "HAVE_CHUNKS", message.getResponseType());
				assertTrue("chunks_required wrong", Arrays.equals(message.getChunkIDs(), testChunks));
				assertEquals("userID wrong", "userB", message.getUserID());
				assertEquals("nonce wrong", "nonceB", message.getNonce());
				lock.countDown();
			}
		};
		listenerA.setHandler(handlerA);
		final ProtocolCommsService listenerB = getProtocolCommsService(7778, userAKeyPair.getPublic(), userBKeyPair.getPrivate());
		ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request) {
				assertTrue("Message wrong type", request instanceof QueryChunksProtocolJsonRequest);
				QueryChunksProtocolJsonRequest message = (QueryChunksProtocolJsonRequest) request;
				assertEquals("request_type wrong", "QUERY_CHUNKS", message.getRequestType());
				assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
				assertEquals("userID wrong", "userA", message.getUserID());
				assertEquals("nonce wrong", "nonceA", message.getNonce());
				lock.countDown();
				ProtocolMessage resp = new QueryChunksProtocolJsonResponse(testChunks, "userB", "nonceB", userAKeyPair.getPublic());
				try {
					listenerB.reply(resp, request.getMessageID());
				} catch (SignatureException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				}
			}

			@Override public void responseReceived(ProtocolMessage response) {
				//nop
			}
		};
		listenerB.setHandler(handlerB);
		listenerA.start();
		listenerB.start();

		KeyPairGenerator commsKeyGen = KeyPairGenerator.getInstance("EC");
		KeyPair fetchPairUserA = commsKeyGen.generateKeyPair();

		ProtocolJsonMessage testMessage = new QueryChunksProtocolJsonRequest(testChunks, "userA", "nonceA", fetchPairUserA.getPublic());
		listenerA.sendMessage("127.0.0.1", 7778, testMessage);
		assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
	}
}
