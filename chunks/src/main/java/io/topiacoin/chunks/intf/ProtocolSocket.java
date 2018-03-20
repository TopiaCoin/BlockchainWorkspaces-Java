package io.topiacoin.chunks.intf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ProtocolSocket {
	OutputStream getOutputStream() throws IOException;

	void close() throws IOException;

	InputStream getInputStream() throws IOException;

	void shutdownOutput() throws IOException;
}
