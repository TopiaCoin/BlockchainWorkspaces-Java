package io.topiacoin.model;

public class Member {

    private String userID;
    private int status;
    private long inviteDate;
    private String inviterID;
    private String authToken;
    private String lockOwner;


    public Member() {
    }

    public Member(String userID, int status, long inviteDate, String inviterID, String authToken, String lockOwner) {
        this.userID = userID;
        this.status = status;
        this.inviteDate = inviteDate;
        this.inviterID = inviterID;
        this.authToken = authToken;
        this.lockOwner = lockOwner;
    }

    public Member(Member member) {
        this.userID = member.userID;
        this.status = member.status;
        this.inviteDate = member.inviteDate;
        this.inviterID = member.inviterID;
        this.authToken = member.authToken;
        this.lockOwner = member.lockOwner;
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

        Member member = (Member) o;

        if (status != member.status) return false;
        if (inviteDate != member.inviteDate) return false;
        if (userID != null ? !userID.equals(member.userID) : member.userID != null) return false;
        return inviterID != null ? inviterID.equals(member.inviterID) : member.inviterID == null;
    }

    @Override
    public int hashCode() {
        int result = userID != null ? userID.hashCode() : 0;
        result = 31 * result + status;
        result = 31 * result + (int) (inviteDate ^ (inviteDate >>> 32));
        result = 31 * result + (inviterID != null ? inviterID.hashCode() : 0);
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

    public String getAuthToken() {
        return this.authToken;
    }

	public void setAuthToken(String authToken) {
        this.authToken = authToken;
	}
}
