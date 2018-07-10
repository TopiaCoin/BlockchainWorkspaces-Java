package io.topiacoin.model;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class CurrentUser extends User {
    private PrivateKey _privateKey;

    public CurrentUser() {
        super();
        _privateKey = null;
    }

    public CurrentUser(String userID, String email, PublicKey pubKey, PrivateKey pk) {
        super(userID, email, pubKey);
        _privateKey = pk;
    }

    public CurrentUser(CurrentUser other) {
        super(other);
        _privateKey = other._privateKey;
    }

    @Override
    public String toString() {
        return "CurrentUser{" +
                "userID='" + super.userID + '\'' +
                ", email='" + super.email + '\'' +
                ", publicKey='" + (this.publicKey == null ? "null" : Base64.getEncoder().encodeToString(this.publicKey.getEncoded())) + '\'' +
                ", privatekey=[" + (_privateKey == null ? "unset" : "set") + "]" +
                '}';
    }

	public PrivateKey getPrivateKey() {
        return _privateKey;
	}
}
