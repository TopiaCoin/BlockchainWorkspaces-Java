package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;

public interface ProtocolCommsHandler {
	public void requestReceived(ProtocolMessage request, MessageID mesesageID);

	public void responseReceived(ProtocolMessage response);

	public void error(Throwable t);

	public void error(String message, boolean shouldReply, MessageID messageId);
}
