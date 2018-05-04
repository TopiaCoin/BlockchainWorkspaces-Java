package io.topiacoin.chunks;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.model.exceptions.NoSuchUserException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ChunkManagerTest {

	@Test
	public void chunkStorageCRUDTest() throws IOException, CryptographicException, NoSuchUserException, FailedToStartCommsListenerException {
		Configuration config = new DefaultConfiguration();
		config.setConfigurationOption("chunkStorageLoc", "./target/chunks1");
		config.setConfigurationOption("chunkStorageQuota", "1000");
		ChunkManager manager = new ChunkManager(config, null);
		assertTrue(!manager.hasChunk("blah"));
		try {
			manager.getChunkData("blah");
			fail();
		} catch (NoSuchChunkException e) {
			//good
		}
		try {
			manager.getChunkDataAsStream("blah");
			fail();
		} catch (NoSuchChunkException e) {
			//good
		}
		String chunkID1 = "chunk1";
		byte[] chunkData1 = "chunkdata1".getBytes();

		String chunkID2 = "chunk2";
		InputStream chunkData2 = new ByteArrayInputStream("chunkdata2".getBytes());

		String chunkID3 = "chunk3";
		File chunkData3 = new File("./target/chunkCRUDchunk3.dat");
		if (chunkData3.exists()) {
			chunkData3.delete();
		}
		chunkData3.createNewFile();
		FileOutputStream fos = new FileOutputStream(chunkData3);
		fos.write("chunkdata3".getBytes());
		fos.close();

		try {
			manager.addChunk(chunkID1, chunkData1);
			manager.addChunk(chunkID2, chunkData2);
			manager.addChunk(chunkID3, chunkData3);

			assertTrue(manager.hasChunk(chunkID1));
			assertTrue(manager.hasChunk(chunkID2));
			assertTrue(manager.hasChunk(chunkID3));

			assertTrue(Arrays.equals(manager.getChunkData(chunkID1), "chunkdata1".getBytes()));
			assertTrue(Arrays.equals(manager.getChunkData(chunkID2), "chunkdata2".getBytes()));
			assertTrue(Arrays.equals(manager.getChunkData(chunkID3), "chunkdata3".getBytes()));

			InputStream is = manager.getChunkDataAsStream(chunkID1);
			assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata1".getBytes()));
			is.close();
			is = manager.getChunkDataAsStream(chunkID2);
			assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata2".getBytes()));
			is.close();
			is = manager.getChunkDataAsStream(chunkID3);
			assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata3".getBytes()));
			is.close();

			manager.removeChunk(chunkID1);
			manager.removeChunk(chunkID2);
			manager.removeChunk(chunkID3);

			assertTrue(!manager.hasChunk(chunkID1));
			assertTrue(!manager.hasChunk(chunkID2));
			assertTrue(!manager.hasChunk(chunkID3));
		} catch (InsufficientSpaceException e) {
			fail();
		} catch (IOException e) {
			fail();
		} catch (DuplicateChunkException e) {
			fail();
		} catch (NoSuchChunkException e) {
			fail();
		}
	}

	@Test
	public void chunkFetchTest() throws IOException, CryptographicException, NoSuchUserException, FailedToStartCommsListenerException {
		Configuration config = new DefaultConfiguration();
		config.setConfigurationOption("chunkStorageLoc", "./target/chunks1");
		config.setConfigurationOption("chunkStorageQuota", "1000");
		ChunkManager manager = new ChunkManager(config, null);
		assertTrue(!manager.hasChunk("blah"));
		try {
			manager.getChunkData("blah");
			fail();
		} catch (NoSuchChunkException e) {
			//good
		}
		try {
			manager.getChunkDataAsStream("blah");
			fail();
		} catch (NoSuchChunkException e) {
			//good
		}
		String chunkID1 = "chunk1";
		byte[] chunkData1 = "chunkdata1".getBytes();

		String chunkID2 = "chunk2";
		InputStream chunkData2 = new ByteArrayInputStream("chunkdata2".getBytes());

		String chunkID3 = "chunk3";
		File chunkData3 = new File("./target/chunkCRUDchunk3.dat");
		if (chunkData3.exists()) {
			chunkData3.delete();
		}
		chunkData3.createNewFile();
		FileOutputStream fos = new FileOutputStream(chunkData3);
		fos.write("chunkdata3".getBytes());
		fos.close();

		try {
			manager.addChunk(chunkID1, chunkData1);
			manager.addChunk(chunkID2, chunkData2);
			manager.addChunk(chunkID3, chunkData3);

			assertTrue(manager.hasChunk(chunkID1));
			assertTrue(manager.hasChunk(chunkID2));
			assertTrue(manager.hasChunk(chunkID3));

			assertTrue(Arrays.equals(manager.getChunkData(chunkID1), "chunkdata1".getBytes()));
			assertTrue(Arrays.equals(manager.getChunkData(chunkID2), "chunkdata2".getBytes()));
			assertTrue(Arrays.equals(manager.getChunkData(chunkID3), "chunkdata3".getBytes()));

			InputStream is = manager.getChunkDataAsStream(chunkID1);
			assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata1".getBytes()));
			is.close();
			is = manager.getChunkDataAsStream(chunkID2);
			assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata2".getBytes()));
			is.close();
			is = manager.getChunkDataAsStream(chunkID3);
			assertTrue(Arrays.equals(IOUtils.toByteArray(is), "chunkdata3".getBytes()));
			is.close();

			manager.removeChunk(chunkID1);
			manager.removeChunk(chunkID2);
			manager.removeChunk(chunkID3);

			assertTrue(!manager.hasChunk(chunkID1));
			assertTrue(!manager.hasChunk(chunkID2));
			assertTrue(!manager.hasChunk(chunkID3));
			fail();
		} catch (InsufficientSpaceException e) {
			fail();
		} catch (IOException e) {
			fail();
		} catch (DuplicateChunkException e) {
			fail();
		} catch (NoSuchChunkException e) {
			fail();
		}
	}
}
