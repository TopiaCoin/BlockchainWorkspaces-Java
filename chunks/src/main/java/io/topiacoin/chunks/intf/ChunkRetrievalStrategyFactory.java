package io.topiacoin.chunks.intf;

import java.util.List;

public interface ChunkRetrievalStrategyFactory {

	public ChunkRetrievalStrategy createStrategy(List<String> chunkIDs);
}
