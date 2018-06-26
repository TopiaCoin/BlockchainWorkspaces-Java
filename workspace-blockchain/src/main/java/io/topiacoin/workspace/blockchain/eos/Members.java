package io.topiacoin.workspace.blockchain.eos;

import io.topiacoin.model.Member;

import java.util.List;

public class Members extends TableResponse<Member> {

    public Members(List<Member> members, boolean hasMore, long continuationToken) {
        super(members, hasMore, continuationToken);
    }

    public List<Member> getMembers() {
        return getItems();
    }
}
