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
        if (!entryID.equals(that.entryID)) return false;
        if (!versionID.equals(that.versionID)) return false;
        return recipientID.equals(that.recipientID);
    }

    @Override
    public int hashCode() {
        int result = entryID.hashCode();
        result = 31 * result + versionID.hashCode();
        result = 31 * result + recipientID.hashCode();
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
