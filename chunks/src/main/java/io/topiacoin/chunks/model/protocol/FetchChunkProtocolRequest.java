package io.topiacoin.chunks.model.protocol;

import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.MemberNode;

public class FetchChunkProtocolRequest extends ChunksProtocolMessage {
	private static final String MESSAGE_TYPE = "REQUEST_CHUNK";

	public FetchChunkProtocolRequest(String chunkID, String userID, String authToken) {
		super(new String[] { chunkID }, userID, authToken, true, MESSAGE_TYPE);
	}

	public FetchChunkProtocolRequest(String chunkID, CurrentUser me, MemberNode targetNode) {
		super(new String[] { chunkID }, me.getUserID(), targetNode.getAuthToken(), true, MESSAGE_TYPE);
	}

	public FetchChunkProtocolRequest() {
		super(null, null, null, true, MESSAGE_TYPE);
	}

	public String getChunkID() {
		return super.getChunks() == null || super.getChunks().length == 0 ? null : super.getChunks()[0];
	}
}
