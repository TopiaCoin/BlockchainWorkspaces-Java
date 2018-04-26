package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.impl.retrievalStrategies.SimpleChunkRetrievalStrategy;
import io.topiacoin.chunks.intf.ChunkRetrievalStrategy;
import io.topiacoin.chunks.intf.ChunkRetrievalStrategyFactory;

import java.util.List;

public class SimpleChunkRetrievalStrategyFactory implements ChunkRetrievalStrategyFactory {
	@Override public ChunkRetrievalStrategy createStrategy(List<String> chunkIDs) {
		return new SimpleChunkRetrievalStrategy(chunkIDs);
	}
}
