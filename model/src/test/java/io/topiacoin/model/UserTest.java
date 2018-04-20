package io.topiacoin.model;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class UserTest {


    @Test
    public void testDefaultConstructor() throws Exception {
        User user = new User();

        assertNull(user.getUserID());
        assertNull(user.getEmail());
    }

    @Test
    public void testConstructor() throws Exception {
        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User(userID, email);

        assertEquals(userID, user.getUserID());
        assertEquals(email, user.getEmail());
    }

    @Test
    public void testBasicAccessors() throws Exception {
        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = new User();

        assertNull(user.getUserID());
        user.setUserID(userID);
        assertEquals(userID, user.getUserID());
        user.setUserID(null);
        assertNull(user.getUserID());

        assertNull(user.getEmail());
        user.setEmail(email);
        assertEquals(email, user.getEmail());
        user.setEmail(null);
        assertNull(user.getEmail());
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user1 = new User(userID, email);
        User user2 = new User(userID, email);

        assertEquals(user1, user1);
        assertEquals(user2, user2);
        assertEquals(user1, user2);
        assertEquals(user2, user1);

        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {
        User user1 = new User();
        User user2 = new User();

        assertEquals(user1, user1);
        assertEquals(user2, user2);
        assertEquals(user1, user2);
        assertEquals(user2, user1);

        assertEquals(user1.hashCode(), user2.hashCode());
    }

}
