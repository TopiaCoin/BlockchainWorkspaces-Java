package io.topiacoin.model;

import java.util.Arrays;
import java.util.Objects;

public class Message {

    private String authorID;
    private String entityID;
    private long guid;
    private long seq;
    private long timestamp;
    private String text;
    private String mimeType;
    private byte[] digitalSignature;


    public Message() {
    }

    public Message(String authorID, String entityID, long guid, long seq, long timestamp, String text, String mimeType, byte[] digitalSignature) {
        this.authorID = authorID;
        this.entityID = entityID;
        this.guid = guid;
        this.seq = seq;
        this.timestamp = timestamp;
        this.text = text;
        this.mimeType = mimeType;
        this.digitalSignature = digitalSignature;
    }

    public Message(Message next) {
        this.authorID = next.authorID;
        this.entityID = next.entityID;
        this.guid = next.guid;
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

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public long getGuid() {
        return guid;
    }

    public void setGuid(long guid) {
        this.guid = guid;
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
        return guid == message.guid &&
                seq == message.seq &&
                timestamp == message.timestamp &&
                Objects.equals(authorID, message.authorID) &&
                Objects.equals(entityID, message.entityID) &&
                Objects.equals(text, message.text) &&
                Objects.equals(mimeType, message.mimeType) &&
                Arrays.equals(digitalSignature, message.digitalSignature);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(authorID, entityID, guid, seq, timestamp, text, mimeType);
        result = 31 * result + Arrays.hashCode(digitalSignature);
        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "authorID='" + authorID + '\'' +
                ", entityID='" + entityID + '\'' +
                ", guid='" + guid + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", text='" + text + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", digitalSignature=" + Arrays.toString(digitalSignature) +
                '}';
    }
}
