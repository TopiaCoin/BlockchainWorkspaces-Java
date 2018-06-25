package io.topiacoin.workspace.blockchain;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.UsersAPI;
import io.topiacoin.core.exceptions.UnableToCreateUserException;
import io.topiacoin.model.User;

public class BlockchainUsersAPI implements UsersAPI {
    public BlockchainUsersAPI(Configuration configuration) {

    }

    /**
     * Returns the user with the specified identifier, if available. Depending on the configuration of the SDK, the
     * userIdentifier may be the userID, email address, or blockchain wallet address. If no user can be found with this
     * identifier, null is returned.
     *
     * @param userIdentifier
     *
     * @return
     */
    @Override
    public User getUser(String userIdentifier) {
        return null;
    }

    /**
     * Attempts to create an account for the specified email Address. If the account is successfully created, the user
     * info for the account is returned. If the account already exists, an error is returned. If the account cannot be
     * created, an error is returned.
     *
     * @param emailAddress
     *
     * @return
     */
    @Override
    public User createUser(String emailAddress) throws UnableToCreateUserException {
        return null;
    }

    /**
     * Attempts to update the specified user account to set the wallet Address associated with it. If the wallet Address
     * is already set, an error is returned. If the SDK fails to update the wallet Address for any reason, an error is
     * returned.
     *
     * @param userIdentifier
     * @param walletAddress
     */
    @Override
    public void updateUserWalletAddress(String userIdentifier, String walletAddress) {

    }
}
