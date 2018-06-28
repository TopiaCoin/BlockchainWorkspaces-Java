package io.topiacoin.core.callbacks;

import io.topiacoin.model.Message;

public interface AcknowledgeMessageCallback {
	public void acknowlegedMessage(Message messageToAcknowledge);
	public void failedToAcknowledgeMessage(Message messageToAcknowledge);
}
