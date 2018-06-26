package io.topiacoin.workspace.blockchain.eos;

public class WorkspaceInfo {

    private long guid;
    private String workspaceName;
    private String workspaceDescription;
    private String owner;
    private String newOwner;

    public WorkspaceInfo(long guid, String workspaceName, String workspaceDescription, String owner, String newOwner) {
        this.guid = guid;
        this.workspaceName = workspaceName;
        this.workspaceDescription = workspaceDescription;
        this.owner = owner;
        this.newOwner = newOwner;
    }

    public long getGuid() {
        return guid;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getWorkspaceDescription() {
        return workspaceDescription;
    }

    public String getOwner() {
        return owner;
    }

    public String getNewOwner() {
        return newOwner;
    }
}
