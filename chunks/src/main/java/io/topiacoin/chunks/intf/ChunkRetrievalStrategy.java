package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.model.ChunkLocationResponse;
import io.topiacoin.chunks.model.ChunkRetrievalPlan;

import java.security.KeyPair;
import java.util.List;

public interface ChunkRetrievalStrategy {
	public ChunkRetrievalPlan generateRetrievalPlan(ChunkLocationResponse[] chunkLocationResponses, KeyPair fetchPair, List<String> chunkIDs);
}
