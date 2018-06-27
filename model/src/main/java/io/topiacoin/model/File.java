package io.topiacoin.model;

import java.util.ArrayList;
import java.util.List;

public class File {

    private String name ;
    private String mimeType ;
    private String entryID ;
    private long containerID ;
    private String parentID ;
    private boolean isFolder;
    private int status;
    private String lockOwner;

    private List<FileVersion> versions ;

    public File() {
        versions = new ArrayList<FileVersion>();
    }

    public File(String name, String mimeType, String entryID, long containerID,String parentID, boolean isFolder, int status, String lockOwner, List<FileVersion> versions) {
        this.name = name;
        this.mimeType = mimeType;
        this.entryID = entryID;
        this.containerID = containerID;
        this.parentID = parentID;
        this.isFolder = isFolder;
        this.status = status;
        this.lockOwner = lockOwner;
        if ( versions != null ) {
            this.versions = new ArrayList<FileVersion>(versions);
        } else {
            this.versions = new ArrayList<FileVersion>();
        }
    }

    public File(File other) {
        this.name = other.name;
        this.mimeType = other.mimeType;
        this.entryID = other.entryID;
        this.containerID = other.containerID;
        this.parentID = other.parentID;
        this.isFolder = other.isFolder;
        this.status = other.status;
        this.lockOwner = other.lockOwner;
        if ( other.versions != null ) {
            this.versions = new ArrayList<FileVersion>(other.versions);
        } else {
            this.versions = new ArrayList<FileVersion>();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getEntryID() {
        return entryID;
    }

    public void setEntryID(String entryID) {
        this.entryID = entryID;
    }

    public long getContainerID() {
        return containerID;
    }

    public void setContainerID(long containerID) {
        this.containerID = containerID;
    }

    public String getParentID() {
        return parentID;
    }

    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    public List<FileVersion> getVersions() {
        return new ArrayList<FileVersion>(versions);
    }

    public void setVersions(List<FileVersion> versions) {
        this.versions.clear();
        if ( versions != null ) {
            this.versions.addAll(versions);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        File file = (File) o;

        if (isFolder != file.isFolder) return false;
        if (status != file.status) return false;
        if (name != null ? !name.equals(file.name) : file.name != null) return false;
        if (mimeType != null ? !mimeType.equals(file.mimeType) : file.mimeType != null) return false;
        if (entryID != null ? !entryID.equals(file.entryID) : file.entryID != null) return false;
        if (containerID != file.containerID) return false;
        if (parentID != null ? !parentID.equals(file.parentID) : file.parentID != null) return false;
        if (lockOwner != null ? !lockOwner.equals(file.lockOwner) : file.lockOwner != null) return false;
        return versions != null ? versions.equals(file.versions) : file.versions == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        result = 31 * result + (entryID != null ? entryID.hashCode() : 0);
        result = 31 * result + (int) (containerID ^ (containerID >>> 32));
        result = 31 * result + (parentID != null ? parentID.hashCode() : 0);
        result = 31 * result + (isFolder ? 1 : 0);
        result = 31 * result + status;
        result = 31 * result + (lockOwner != null ? lockOwner.hashCode() : 0);
        result = 31 * result + (versions != null ? versions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "File{" +
                "name='" + name + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", entryID='" + entryID + '\'' +
                ", containerID='" + containerID + '\'' +
                ", parentID='" + parentID + '\'' +
                ", isFolder=" + isFolder +
                ", status=" + status +
                ", lockOwner='" + lockOwner + '\'' +
                ", versions=" + versions +
                '}';
    }
}
