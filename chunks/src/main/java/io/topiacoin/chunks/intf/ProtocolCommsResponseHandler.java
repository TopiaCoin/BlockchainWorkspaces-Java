package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;

public interface ProtocolCommsResponseHandler {
	public void responseReceived(ProtocolMessage response, MessageID messageID);

	public void error(Throwable t, MessageID messageID);

	public void error(String message, boolean shouldReply, MessageID messageId);
}
