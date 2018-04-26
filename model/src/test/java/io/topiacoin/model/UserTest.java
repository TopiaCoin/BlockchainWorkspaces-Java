package io.topiacoin.model;

public class UserTest extends AbstractUserTest {

	@Override public User getUser(String userID, String email) {
		return new User(userID, email);
	}

	@Override public User getUser() {
		return new User();
	}
}
