package io.topiacoin.chunks.model.protocol;

public class FetchChunkProtocolRequest extends ChunksProtocolMessage {
	private static final String MESSAGE_TYPE = "REQUEST_CHUNK";

	public FetchChunkProtocolRequest(String chunkID, String userID) {
		super(new String[] { chunkID }, userID, true, MESSAGE_TYPE);
	}

	public FetchChunkProtocolRequest() {
		super(null, null, true, MESSAGE_TYPE);
	}

	public String getChunkID() {
		return super.getChunks() == null || super.getChunks().length == 0 ? null : super.getChunks()[0];
	}
}
