package io.topiacoin.model;

import io.topiacoin.crypto.CryptoUtils;
import org.junit.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

import static org.junit.Assert.*;

public abstract class AbstractUserTest {

    public abstract User getUser(String userID, String email, PublicKey pubKey, PrivateKey privKeyWhereApplicable);

    public abstract User getUser();

    @Test
    public void testDefaultConstructor() throws Exception {
        User user = getUser();

        assertNull(user.getUserID());
        assertNull(user.getEmail());
    }

    @Test
    public void testConstructor() throws Exception {
        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";
        KeyPair keyPair = CryptoUtils.generateECKeyPair();

        User user = getUser(userID, email, keyPair.getPublic(), keyPair.getPrivate());

        assertEquals(userID, user.getUserID());
        assertEquals(email, user.getEmail());
    }

    @Test
    public void testBasicAccessors() throws Exception {
        String userID = UUID.randomUUID().toString();
        String email = "foo@example.com";

        User user = getUser();

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
		KeyPair keyPair = CryptoUtils.generateECKeyPair();

        User user1 = getUser(userID, email, keyPair.getPublic(), keyPair.getPrivate());
        User user2 = getUser(userID, email, keyPair.getPublic(), keyPair.getPrivate());

        assertEquals(user1, user1);
        assertEquals(user2, user2);
        assertEquals(user1, user2);
        assertEquals(user2, user1);

        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {
        User user1 = getUser();
        User user2 = getUser();

        assertEquals(user1, user1);
        assertEquals(user2, user2);
        assertEquals(user1, user2);
        assertEquals(user2, user1);

        assertEquals(user1.hashCode(), user2.hashCode());
    }
}
