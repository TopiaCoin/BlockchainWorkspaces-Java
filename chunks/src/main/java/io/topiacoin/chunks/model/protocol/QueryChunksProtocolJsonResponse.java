package io.topiacoin.chunks.model.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

public class QueryChunksProtocolJsonResponse extends ProtocolJsonRequest {
	private static final String REQUEST_TYPE = "HAVE_CHUNKS";

	private String[] _chunkIDs;

	public QueryChunksProtocolJsonResponse(String[] chunkIDs, String userID, String nonce, PublicKey pubkey) {
		super(REQUEST_TYPE, userID, nonce, pubkey);
		_chunkIDs = chunkIDs;
	}

	QueryChunksProtocolJsonResponse(JsonObject message) {
		JsonObject request = super.readMessage(message);
		JsonArray chunkIDs = request.getAsJsonArray("chunkIDs");
		String[] chunkIDsArr = new String[chunkIDs.size()];
		for (int i = 0; i < chunkIDsArr.length; i++) {
			chunkIDsArr[i] = chunkIDs.get(i).getAsString();
		}
		_chunkIDs = chunkIDsArr;

	}

	@Override public byte[] toBytes(PrivateKey privKey) throws SignatureException, InvalidKeyException {
		JsonObject message = super.toJsonObject();
		JsonObject response = super.getRequestOrResponse(message);
		JsonArray chunkIDs = new JsonArray();
		for (String chunk : _chunkIDs) {
			chunkIDs.add(chunk);
		}
		response.add("chunkIDs", chunkIDs);

		message = super.signMessage(privKey, message);
		return super.jsonObjectToBytes(message);
	}

	public String[] getChunkIDs() {
		return _chunkIDs;
	}
}
