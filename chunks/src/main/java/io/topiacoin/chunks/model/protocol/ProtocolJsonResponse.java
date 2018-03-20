package io.topiacoin.chunks.model.protocol;

import com.google.gson.JsonObject;

import java.security.PublicKey;

public abstract class ProtocolJsonResponse extends ProtocolJsonMessage {
	String _responseType;

	ProtocolJsonResponse(String responseType, String userID, String nonce, PublicKey pubkey) {
		super(userID, nonce, pubkey);
		_responseType = responseType;
	}

	ProtocolJsonResponse() {

	}

	public String getResponseType() {
		return _responseType;
	}

	@Override public JsonObject getRequestOrResponse(JsonObject message) {
		return message == null ? null : message.getAsJsonObject("response");
	}

	JsonObject readMessage(JsonObject message) {
		JsonObject response = super.readMessage(message);
		_responseType = response.get("response_type").getAsString();
		return response;
	}

	JsonObject toJsonObject() {
		JsonObject message = super.toJsonObject();
		JsonObject response = new JsonObject();
		JsonObject authorization = new JsonObject();
		response.addProperty("response_type", _responseType);
		response.addProperty("userID", _userID);
		response.addProperty("nonce", _nonce);
		authorization.addProperty("pubkey", _pubkey);
		response.add("authorization", authorization);
		message.add("response", response);
		return message;
	}
}
