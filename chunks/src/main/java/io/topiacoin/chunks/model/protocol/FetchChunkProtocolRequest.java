package io.topiacoin.chunks.model.protocol;

public class FetchChunkProtocolRequest extends ChunksProtocolMessage {
	private static final String MESSAGE_TYPE = "REQUEST_CHUNK";

	public FetchChunkProtocolRequest(String chunkID, String userID, String authToken) {
		super(new String[] { chunkID }, userID, authToken, true, MESSAGE_TYPE);
	}

	public FetchChunkProtocolRequest() {
		super(null, null, null, true, MESSAGE_TYPE);
	}

	public String getChunkID() {
		return super.getChunks() == null || super.getChunks().length == 0 ? null : super.getChunks()[0];
	}
}
