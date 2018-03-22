package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.model.protocol.ProtocolMessage;

public interface ProtocolCommsHandler {
	public void requestReceived(ProtocolMessage request);

	public void responseReceived(ProtocolMessage response);
}
