package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.exceptions.InvalidMessageIDException;
import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;

/**
 * The ProtocolCommsService describes the behavior of the SDFS protocol, sending messages, replying to messages, handling crypto, and framing packets
 */
public interface ProtocolCommsService {

	/**
	 * Sends a ProtocolMessage to the given location on the given port. If this is an initial request to this address, the transferKey will be required to be passed in
	 * Also, the authToken should be passed in. These values can be retrieved from the blockchain. Finally, the message you want to send should be passed in.
	 * Before calling this function, typically the ProtocolMessage should be signed.
	 * This function takes care of encrypting and framing the protocol message, establishing a connection (if necessary) to the destination, and sending the data.
	 *
	 * ProtocolMessage implementations are either meant to be Requests or Responses - if you pass a response into sendMessage, an InvalidMessageException will be thrown.
	 *
	 * If the listener hasn't been started, a FailedToStartCommsListenerException will the thrown. see {@link #startListener()} for more information
	 *
	 * @param location The destination location (typically an IP address or hostname)
	 * @param port The port number on the destination to connect to
	 * @param transferPublicKey The public transferKey of the destination, used to generate cryptographic keys for communications - from the blockchain
	 * @param authToken The authToken of the destination - from the blockchain
	 * @param message The message you want to send
	 * @return the messageID of this communication
	 * @throws InvalidKeyException If the transferPublicKey is invalid or, if this is an initial message, null.
	 * @throws IOException If a connection could not be established
	 * @throws InvalidMessageException If the message passed in is not properly formed, or if you pass a Response-type message in
	 * @throws FailedToStartCommsListenerException If you try to send a message without first starting the listener
	 */
	public MessageID sendMessage(String location, int port, byte[] transferPublicKey, String authToken, ProtocolMessage message)
			throws InvalidKeyException, IOException, InvalidMessageException, FailedToStartCommsListenerException;

	/**
	 * Replies to a received message - since this is a connection-based protocol, every received message is expected to be replied to, even if the reply indicates some failure.
	 * See {@link #setHandler(ProtocolCommsHandler)} for more information on how to set up replies.
	 *
	 * ProtocolMessage implementations are either meant to be Requests or Responses - if you pass a request into reply, an InvalidMessageException will be thrown.
	 *
	 * If the listener hasn't been started, a FailedToStartCommsListenerException will the thrown. see {@link #startListener()} for more information
	 *
	 * @param message The message you want to send as a reply
	 * @param messageID the messageID of the message you want to reply to.
	 * @throws FailedToStartCommsListenerException If you try to send a message without first starting the listener
	 * @throws InvalidMessageException If the message passed in is not properly formed, or if you pass a Response-type message in
	 * @throws InvalidMessageIDException If the messageID passed in is invalid, or a reply address cannot be determined for some reason
	 */
	public void reply(ProtocolMessage message, MessageID messageID) throws FailedToStartCommsListenerException, InvalidMessageException, InvalidMessageIDException;

	/**
	 * Sets the ProtocolCommsHandler that will be used to handle incoming messages from other SDFS clients. This must be set in order to start the listener.
	 * See {@link io.topiacoin.chunks.intf.ProtocolCommsHandler} for more information on the requirements of an implementation
	 * @param handler The handler
	 */
	public void setHandler(ProtocolCommsHandler handler);

	/**
	 * Starts the Listener, which listens to and serves incoming requests and responses from other SDFS clients
	 * @throws FailedToStartCommsListenerException If the parameters specified at construction (such as a port number to listen on) are invalid or unusable (e.g. bind exception)
	 */
	public void startListener() throws FailedToStartCommsListenerException;

	/**
	 * Stops the Listener. This should be called on shutdown.
	 */
	public void stopListener();
}
