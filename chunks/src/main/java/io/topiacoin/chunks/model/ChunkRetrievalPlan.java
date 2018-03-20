package io.topiacoin.chunks.model;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkRetrievalPlan {
	private Map<String, Chunk> chunks = new HashMap<String, Chunk>();
	private Map<String, SecretKey> keys = new HashMap<String, SecretKey>();
	private List<String> chunkIDs;
	private List<String> plannedChunkIDs = new ArrayList<>();

	public ChunkRetrievalPlan(List<String> chunkIDs) {
		this.chunkIDs = chunkIDs;
	}

	public void addChunk(int chunkIdx, String chunkID, String chunkSource) {
		if(chunkIDs.contains(chunkID)) {
			Chunk chunk = getChunk(chunkID);
			if (chunk == null) {
				chunk = new Chunk(chunkIdx, chunkID);
			}
			chunk.addSource(chunkSource);
			chunks.put(chunkID, chunk);
			plannedChunkIDs.add(chunkID);
		}
	}

	public Chunk getChunk(String chunkID) {
		return chunks.get(chunkID);
	}

	public void addKey(String source, SecretKey key) {
		keys.put(source, key);
	}

	public SecretKey getKey(String source) {
		return keys.get(source);
	}

	public boolean isCompletePlan() {
		return chunkIDs.size() == plannedChunkIDs.size();
	}

	public class Chunk {
		int chunkIdx;
		String chunkID;
		List<String> chunkSources = new ArrayList<String>();

		public Chunk(int idx, String id) {
			chunkIdx = idx;
			chunkID = id;
		}

		public void addSource(String source) {
			if(!chunkSources.contains(source)) {
				chunkSources.add(source);
			}
		}
	}
}
