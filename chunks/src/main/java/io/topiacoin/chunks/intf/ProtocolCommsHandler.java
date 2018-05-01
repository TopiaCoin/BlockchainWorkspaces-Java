package io.topiacoin.chunks.intf;

import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;

public interface ProtocolCommsHandler {
	void requestReceived(ProtocolMessage request, MessageID messageID);

	void responseReceived(ProtocolMessage response, MessageID messageID);

	void error(Throwable t);

	void error(String message, boolean shouldReply, MessageID messageId);
}
