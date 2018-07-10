package io.topiacoin.model;

import java.security.PrivateKey;
import java.security.PublicKey;

public class CurrentUserTest extends AbstractUserTest {

	@Override public User getUser(String userID, String email, PublicKey pubKey, PrivateKey privKeyWhereApplicable) {
		return new CurrentUser(userID, email, pubKey, privKeyWhereApplicable);
	}

	@Override public User getUser() {
		return new CurrentUser();
	}
}