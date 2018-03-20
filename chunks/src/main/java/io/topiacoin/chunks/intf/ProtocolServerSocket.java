package io.topiacoin.chunks.intf;

import java.io.IOException;

public interface ProtocolServerSocket {
	void close() throws IOException;

	ProtocolSocket accept() throws IOException;
}
