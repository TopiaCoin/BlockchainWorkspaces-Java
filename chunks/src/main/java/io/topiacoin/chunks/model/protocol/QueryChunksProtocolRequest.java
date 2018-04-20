package io.topiacoin.chunks.model.protocol;

public class QueryChunksProtocolRequest extends ChunksProtocolMessage {
	private static final String MESSAGE_TYPE = "QUERY_CHUNKS";

	public QueryChunksProtocolRequest(String[] chunksRequired, String userID, String authToken) {
		super(chunksRequired, userID, authToken, true, MESSAGE_TYPE);
	}

	public QueryChunksProtocolRequest() {
		super(null, null, null, true, MESSAGE_TYPE);
	}

	public String[] getChunksRequired() {
		return super.getChunks();
	}
}
