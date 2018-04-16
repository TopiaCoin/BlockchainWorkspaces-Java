package io.topiacoin.model;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

public class Workspace {

    private String name;
    private String description;
    private int status;
    private SecretKey workspaceKey;
    private String guid;
    private long lastModified;

    private List<Member> members;

    private List<File> files;

    private List<Message> messages;

    public Workspace() {
        this.members = new ArrayList<Member>();
        this.files = new ArrayList<File>();
        this.messages = new ArrayList<Message>();
    }

    public Workspace(String name, String description, int status, SecretKey workspaceKey, String guid, long lastModified, List<Member> members, List<File> files, List<Message> messages) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.workspaceKey = workspaceKey;
        this.guid = guid;
        this.lastModified = lastModified;
        this.members = (members != null ? new ArrayList<Member>(members) : new ArrayList<Member>());
        this.files = (files != null ? new ArrayList<File>(files) : new ArrayList<File>());
        this.messages = (messages != null ? new ArrayList<Message>(messages) : new ArrayList<Message>());
    }

    public Workspace(Workspace workspace) {
        this.name = workspace.name;
        this.description = workspace.description;
        this.status = workspace.status;
        this.workspaceKey = workspace.workspaceKey;
        this.guid = workspace.guid;
        this.lastModified = workspace.lastModified;
        this.members = (workspace.members != null ? new ArrayList<Member>(workspace.members) : new ArrayList<Member>());
        this.files = (workspace.files != null ? new ArrayList<File>(workspace.files) : new ArrayList<File>());
        this.messages = (workspace.messages != null ? new ArrayList<Message>(workspace.messages) : new ArrayList<Message>());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public SecretKey getWorkspaceKey() {
        return workspaceKey;
    }

    public void setWorkspaceKey(SecretKey workspaceKey) {
        this.workspaceKey = workspaceKey;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public List<Member> getMembers() {
        return new ArrayList<Member>(members);
    }

    public void setMembers(List<Member> members) {
        this.members.clear();
        if ( members != null ) {
            this.members.addAll(members) ;
        }
    }

    public List<File> getFiles() {
        return new ArrayList<File>(files);
    }

    public void setFiles(List<File> files) {
        this.files.clear(); ;
        if ( files != null ) {
            this.files.addAll(files);
        }
    }

    public List<Message> getMessages() {
        return new ArrayList<Message>(messages);
    }

    public void setMessages(List<Message> messages) {
        this.messages.clear() ;
        if ( messages != null ) {
            this.messages.addAll(messages);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Workspace workspace = (Workspace) o;

        if (status != workspace.status) return false;
        if (lastModified != workspace.lastModified) return false;
        if (name != null ? !name.equals(workspace.name) : workspace.name != null) return false;
        if (description != null ? !description.equals(workspace.description) : workspace.description != null)
            return false;
        if (workspaceKey != null ? !workspaceKey.equals(workspace.workspaceKey) : workspace.workspaceKey != null)
            return false;
        if (guid != null ? !guid.equals(workspace.guid) : workspace.guid != null) return false;
        if (!members.equals(workspace.members)) return false;
        if (!files.equals(workspace.files)) return false;
        return messages.equals(workspace.messages);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + status;
        result = 31 * result + (workspaceKey != null ? workspaceKey.hashCode() : 0);
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        result = 31 * result + (int) (lastModified ^ (lastModified >>> 32));
        result = 31 * result + members.hashCode();
        result = 31 * result + files.hashCode();
        result = 31 * result + messages.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Workspace{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", workspaceKey=" + workspaceKey +
                ", guid='" + guid + '\'' +
                ", lastModified=" + lastModified +
                ", members=" + members +
                ", files=" + files +
                ", messages=" + messages +
                '}';
    }
}
