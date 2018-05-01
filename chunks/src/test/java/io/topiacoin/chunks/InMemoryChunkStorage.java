package io.topiacoin.chunks;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.InvalidReservationException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunkStorage;
import io.topiacoin.chunks.intf.ReservationID;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class InMemoryChunkStorage implements ChunkStorage {
	Map<String, byte[]> chunkdata = new HashMap<>();
	@Override public void addChunk(String chunkID, InputStream chunkStream, ReservationID reservationID, boolean purgeable) throws DuplicateChunkException, InvalidReservationException, InsufficientSpaceException, IOException {
		if(chunkdata.containsKey(chunkID)) {
			throw new DuplicateChunkException("...");
		}
		byte[] data = IOUtils.toByteArray(chunkStream);
		chunkdata.put(chunkID, data);
	}

	@Override public InputStream getChunkDataStream(String chunkID) throws NoSuchChunkException {
		if(chunkdata.containsKey(chunkID)) {
			byte[] data = chunkdata.get(chunkID);
			return new ByteArrayInputStream(data);
		} else {
			throw new NoSuchChunkException("");
		}
	}

	@Override public byte[] getChunkData(String chunkID) throws NoSuchChunkException, IOException {
		if(chunkdata.containsKey(chunkID)) {
			byte[] data = chunkdata.get(chunkID);
			return data;
		} else {
			throw new NoSuchChunkException("");
		}
	}

	@Override public boolean hasChunk(String chunkID) {
		return chunkdata.containsKey(chunkID);
	}

	@Override public boolean removeChunk(String chunkID) {
		return chunkdata.remove(chunkID) != null;
	}

	@Override public long getStorageQuota() {
		return 999999999;
	}

	@Override public long getAvailableStorage() {
		return 999999999;
	}

	@Override public boolean purgeStorage(long neededAvailableSpace) {
		return false;
	}

	@Override public ReservationID reserveStorageSpace(long spaceToReserve) throws InsufficientSpaceException {
		return null;
	}

	@Override public void releaseSpaceReservation(ReservationID reservationID) throws InvalidReservationException {

	}
}
