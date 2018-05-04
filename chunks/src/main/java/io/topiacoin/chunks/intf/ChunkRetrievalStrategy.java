package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.model.ChunkRetrievalPlan;
import io.topiacoin.chunks.model.protocol.ErrorProtocolResponse;
import io.topiacoin.model.UserNode;
import io.topiacoin.chunks.model.protocol.HaveChunksProtocolResponse;

import java.util.List;

public interface ChunkRetrievalStrategy {
	public void submitLocationResponse(HaveChunksProtocolResponse response, UserNode memberNode);

	public void submitLocationResponse(ErrorProtocolResponse response, UserNode memberNode);

	public void allResponsesSubmitted();

	public boolean isCompletePlan();

	public void setChunkIDs(List<String> chunkIDs);

	public ChunkRetrievalPlan getPlan();
}
