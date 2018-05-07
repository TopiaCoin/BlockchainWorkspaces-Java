package io.topiacoin.chunks.model;

import io.topiacoin.model.UserNode;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualTreeBidiMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChunkRetrievalPlan {
	private List<String> _plannedChunkIDs;
	private Set<String> _retrievableChunkIDs = new HashSet<String>();
	private Set<UserNode> _sources = new HashSet<>();
	private Map<UserNode, Set<String>> _nodeChunks = new HashMap<>();
	private BidiMap<String, UserNode> _chunksAndSourcesInUse = new DualTreeBidiMap<>();
	private Set<String> _fetchedChunks = new HashSet<>();

	public ChunkRetrievalPlan(List<String> chunkIDs) {
		this._plannedChunkIDs = chunkIDs;
	}

	public void addChunk(String chunkID, UserNode chunkSource) {
		if (_plannedChunkIDs.contains(chunkID)) {
			_sources.add(chunkSource);
			Set<String> nodeChunkSet = _nodeChunks.get(chunkSource);
			if(nodeChunkSet == null) {
				nodeChunkSet = new HashSet<>();
			}
			nodeChunkSet.add(chunkID);
			_nodeChunks.put(chunkSource, nodeChunkSet);
			_retrievableChunkIDs.add(chunkID);
		}
	}

	public boolean isFullyFormedPlan() {
		return _retrievableChunkIDs.size() == _plannedChunkIDs.size();
	}

	public PlanTask getNextTask() {
		PlanTask tr = null;
		//Remove the set of sources in use from the list of all sources
		Set<UserNode> unusedNodes = new HashSet<>(_sources);
		unusedNodes.removeAll(_chunksAndSourcesInUse.values());
		//If there are any sources that are not currently in use...
		if(!unusedNodes.isEmpty()) {
			//Iterate over the set of unused nodes
			Iterator<UserNode> nodeIterator = unusedNodes.iterator();
			while(tr == null && nodeIterator.hasNext()) {
				UserNode unusedNode = nodeIterator.next();
				//Grab the full chunk list for the node
				Set<String> nodeChunkSet = _nodeChunks.get(unusedNode);
				//Remove the set of chunks we've already fetched
				nodeChunkSet.removeAll(_fetchedChunks);
				if(nodeChunkSet.isEmpty()) {
					//This source no longer has anything useful for us. Drop it
					_sources.remove(unusedNode);
					_nodeChunks.remove(unusedNode);
				} else {
					//...then remove the set of chunks we're already working on
					nodeChunkSet.removeAll(_chunksAndSourcesInUse.keySet());
					if(!nodeChunkSet.isEmpty()) {
						//This node, which is not in use, has at least one chunk we are not currently fetching.
						String chunk = nodeChunkSet.iterator().next();
						_chunksAndSourcesInUse.put(chunk, unusedNode);
						tr = new PlanTask(chunk, unusedNode);
					} //else this node doesn't presently have anything useful for us
				}
			}
		}
		//If all sources are in use, or if none of the unused nodes have anything useful for us at the moment, there is no next task.
		return tr;
	}

	public void markChunkAsFetched(String chunkID) {
		UserNode node = _chunksAndSourcesInUse.remove(chunkID);
		_fetchedChunks.add(chunkID);
		Set<String> chunks = _nodeChunks.get(node);
		chunks.remove(chunkID);
	}

	public void markChunkAsFailed(String chunkID) {
		UserNode node = _chunksAndSourcesInUse.remove(chunkID);
		Set<String> chunks = _nodeChunks.get(node);
		chunks.remove(chunkID);
	}

	public boolean isComplete() {
		return _fetchedChunks.size() == _plannedChunkIDs.size() || _sources.isEmpty();
	}

	public int getChunksFetched() {
		return _fetchedChunks.size();
	}

	public List<String> getFailedChunks() {
		List<String> failedChunks = _plannedChunkIDs;
		failedChunks.removeAll(_fetchedChunks);
		return failedChunks;
	}

	public int getTotalChunks() {
		return _plannedChunkIDs.size();
	}

	public class PlanTask {
		public String chunkID;
		public UserNode source;

		PlanTask(String chunkID, UserNode source) {
			this.chunkID = chunkID;
			this.source = source;
		}
	}
}
