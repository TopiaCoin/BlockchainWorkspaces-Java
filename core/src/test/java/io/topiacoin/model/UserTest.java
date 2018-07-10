package io.topiacoin.model;

import java.security.PrivateKey;
import java.security.PublicKey;

public class UserTest extends AbstractUserTest {

	@Override public User getUser(String userID, String email, PublicKey publicKey, PrivateKey ignored) {
		return new User(userID, email, publicKey);
	}

	@Override public User getUser() {
		return new User();
	}
}
