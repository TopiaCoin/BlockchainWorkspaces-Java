package io.topiacoin.core;

import io.topiacoin.core.exceptions.UnableToCreateUserException;
import io.topiacoin.model.User;

public interface UsersAPI {

    /**
     * Returns the user with the specified identifier, if available. Depending on the configuration of the SDK, the
     * userIdentifier may be the userID, email address, or blockchain wallet address. If no user can be found with this
     * identifier, null is returned.
     *
     * @param userIdentifier
     *
     * @return
     */
    User getUser(String userIdentifier);

    /**
     * Attempts to create an account for the specified email Address. If the account is successfully created, the user
     * info for the account is returned. If the account already exists, an error is returned. If the account cannot be
     * created, an error is returned.
     *
     * @param emailAddress
     *
     * @return
     */
    User createUser(String emailAddress) throws UnableToCreateUserException;

    /**
     * Attempts to update the specified user account to set the wallet Address associated with it. If the wallet Address
     * is already set, an error is returned. If the SDK fails to update the wallet Address for any reason, an error is
     * returned.
     *
     * @param userIdentifier
     * @param walletAddress
     */
    void updateUserWalletAddress(String userIdentifier, String walletAddress);
}
