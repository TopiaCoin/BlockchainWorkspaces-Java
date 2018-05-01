package io.topiacoin.model;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Objects;

public class User {

    String userID;
    String email;
    PublicKey publicKey;

    public User() {
    }

    public User(String userID, String email, PublicKey publicKey) {
        this.userID = userID;
        this.email = email;
        this.publicKey = publicKey;
    }

    public User(User other) {
        this.userID = other.userID;
        this.email = other.email;
        this.publicKey = other.publicKey;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return Objects.equals(userID, user.userID) &&
                Objects.equals(email, user.email) &&
                Objects.equals(publicKey, user.publicKey);
    }

    @Override public int hashCode() {

        return Objects.hash(userID, email, publicKey);
    }

    @Override
    public String toString() {
        return "User{" +
                "userID='" + userID + '\'' +
                ", email='" + email + '\'' +
                ", publicKey='" + (this.publicKey == null ? "null" : Base64.getEncoder().encodeToString(this.publicKey.getEncoded())) + '\'' +
                '}';
    }
}
