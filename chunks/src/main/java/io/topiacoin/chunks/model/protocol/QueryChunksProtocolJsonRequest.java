package io.topiacoin.chunks.model.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

public class QueryChunksProtocolJsonRequest extends ProtocolJsonRequest {
	private static final String REQUEST_TYPE = "QUERY_CHUNKS";

	private String[] _chunksRequired;

	public QueryChunksProtocolJsonRequest(String[] chunksRequired, String userID, String nonce, PublicKey pubkey) {
		super(REQUEST_TYPE, userID, nonce, pubkey);
		_chunksRequired = chunksRequired;
	}

	QueryChunksProtocolJsonRequest(JsonObject message) {
		JsonObject request = super.readMessage(message);
		JsonArray chunksRequired = request.getAsJsonArray("chunks_required");
		String[] chunksRequiredArr = new String[chunksRequired.size()];
		for (int i = 0; i < chunksRequiredArr.length; i++) {
			chunksRequiredArr[i] = chunksRequired.get(i).getAsString();
		}
		_chunksRequired = chunksRequiredArr;

	}

	@Override public byte[] toBytes(PrivateKey privKey) throws SignatureException, InvalidKeyException {
		JsonObject message = toJsonObject();
		message = super.signMessage(privKey, message);
		return super.jsonObjectToBytes(message);
	}

	JsonObject toJsonObject() {
		JsonObject message = super.toJsonObject();
		JsonObject request = super.getRequestOrResponse(message);
		JsonArray chunksRequired = new JsonArray();
		for (String chunk : _chunksRequired) {
			chunksRequired.add(chunk);
		}
		request.add("chunks_required", chunksRequired);
		return message;
	}

	public String[] getChunksRequired() {
		return _chunksRequired;
	}
}
