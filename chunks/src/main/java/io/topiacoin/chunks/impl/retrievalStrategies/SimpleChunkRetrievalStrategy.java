package io.topiacoin.chunks.impl.retrievalStrategies;

import io.topiacoin.chunks.intf.ChunkRetrievalStrategy;
import io.topiacoin.chunks.model.ChunkRetrievalPlan;
import io.topiacoin.model.MemberNode;
import io.topiacoin.chunks.model.protocol.HaveChunksProtocolResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class SimpleChunkRetrievalStrategy implements ChunkRetrievalStrategy {
	private static final Log _log = LogFactory.getLog(SimpleChunkRetrievalStrategy.class);
	private List<String> _chunkIDs = null;
	private List<String> _chunkIDsStillNeeded = null;
	private ChunkRetrievalPlan _plan;

	public SimpleChunkRetrievalStrategy(List<String> chunkIDs) {
		_plan = new ChunkRetrievalPlan(chunkIDs);
	}

	@Override public void submitLocationResponse(HaveChunksProtocolResponse response, MemberNode memberNode) {
		for(String chunkID : response.getChunkIDs()) {
			_plan.addChunk(chunkID, memberNode);
		}
	}

	@Override public void allResponsesSubmitted() {
		//I don't think I need to do anything here.
	}

	@Override public boolean isCompletePlan() {
		return _plan.isFullyFormedPlan();
	}

	@Override public void setChunkIDs(List<String> chunkIDs) {
		_chunkIDs = chunkIDs;
		_chunkIDsStillNeeded = _chunkIDs;
	}

	@Override public ChunkRetrievalPlan getPlan() {
		return _plan;
	}
}
