package io.topiacoin.model;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Workspace as understood by the DHT
 */
public class DHTWorkspaceEntry {
	private String _workspaceID;
	private SecretKey _dhtKey;
	private List<MemberNode> _memberNodes;

	public DHTWorkspaceEntry(String workspaceID, SecretKey dhtKey, List<MemberNode> memberNodes) {
		_workspaceID = workspaceID;
		_dhtKey = dhtKey;
		_memberNodes = memberNodes;
	}

	public String getWorkspaceID() {
		return _workspaceID;
	}

	public SecretKey getDhtKey() {
		return _dhtKey;
	}

	public List<MemberNode> getMemberNodes() {
		return _memberNodes;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DHTWorkspaceEntry that = (DHTWorkspaceEntry) o;
		return Objects.equals(_workspaceID, that._workspaceID) &&
				Objects.equals(_dhtKey, that._dhtKey);
	}

	@Override public int hashCode() {

		return Objects.hash(_workspaceID, _dhtKey);
	}
}
