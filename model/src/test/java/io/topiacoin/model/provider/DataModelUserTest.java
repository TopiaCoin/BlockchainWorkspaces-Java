package io.topiacoin.model.provider;

import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.User;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.model.exceptions.UserAlreadyExistsException;
import org.junit.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public abstract class DataModelUserTest {

    public abstract DataModel initDataModel();

    // -------- User Tests --------

    @Test
    public void testUserCRUD() throws Exception {

        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";
        KeyPair keyPair = CryptoUtils.generateECKeyPair();

        User user = new User();
        user.setUserID(userID);
        user.setEmail(email);
        user.setPublicKey(keyPair.getPublic());

        DataModel dataModel = initDataModel();

        List<User> users;

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(0, users.size());

        dataModel.addUser(user);

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(user, users.get(0));
        assertNotSame(user, users.get(0));

        User fetchedUser = dataModel.getUserByID(userID);
        assertNotNull(fetchedUser);
        assertEquals(user, fetchedUser);
        assertNotSame(user, fetchedUser);

        fetchedUser = dataModel.getUserByEmail(email);
        assertNotNull(fetchedUser);
        assertEquals(user, fetchedUser);
        assertNotSame(user, fetchedUser);


        user.setEmail("bar@example.com");
        dataModel.updateUser(user);

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(user, users.get(0));
        assertNotSame(user, users.get(0));

        dataModel.removeUser(user);

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(0, users.size());

        CurrentUser currentUser = new CurrentUser(userID, email, keyPair.getPublic(), keyPair.getPrivate());
        dataModel.setCurrentUser(currentUser);

        CurrentUser fetchedCurrentUser = dataModel.getCurrentUser();
        assertEquals(currentUser, fetchedCurrentUser);

        dataModel.removeCurrentUser();

        try {
            dataModel.getCurrentUser();
            fail();
        } catch (NoSuchUserException e) {
            //Good
        }
    }

    @Test
    public void testRemoveUserByUserID() throws Exception {

        String userID = UUID.randomUUID().toString();

        User user = new User();
        user.setUserID(userID);
        user.setEmail("foo@example.com");

        DataModel dataModel = initDataModel();

        List<User> users;

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(0, users.size());

        dataModel.addUser(user);

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(user, users.get(0));
        assertNotSame(user, users.get(0));

        dataModel.removeUser(userID);

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(0, users.size());
    }

    @Test
    public void testChangingUserAfterAddDoesNotChangeModel() throws Exception {

        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User();
        user.setUserID(userID);
        user.setEmail(email);

        List<User> users;

        DataModel dataModel = initDataModel();

        dataModel.addUser(user);

        user.setEmail("bar@example.com");

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertNotEquals(user, users.get(0));
    }

    @Test
    public void testChangingUserAfterFetchDoesNotChangeModel() throws Exception {

        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User();
        user.setUserID(userID);
        user.setEmail(email);

        DataModel dataModel = initDataModel();

        dataModel.addUser(user);

        List<User> users;

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(user, users.get(0));

        User fetchedUser = users.get(0);

        fetchedUser.setEmail("bar@example.com");

        users = dataModel.getUsers();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertNotEquals(fetchedUser, users.get(0));
        assertEquals(user, users.get(0));
    }

    @Test(expected = NoSuchUserException.class)
    public void testGetNonExistentUserByID() throws Exception {

        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User();
        user.setUserID(userID);
        user.setEmail(email);

        DataModel dataModel = initDataModel();

        List<User> users;

        User fetchedUser = dataModel.getUserByID(userID);
    }

    @Test(expected = NoSuchUserException.class)
    public void testGetNonExistentUserByEmail() throws Exception {

        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User();
        user.setUserID(userID);
        user.setEmail(email);

        DataModel dataModel = initDataModel();

        List<User> users;

        User fetchedUser = dataModel.getUserByEmail(userID);
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void testAddDuplicateUser() throws Exception {

        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User();
        user.setUserID(userID);
        user.setEmail(email);

        DataModel dataModel = initDataModel();

        List<User> users;

        dataModel.addUser(user);

        dataModel.addUser(user);
    }

    @Test(expected = NoSuchUserException.class)
    public void testUpdateNonExistentUser() throws Exception {

        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User();
        user.setUserID(userID);
        user.setEmail(email);

        DataModel dataModel = initDataModel();

        dataModel.updateUser(user);
    }

    @Test(expected = NoSuchUserException.class)
    public void testRemoveNonExistentUser() throws Exception {

        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User();
        user.setUserID(userID);
        user.setEmail(email);

        DataModel dataModel = initDataModel();

        dataModel.removeUser(user);
    }


}
