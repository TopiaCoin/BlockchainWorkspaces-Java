package io.topiacoin.model;

import java.util.ArrayList;
import java.util.List;

public class FileVersion {

    private String entryID;
    private String versionID;
    private String ownerID;
    private long size;
    private long date;
    private long uploadDate;
    private String fileHash;
    private List<FileTag> userTags;
    private List<FileTag> systemTags;
    private String status;
    private String lockOwner;

    private List<FileChunk> fileChunks;

    private List<FileVersionReceipt> receipts;
    private List<String> ancestorVersionIDs;

    public FileVersion() {
        this.userTags = new ArrayList<FileTag>();
        this.systemTags = new ArrayList<FileTag>();
        this.fileChunks = new ArrayList<FileChunk>();
        this.receipts = new ArrayList<FileVersionReceipt>();
        this.ancestorVersionIDs = new ArrayList<String>();
    }

    public FileVersion(String entryID, String versionID, String ownerID, long size, long date, long uploadDate, String fileHash, String status, List<FileTag> userTags, List<FileTag> systemTags, List<FileChunk> fileChunks, List<FileVersionReceipt> receipts, List<String> ancestorVersionIDs, String lockOwner) {
        this.entryID = entryID;
        this.versionID = versionID;
        this.ownerID = ownerID;
        this.size = size;
        this.date = date;
        this.uploadDate = uploadDate;
        this.fileHash = fileHash;
        this.status = status;
        this.userTags = ( userTags != null ? new ArrayList<FileTag>(userTags) : new ArrayList<FileTag>());
        this.systemTags = (systemTags != null ? new ArrayList<FileTag>(systemTags) : new ArrayList<FileTag>());
        this.fileChunks = (fileChunks != null ? new ArrayList<FileChunk>(fileChunks) : new ArrayList<FileChunk>());
        this.receipts = (receipts != null ? new ArrayList<FileVersionReceipt>(receipts) : new ArrayList<FileVersionReceipt>());
        this.ancestorVersionIDs = (ancestorVersionIDs != null ? new ArrayList<String>(ancestorVersionIDs) : new ArrayList<String>());
    }

    public FileVersion(FileVersion other) {
        this.entryID = other.entryID;
        this.versionID = other.versionID;
        this.ownerID = other.ownerID;
        this.size = other.size;
        this.date = other.date;
        this.uploadDate = other.uploadDate;
        this.fileHash = other.fileHash;
        this.status = other.status;
        this.userTags = ( other.userTags != null ? new ArrayList<FileTag>(other.userTags) : new ArrayList<FileTag>());
        this.systemTags = (other.systemTags != null ? new ArrayList<FileTag>(other.systemTags) : new ArrayList<FileTag>());
        this.fileChunks = (other.fileChunks != null ? new ArrayList<FileChunk>(other.fileChunks) : new ArrayList<FileChunk>());
        this.receipts = (other.receipts != null ? new ArrayList<FileVersionReceipt>(other.receipts) : new ArrayList<FileVersionReceipt>());
        this.ancestorVersionIDs = other.ancestorVersionIDs;
        this.lockOwner = other.lockOwner;
    }

    public String getEntryID() {
        return entryID;
    }

    public void setEntryID(String entryID) {
        this.entryID = entryID;
    }

    public String getVersionID() {
        return versionID;
    }

    public void setVersionID(String versionID) {
        this.versionID = versionID;
    }

    public String getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(long uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public List<FileTag> getUserTags() {
        return new ArrayList<FileTag>(userTags);
    }

    public void setUserTags(List<FileTag> userTags) {
        this.userTags.clear();
        if ( userTags != null ) {
            this.userTags.addAll(userTags);
        }

    }

    public List<FileTag> getSystemTags() {
        return new ArrayList<FileTag>(systemTags);
    }

    public void setSystemTags(List<FileTag> systemTags) {
        this.systemTags.clear() ;
        if ( systemTags != null ) {
            this.systemTags.addAll(systemTags);
        }

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<FileChunk> getFileChunks() {
        return new ArrayList<FileChunk>(fileChunks);
    }

    public void setFileChunks(List<FileChunk> fileChunks) {
        this.fileChunks.clear() ;
        if ( fileChunks != null ) {
            this.fileChunks.addAll(fileChunks);
        }
    }

    public List<FileVersionReceipt> getReceipts() {
        return new ArrayList<FileVersionReceipt>(receipts);
    }

    public void setReceipts(List<FileVersionReceipt> receipts) {
        this.receipts.clear();
        if ( receipts != null ) {
            this.receipts.addAll(receipts);
        }
    }

    public List<String> getAncestorVersionIDs() { return this.ancestorVersionIDs; }

    public void setAncestorVersionIDs(List<String> ancestorVersionIDs) { this.ancestorVersionIDs = ancestorVersionIDs; }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileVersion that = (FileVersion) o;

        if (size != that.size) return false;
        if (date != that.date) return false;
        if (uploadDate != that.uploadDate) return false;
        if (entryID != null ? !entryID.equals(that.entryID) : that.entryID != null) return false;
        if (versionID != null ? !versionID.equals(that.versionID) : that.versionID != null) return false;
        if (ownerID != null ? !ownerID.equals(that.ownerID) : that.ownerID != null) return false;
        if (fileHash != null ? !fileHash.equals(that.fileHash) : that.fileHash != null) return false;
        if (!userTags.equals(that.userTags)) return false;
        if (!systemTags.equals(that.systemTags)) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (!fileChunks.equals(that.fileChunks)) return false;
        if(!ancestorVersionIDs.equals(that.ancestorVersionIDs)) return false;
        return receipts.equals(that.receipts);
    }

    @Override
    public int hashCode() {
        int result = entryID != null ? entryID.hashCode() : 0;
        result = 31 * result + (versionID != null ? versionID.hashCode() : 0);
        result = 31 * result + (ownerID != null ? ownerID.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (date ^ (date >>> 32));
        result = 31 * result + (int) (uploadDate ^ (uploadDate >>> 32));
        result = 31 * result + (fileHash != null ? fileHash.hashCode() : 0);
        result = 31 * result + userTags.hashCode();
        result = 31 * result + systemTags.hashCode();
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + fileChunks.hashCode();
        result = 31 * result + receipts.hashCode();
        result = 31 * result + ancestorVersionIDs.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FileVersion{" +
                "entryID='" + entryID + '\'' +
                ", versionID='" + versionID + '\'' +
                ", ownerID='" + ownerID + '\'' +
                ", size=" + size +
                ", date=" + date +
                ", uploadDate=" + uploadDate +
                ", fileHash='" + fileHash + '\'' +
                ", userTags=" + userTags +
                ", systemTags=" + systemTags +
                ", status='" + status + '\'' +
                ", fileChunks=" + fileChunks +
                ", receipts=" + receipts +
                ", receipts=" + ancestorVersionIDs +
                '}';
    }
}
