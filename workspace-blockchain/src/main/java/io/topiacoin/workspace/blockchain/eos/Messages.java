package io.topiacoin.workspace.blockchain.eos;

import io.topiacoin.model.File;
import io.topiacoin.model.Message;

import java.util.List;

public class Messages extends TableResponse<Message>{

    public Messages(List<Message> messages, boolean hasMore, Object continuationToken) {
        super(messages, hasMore, continuationToken);
    }

    public List<Message> getMessages() {
        return getItems();
    }
}
