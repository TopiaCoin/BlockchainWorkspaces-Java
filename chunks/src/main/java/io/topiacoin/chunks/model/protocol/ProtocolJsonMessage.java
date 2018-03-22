package io.topiacoin.chunks.model.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.topiacoin.chunks.exceptions.InvalidSignatureException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public abstract class ProtocolJsonMessage extends ProtocolMessage {
	String _userID;
	String _nonce;
	String _pubkey;

	ProtocolJsonMessage(String userID, String nonce, PublicKey pubkey) {
		_userID = userID;
		_nonce = nonce;
		String pk = new BASE64Encoder().encode(pubkey.getEncoded());
		_pubkey = pk;
	}

	ProtocolJsonMessage() {

	}

	public abstract byte[] toBytes(PrivateKey privKey) throws SignatureException, InvalidKeyException;

	public static ProtocolJsonMessage fromBytes(byte[] data, PublicKey pubKey) throws InvalidSignatureException, IOException {
		if (data != null) {
			String msgString = new String(new BASE64Decoder().decodeBuffer(new String(data)));
			System.out.println("Parsing JSON: " + msgString);
			JsonObject message = new JsonParser().parse(msgString).getAsJsonObject();
			JsonElement msgData = message.get("request");
			JsonObject msgObj = null;
			String msgType = null;
			if(msgData != null) {
				msgObj = msgData.getAsJsonObject();
				msgType = msgObj.get("request_type").getAsString();
			} else {
				msgData = message.get("response");
				msgObj = msgData.getAsJsonObject();
				msgType = msgObj.get("response_type").getAsString();
			}
			String signatureB64 = message.get("signature").getAsString();
			verifySignature(pubKey, msgObj, signatureB64);
			if (msgType.equalsIgnoreCase("QUERY_CHUNKS")) {
				return new QueryChunksProtocolJsonRequest(message);
			} else if (msgType.equalsIgnoreCase("HAVE_CHUNKS")) {
				return new QueryChunksProtocolJsonResponse(message);
			}  else if (msgType.equalsIgnoreCase("REQUEST_CHUNK")) {
				throw new UnsupportedOperationException("Uhh");
			}
		}
		return null;
	}

	JsonObject signMessage(PrivateKey privKey, JsonObject message) throws InvalidKeyException, SignatureException {
		JsonObject data = getRequestOrResponse(message);
		if(data != null) {
			Gson gson = new Gson();
			String dataString = gson.toJson(data);
			Signature sig;
			try {
				sig = Signature.getInstance("SHA1withECDSA");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("Failed to init signature", e);
			}
			sig.initSign(privKey);
			sig.update(dataString.getBytes(Charset.forName("UTF-8")));
			byte[] signatureBytes = sig.sign();
			message.addProperty("signature", new BASE64Encoder().encode(signatureBytes));
			return message;
		}
		return null;
	}

	byte[] jsonObjectToBytes(JsonObject message) {
		if (message != null) {
			Gson gson = new Gson();
			String msgString = gson.toJson(message);
			return new BASE64Encoder().encode(msgString.getBytes()).getBytes(Charset.forName("UTF-8"));
		}
		return null;
	}

	public String getUserID() {
		return _userID;
	}

	public String getNonce() {
		return _nonce;
	}

	public String getAuthorizationPubKey() {
		return _pubkey;
	}

	public abstract JsonObject getRequestOrResponse(JsonObject message);

	JsonObject readMessage(JsonObject message) {
		JsonObject inner = getRequestOrResponse(message);
		_userID = inner.get("userID").getAsString();
		_nonce = inner.get("nonce").getAsString();
		JsonObject authorization = inner.getAsJsonObject("authorization");
		_pubkey = authorization.get("pubkey").getAsString();
		return inner;
	}

	static void verifySignature(PublicKey pubKey, JsonObject requestOrResponse, String signature) throws InvalidSignatureException {
		if(requestOrResponse != null) {
			Gson gson = new Gson();
			String dataString = gson.toJson(requestOrResponse);
			Signature sig;
			try {
				sig = Signature.getInstance("SHA1withECDSA");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("Failed to init signature", e);
			}
			try {
				byte[] signatureBytes = new BASE64Decoder().decodeBuffer(signature);
				sig.initVerify(pubKey);
				sig.update(dataString.getBytes(Charset.forName("UTF-8")));
				if(!sig.verify(signatureBytes)) {
					throw new InvalidSignatureException("Signature doesn't match");
				}
			} catch (SignatureException e) {
				throw new InvalidSignatureException("Invalid Signature", e);
			} catch (InvalidKeyException e) {
				throw new InvalidSignatureException("Invalid Key", e);
			} catch (IOException e) {
				throw new InvalidSignatureException("Couldn't parse Signature Base64", e);
			}
		}
	}

	//Can't do much at this level
	JsonObject toJsonObject() {
		JsonObject message = new JsonObject();
		return message;
	}
}
