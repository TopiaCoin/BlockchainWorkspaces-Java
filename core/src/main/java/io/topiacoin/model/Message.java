package io.topiacoin.model;

import java.util.Arrays;
import java.util.Objects;

public class Message {

    private String authorID;
    private String messageID;
    private long workspaceGuid;
    private long seq;
    private long timestamp;
    private String text;
    private String mimeType;
    private byte[] digitalSignature;


    public Message() {
    }

    public Message(String authorID, String messageID, long workspaceGuid, long seq, long timestamp, String text, String mimeType, byte[] digitalSignature) {
        this.authorID = authorID;
        this.messageID = messageID;
        this.workspaceGuid = workspaceGuid;
        this.seq = seq;
        this.timestamp = timestamp;
        this.text = text;
        this.mimeType = mimeType;
        this.digitalSignature = digitalSignature;
    }

    public Message(Message next) {
        this.authorID = next.authorID;
        this.messageID = next.messageID;
        this.workspaceGuid = next.workspaceGuid;
        this.seq = next.seq;
        this.timestamp = next.timestamp;
        this.text = next.text;
        this.mimeType = next.mimeType;
        this.digitalSignature = next.digitalSignature;
    }

    public String getAuthorID() {
        return authorID;
    }

    public void setAuthorID(String authorID) {
        this.authorID = authorID;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public long getWorkspaceGuid() {
        return workspaceGuid;
    }

    public void setWorkspaceGuid(long workspaceGuid) {
        this.workspaceGuid = workspaceGuid;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getDigitalSignature() {
        return digitalSignature;
    }

    public void setDigitalSignature(byte[] digitalSignature) {
        this.digitalSignature = digitalSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return workspaceGuid == message.workspaceGuid &&
                seq == message.seq &&
                timestamp == message.timestamp &&
                Objects.equals(authorID, message.authorID) &&
                Objects.equals(messageID, message.messageID) &&
                Objects.equals(text, message.text) &&
                Objects.equals(mimeType, message.mimeType) &&
                Arrays.equals(digitalSignature, message.digitalSignature);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(authorID, messageID, workspaceGuid, seq, timestamp, text, mimeType);
        result = 31 * result + Arrays.hashCode(digitalSignature);
        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "authorID='" + authorID + '\'' +
                ", messageID='" + messageID + '\'' +
                ", workspaceGuid='" + workspaceGuid + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", text='" + text + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", digitalSignature=" + Arrays.toString(digitalSignature) +
                '}';
    }
}
