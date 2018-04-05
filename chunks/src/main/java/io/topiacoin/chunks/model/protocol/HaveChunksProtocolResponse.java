package io.topiacoin.chunks.model.protocol;

public class HaveChunksProtocolResponse extends ChunksProtocolMessage {
	private static final String MESSAGE_TYPE = "HAVE_CHUNKS";

	public HaveChunksProtocolResponse(String[] chunksIHave, String userID) {
		super(chunksIHave, userID, false, MESSAGE_TYPE);
	}

	public HaveChunksProtocolResponse() {
		super(null, null, false, MESSAGE_TYPE);
	}

	public String[] getChunkIDs() {
		return super.getChunks();
	}
}
