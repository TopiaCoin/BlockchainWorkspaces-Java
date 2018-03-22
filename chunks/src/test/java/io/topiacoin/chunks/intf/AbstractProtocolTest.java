package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.impl.transferRunnables.tcp.TCPProtocolCommsService;
import io.topiacoin.chunks.model.protocol.ProtocolJsonMessage;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolJsonRequest;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolJsonResponse;
import io.topiacoin.util.Notification;
import io.topiacoin.util.NotificationCenter;
import io.topiacoin.util.NotificationHandler;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractProtocolTest {

	protected abstract ProtocolCommsService getProtocolCommsService(int port, PublicKey pubKey, PrivateKey privKey) throws IOException;

	@Test
	public void testQueryChunksRequest() throws Exception {
		final String[] testChunks = new String[]{"foo", "bar", "baz"};
		NotificationCenter notificationCenter = NotificationCenter.defaultCenter();
		final CountDownLatch lock = new CountDownLatch(1);
		NotificationHandler userAHandler = new NotificationHandler() {
			@Override public void handleNotification(Notification notification) {
				//lock.countDown();
			}
		};
		NotificationHandler userBHandler = new NotificationHandler() {
			@Override public void handleNotification(Notification notification) {
				assertTrue("Got null notification", notification != null);
				Map<String, Object> notificationInfo = notification.getNotificationInfo();
				assertTrue("NotificationInfo null", notificationInfo != null);
				Object messageObj = notificationInfo.get("message");
				assertTrue("Message null", messageObj != null);
				assertTrue("Message wrong type", messageObj instanceof QueryChunksProtocolJsonRequest);
				QueryChunksProtocolJsonRequest message = (QueryChunksProtocolJsonRequest) messageObj;
				assertEquals("request_type wrong", "QUERY_CHUNKS", message.getRequestType());
				assertTrue("chunks_required wrong", Arrays.equals(message.getChunksRequired(), testChunks));
				assertEquals("userID wrong", "userA", message.getUserID());
				assertEquals("nonce wrong", "nonceA", message.getNonce());
				lock.countDown();
			}
		};
		notificationCenter.addHandler(userAHandler, "ProtocolMessageReceived", "userB");
		notificationCenter.addHandler(userBHandler, "ProtocolMessageReceived", "userA");

		KeyPairGenerator userKeyGen = KeyPairGenerator.getInstance("EC");
		userKeyGen.initialize(571);
		final KeyPair userAKeyPair = userKeyGen.genKeyPair();
		final KeyPair userBKeyPair = userKeyGen.genKeyPair();

		//ProtocolListener listenerA = getProtocolListener(7777, userBKeyPair.getPublic(), userAKeyPair.getPrivate());
		final ProtocolCommsService listenerA = getProtocolCommsService(7777, userBKeyPair.getPublic(), userAKeyPair.getPrivate());
		ProtocolCommsHandler handlerA = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request) {
				String[] chunks = new String[]{"foo", "bar"};
				ProtocolMessage resp = new QueryChunksProtocolJsonResponse(chunks, "userId", "nonce", userBKeyPair.getPublic());
				try {
					listenerA.reply(resp, request.getMessageID());
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
		listenerA.setHandler(handlerA);
		//ProtocolSender senderA = getProtocolSender(userBKeyPair.getPublic(), userAKeyPair.getPrivate());

		//ProtocolListener listenerB = getProtocolListener(7778, userAKeyPair.getPublic(), userBKeyPair.getPrivate());
		final ProtocolCommsService listenerB = getProtocolCommsService(7778, userAKeyPair.getPublic(), userBKeyPair.getPrivate());
		ProtocolCommsHandler handlerB = new ProtocolCommsHandler() {
			@Override public void requestReceived(ProtocolMessage request) {
				String[] chunks = new String[]{"foo", "bar"};
				ProtocolMessage resp = new QueryChunksProtocolJsonResponse(chunks, "userId", "nonce", userAKeyPair.getPublic());
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
		//ProtocolSender senderB = getProtocolSender(userAKeyPair.getPublic(), userBKeyPair.getPrivate());
		listenerA.start();
		listenerB.start();

		KeyPairGenerator commsKeyGen = KeyPairGenerator.getInstance("EC");
		KeyPair fetchPairUserA = commsKeyGen.generateKeyPair();

		ProtocolJsonMessage testMessage = new QueryChunksProtocolJsonRequest(testChunks, "userA", "nonceA", fetchPairUserA.getPublic());
		listenerA.sendMessage("127.0.0.1", 7778, testMessage);
		assertTrue("Message never received", lock.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testContainsFullMessage() {
		String testmessage = "DEADBEEF";
		ByteBuffer testbuf = ByteBuffer.allocate(1 + Integer.BYTES + testmessage.getBytes().length);
		byte whatever = 0x01;
		testbuf.put(whatever);
		testbuf.putInt(testmessage.getBytes().length);
		testbuf.put(testmessage.getBytes());
		ByteBuffer clone = testbuf.duplicate();
		boolean fullMessage = ProtocolMessage.containsFullMessage(testbuf);
		assertTrue("ByteBuffer was modified - it shouldn't be", testbuf.equals(clone));
		assertTrue("FullMessage came back false, should be true", fullMessage);
	}
}
