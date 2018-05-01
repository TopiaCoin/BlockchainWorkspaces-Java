package io.topiacoin.chunks.model.protocol;

import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.MemberNode;

public class QueryChunksProtocolRequest extends ChunksProtocolMessage {
	private static final String MESSAGE_TYPE = "QUERY_CHUNKS";

	public QueryChunksProtocolRequest(String[] chunksRequired, String userID, String authToken) {
		super(chunksRequired, userID, authToken, true, MESSAGE_TYPE);
	}

	public QueryChunksProtocolRequest(String[] chunksRequired, CurrentUser me, MemberNode targetNode) {
		super(chunksRequired, me.getUserID(), targetNode.getAuthToken(), true, MESSAGE_TYPE);
	}

	public QueryChunksProtocolRequest() {
		super(null, null, null, true, MESSAGE_TYPE);
	}

	public String[] getChunksRequired() {
		return super.getChunks();
	}
}
