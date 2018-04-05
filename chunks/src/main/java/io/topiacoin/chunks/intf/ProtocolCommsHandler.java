package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.model.protocol.ProtocolMessage;

public interface ProtocolCommsHandler {
	public void requestReceived(ProtocolMessage request, int mesesageID);

	public void responseReceived(ProtocolMessage response);

	public void error(Throwable t);
}
