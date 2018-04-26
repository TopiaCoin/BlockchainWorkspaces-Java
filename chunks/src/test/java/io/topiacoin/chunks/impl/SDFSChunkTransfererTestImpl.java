package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.intf.AbstractChunkTransfererTest;
import io.topiacoin.chunks.intf.ChunkTransferer;
import io.topiacoin.chunks.intf.ProtocolCommsHandler;

import java.io.IOException;
import java.security.KeyPair;

public class SDFSChunkTransfererTestImpl extends AbstractChunkTransfererTest {

	@Override public ChunkTransferer getChunkTransferer(int listenPort, KeyPair chunkTransferPair) throws IOException, FailedToStartCommsListenerException {
		return new SDFSChunkTransferer(listenPort, chunkTransferPair);
	}
}
