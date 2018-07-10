package io.topiacoin.model;

public class FileVersionReceipt {

    private String entryID;
    private String versionID;
    private String recipientID;
    private long date;

    public FileVersionReceipt() {
    }

    public FileVersionReceipt(String entryID, String versionID, String recipientID, long date) {
        this.entryID = entryID;
        this.versionID = versionID;
        this.recipientID = recipientID;
        this.date = date;
    }

    public FileVersionReceipt(FileVersionReceipt other) {
        this.entryID = other.entryID;
        this.versionID = other.versionID;
        this.recipientID = other.recipientID;
        this.date = other.date;
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

    public String getRecipientID() {
        return recipientID;
    }

    public void setRecipientID(String recipientID) {
        this.recipientID = recipientID;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileVersionReceipt that = (FileVersionReceipt) o;

        if (date != that.date) return false;
        if (entryID != null ? !entryID.equals(that.entryID) : that.entryID != null) return false;
        if (versionID != null ? !versionID.equals(that.versionID) : that.versionID != null) return false;
        return recipientID != null ? recipientID.equals(that.recipientID) : that.recipientID == null;
    }

    @Override
    public int hashCode() {
        int result = entryID != null ? entryID.hashCode() : 0;
        result = 31 * result + (versionID != null ? versionID.hashCode() : 0);
        result = 31 * result + (recipientID != null ? recipientID.hashCode() : 0);
        result = 31 * result + (int) (date ^ (date >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "FileVersionReceipt{" +
                "entryID='" + entryID + '\'' +
                ", versionID='" + versionID + '\'' +
                ", recipientID='" + recipientID + '\'' +
                ", date=" + date +
                '}';
    }
}
