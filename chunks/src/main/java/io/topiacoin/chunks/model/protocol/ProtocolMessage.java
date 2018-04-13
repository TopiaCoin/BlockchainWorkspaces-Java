package io.topiacoin.chunks.model.protocol;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

/** A ProtocolMessage is a message designed to be sent using a ProtocolCommsService
 * Before a ProtocolMessage can be sent, it must first be signed by calling the sign() function.
 * The ProtocolComms will call isValid() to verify the validity of the Message
 *
 * When a ProtocolMessage is received, the ProtocolComms will call isValid() to verify the validity of the Message.
 * Then, its signature should be verified using the verify() function.
 * Should verify() fail, the ProtocolMessage received should be considered invalid, and should not be processed further for security reasons.
 *
 */
public interface ProtocolMessage {

	/**
	 * Signs the ProtocolMessage using the provided signingKey via the SHA1withECDSA algorithm, if necessary.
	 * Some ProtocolMessages may choose not to require signing for whatever reason - in this case, they can choose to NOP this function
	 *
	 * @param signingKey the key used to sign the ProtocolMessage
	 * @throws InvalidKeyException if the signingKey is not in the correct format for signing a message
	 */
	public void sign(PrivateKey signingKey) throws InvalidKeyException;

	/**
	 * Verifies this ProtocolMessage's signature using the provided senderPublicKey via the SHA1withECDSA algorithm, if necessary.
	 * Some ProtocolMessages may choose not to require signing for whatever reason - in this case, they can choose to NOP this function.
	 * That said, if a particular type of message generally expects to find a signature and doesn't, this function will return false
	 * @param senderPublicKey the PublicKey of the sender of this message, used to verify the signature
	 * @return true if the signature is valid, false otherwise
	 * @throws InvalidKeyException if the senderPublicKey is not in the correct format for verifying a message
	 */
	public boolean verify(PublicKey senderPublicKey) throws InvalidKeyException;

	/**
	 * Converts this ProtocolMessage to bytes, stored in a ByteBuffer
	 * @return a ByteBuffer representing this ProtocolMessage in binary
	 */
	public ByteBuffer toBytes();

	/**
	 * Converts a ByteBuffer of data to a ProtocolMessage
	 * @param bytes the ByteBuffer representing a ProtocolMessage
	 */
	public void fromBytes(ByteBuffer bytes);

	/**
	 * Returns true if this ProtocolMessage is internally consistent, false otherwise.
	 * @return true if this ProtocolMessage is internally consistent, false otherwise.
	 */
	public boolean isValid();

	/**
	 * Returns true if this ProtocolMessage represents a request - if false, it is a response. The Protocol uses this to determine certain communications behaviors.
	 * @return true if this ProtocolMessage represents a request - if false, it is a response.
	 */
	public boolean isRequest();

	/**
	 * Returns the Unique Message ID for this type of message
	 * @return the Unique Message ID for this type of message
	 */
	public String getType();
}
