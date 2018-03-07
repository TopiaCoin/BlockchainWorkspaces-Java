package io.topiacoin.model;

public class Member {

    private String userID;
    private int status;
    private long inviteDate;
    private String inviterID;


    public Member() {
    }

    public Member(String userID, int status, long inviteDate, String inviterID) {
        this.userID = userID;
        this.status = status;
        this.inviteDate = inviteDate;
        this.inviterID = inviterID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getInviteDate() {
        return inviteDate;
    }

    public void setInviteDate(long inviteDate) {
        this.inviteDate = inviteDate;
    }

    public String getInviterID() {
        return inviterID;
    }

    public void setInviterID(String inviterID) {
        this.inviterID = inviterID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Member member = (Member) o;

        if (status != member.status) return false;
        if (inviteDate != member.inviteDate) return false;
        if (!userID.equals(member.userID)) return false;
        return inviterID.equals(member.inviterID);
    }

    @Override
    public int hashCode() {
        int result = userID.hashCode();
        result = 31 * result + status;
        result = 31 * result + (int) (inviteDate ^ (inviteDate >>> 32));
        result = 31 * result + inviterID.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Member{" +
                "userID='" + userID + '\'' +
                ", status=" + status +
                ", inviteDate=" + inviteDate +
                ", inviterID='" + inviterID + '\'' +
                '}';
    }
}
