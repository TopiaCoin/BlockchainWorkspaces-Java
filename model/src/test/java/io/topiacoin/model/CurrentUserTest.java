package io.topiacoin.model;

public class CurrentUserTest extends AbstractUserTest {

	@Override public User getUser(String userID, String email) {
		return new CurrentUser(userID, email);
	}

	@Override public User getUser() {
		return new CurrentUser();
	}
}