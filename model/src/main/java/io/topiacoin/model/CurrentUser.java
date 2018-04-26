package io.topiacoin.model;

public class CurrentUser extends User {
    public CurrentUser() {
        super();
    }

    public CurrentUser(String userID, String email) {
        super(userID, email);
    }

    public CurrentUser(CurrentUser other) {
        super(other);
    }

    @Override
    public String toString() {
        return "CurrentUser{" +
                "userID='" + super.userID + '\'' +
                ", email='" + super.email + '\'' +
                '}';
    }
}
