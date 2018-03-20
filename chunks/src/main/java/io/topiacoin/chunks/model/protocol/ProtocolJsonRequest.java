package io.topiacoin.chunks.model.protocol;

import com.google.gson.JsonObject;

import java.security.PublicKey;

public abstract class ProtocolJsonRequest extends ProtocolJsonMessage {
	String _requestType;

	ProtocolJsonRequest(String requestType, String userID, String nonce, PublicKey pubkey) {
		super(userID, nonce, pubkey);
		_requestType = requestType;
	}

	ProtocolJsonRequest() {

	}

	public String getRequestType() {
		return _requestType;
	}

	@Override public JsonObject getRequestOrResponse(JsonObject message) {
		return message == null ? null : message.getAsJsonObject("request");
	}

	JsonObject readMessage(JsonObject message) {
		JsonObject request = super.readMessage(message);
		_requestType = request.get("request_type").getAsString();
		return request;
	}

	JsonObject toJsonObject() {
		JsonObject message = super.toJsonObject();
		JsonObject request = new JsonObject();
		JsonObject authorization = new JsonObject();
		request.addProperty("request_type", _requestType);
		request.addProperty("userID", _userID);
		request.addProperty("nonce", _nonce);
		authorization.addProperty("pubkey", _pubkey);
		request.add("authorization", authorization);
		message.add("request", request);
		return message;
	}
}
