package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.impl.transferRunnables.tcp.TCPProtocolCommsService;
import io.topiacoin.chunks.model.protocol.HaveChunksProtocolResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolRequest;
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
		};
		userBservice.setHandler(handlerB);
		userAservice.start();
		userBservice.start();

		ProtocolMessage testMessage = new QueryChunksProtocolRequest(testChunks, "userA");
		userAservice.sendMessage("127.0.0.1", 7778, userBChunkTransferKeyPair.getPublic().getEncoded(), userBAuthToken, testMessage);
		assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
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

	/*@Test
	public void testStuff() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		//destClientChunkStoragePubKey should be picked up off the Blockchain and decrypted with the Workspace Key/////
		keyGen.initialize(571);
		KeyPair destClientChunkStorageKeypair = keyGen.generateKeyPair();
		String destClientChunkStoragePubKeyStr = new BASE64Encoder().encode(destClientChunkStorageKeypair.getPublic().getEncoded());
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////
		KeyFactory kf = KeyFactory.getInstance("EC");
		X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(new BASE64Decoder().decodeBuffer(destClientChunkStoragePubKeyStr));
		PublicKey destClientChunkStoragePubKey = kf.generatePublic(pkSpec);
		KeyPair srcUserKeyPair = keyGen.generateKeyPair();
		KeyPair destUserKeyPair = keyGen.generateKeyPair();

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



		final String[] testChunks = new String[] { "foo", "bar", "baz" };
		ProtocolJsonMessage testMessage = new QueryChunksProtocolJsonRequest(testChunks, "userA", "nonceA");
	}*/
}
