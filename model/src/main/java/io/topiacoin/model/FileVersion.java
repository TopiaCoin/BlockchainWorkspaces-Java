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

    private List<FileChunk> fileChunks;

    private List<FileVersionReceipt> receipts;


    public FileVersion() {
    }

    public FileVersion(String entryID, String versionID, String ownerID, long size, long date, long uploadDate, String fileHash, List<FileTag> userTags, List<FileTag> systemTags, String status, List<FileChunk> fileChunks, List<FileVersionReceipt> receipts) {
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
        this.userTags = new ArrayList<FileTag>(userTags);
    }

    public List<FileTag> getSystemTags() {
        return new ArrayList<FileTag>(systemTags);
    }

    public void setSystemTags(List<FileTag> systemTags) {
        this.systemTags = new ArrayList<FileTag>(systemTags);
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
        this.fileChunks = new ArrayList<FileChunk>(fileChunks);
    }

    public List<FileVersionReceipt> getReceipts() {
        return new ArrayList<FileVersionReceipt>(receipts);
    }

    public void setReceipts(List<FileVersionReceipt> receipts) {
        this.receipts = new ArrayList<FileVersionReceipt>(receipts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileVersion that = (FileVersion) o;

        if (size != that.size) return false;
        if (date != that.date) return false;
        if (uploadDate != that.uploadDate) return false;
        if (!entryID.equals(that.entryID)) return false;
        if (!versionID.equals(that.versionID)) return false;
        if (!ownerID.equals(that.ownerID)) return false;
        if (!fileHash.equals(that.fileHash)) return false;
        if (!userTags.equals(that.userTags)) return false;
        if (!systemTags.equals(that.systemTags)) return false;
        if (!status.equals(that.status)) return false;
        if (!fileChunks.equals(that.fileChunks)) return false;
        return receipts.equals(that.receipts);
    }

    @Override
    public int hashCode() {
        int result = entryID.hashCode();
        result = 31 * result + versionID.hashCode();
        result = 31 * result + ownerID.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (date ^ (date >>> 32));
        result = 31 * result + (int) (uploadDate ^ (uploadDate >>> 32));
        result = 31 * result + fileHash.hashCode();
        result = 31 * result + userTags.hashCode();
        result = 31 * result + systemTags.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + fileChunks.hashCode();
        result = 31 * result + receipts.hashCode();
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
                '}';
    }
}
