package io.topiacoin.model;

import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class WorkspaceTest {


    @Test
    public void testDefaultConstructor() throws Exception {
        Workspace workspace = new Workspace() ;

        assertNull ( workspace.getName()) ;
        assertNull(workspace.getDescription());
        assertEquals(0, workspace.getLastModified());
        assertEquals(0, workspace.getStatus());
        assertNull(workspace.getWorkspaceKey());

        assertNotNull(workspace.getFiles());
        assertNotNull(workspace.getMembers());
        assertNotNull(workspace.getMessages());
    }

    @Test
    public void testConstructor() throws Exception {

        String name = "Reclaiming Anniera";
        String description = "Return to the Shining Isle";
        int status = 56;
        SecretKey key = new SecretKeySpec(new byte[16], "AES");
        long guid = 12345L;
        long lastModified = 123452465;
        List<Member> members = new ArrayList<Member>();
        List<File> files = new ArrayList<File>();
        List<Message> messages = new ArrayList<Message>();

        Workspace workspace = new Workspace(name, description, status, key, guid, lastModified, members, files, messages);

        assertEquals(name, workspace.getName());
        assertEquals(description, workspace.getDescription());
        assertEquals(status, workspace.getStatus());
        assertEquals(key, workspace.getWorkspaceKey());
        assertEquals(guid, workspace.getGuid());
        assertEquals(lastModified, workspace.getLastModified());

        assertEquals(members, workspace.getMembers());
        assertNotSame(members, workspace.getMembers());
        assertEquals(files, workspace.getFiles());
        assertNotSame(files, workspace.getFiles());
        assertEquals(messages, workspace.getMessages());
        assertNotSame(messages, workspace.getMessages());
    }

    @Test
    public void testConstructorWithNullCollections() throws Exception {

        String name = "Reclaiming Anniera";
        String description = "Return to the Shining Isle";
        int status = 56;
        SecretKey key = new SecretKeySpec(new byte[16], "AES");
        long guid = 12345L;
        long lastModified = 123452465;
        List<Member> members = null;
        List<File> files = null;
        List<Message> messages = null;

        Workspace workspace = new Workspace(name, description, status, key, guid, lastModified, members, files, messages);

        assertEquals(name, workspace.getName());
        assertEquals(description, workspace.getDescription());
        assertEquals(status, workspace.getStatus());
        assertEquals(key, workspace.getWorkspaceKey());
        assertEquals(guid, workspace.getGuid());
        assertEquals(lastModified, workspace.getLastModified());

        assertNotNull(workspace.getFiles());
        assertNotNull(workspace.getMembers());
        assertNotNull(workspace.getMessages());
    }

    @Test
    public void testBasicAccessors() throws Exception {

        String name = "Reclaiming Anniera";
        String description = "Return to the Shining Isle";
        int status = 56;
        SecretKey key = new SecretKeySpec(new byte[16], "AES");
        long guid = 12345L;
        long lastModified = 123452465;

        Workspace workspace = new Workspace();

        assertNull ( workspace.getName()) ;
        workspace.setName(name);
        assertEquals(name, workspace.getName());
        workspace.setName(null);
        assertNull ( workspace.getName()) ;

        assertNull(workspace.getDescription());
        workspace.setDescription(description);
        assertEquals(description, workspace.getDescription());
        workspace.setDescription(null);
        assertNull(workspace.getDescription());

        assertEquals(0, workspace.getStatus());
        workspace.setStatus(status);
        assertEquals(status, workspace.getStatus());
        workspace.setStatus(0);
        assertEquals(0, workspace.getStatus());

        assertNull(workspace.getWorkspaceKey());
        workspace.setWorkspaceKey(key);
        assertEquals(key, workspace.getWorkspaceKey());
        workspace.setWorkspaceKey(null);
        assertNull(workspace.getWorkspaceKey());

        workspace.setGuid(guid);
        assertEquals(guid, workspace.getGuid());

        assertEquals(0, workspace.getLastModified());
        workspace.setLastModified(lastModified);
        assertEquals(lastModified, workspace.getLastModified());
        workspace.setLastModified(0);
        assertEquals(0, workspace.getLastModified());
    }

    @Test
    public void testCollectionAccessors() throws Exception {

        String name = "Reclaiming Anniera";
        int status = 56;
        long guid = 12345L;
        String userID = "NeahWingfeather";
        String authToken = "NeahAuthToken";
        long date = 12346723435L;
        String inviterID = "JannerWingfeather" ;
        String mimeType = "text/book";
        String entryID = "A Brief History of Anniera";
        String parentID = "None" ;
        boolean isFolder = false;
        String lockOwner = "" ;
        List<FileVersion> versions = new ArrayList<FileVersion>();
        long messageID = 45678L;
        long seqNum = 43562;
        String message = "On the Dark Sea of Darkness" ;
        byte[] digSig = new byte[128] ;

        List<Member> members = new ArrayList<Member>();
        List<File> files = new ArrayList<File>();
        List<Message> messages = new ArrayList<Message>();

        members.add(new Member(userID, status, date, inviterID, authToken)) ;

        files.add(new File(name, mimeType, entryID,guid, parentID, isFolder, status, lockOwner, versions)) ;

        messages.add(new Message(userID, messageID, guid, seqNum, date, message, mimeType, digSig));

        Workspace workspace = new Workspace();

        assertNotNull(workspace.getFiles());
        workspace.setFiles(files);
        assertEquals(files, workspace.getFiles());
        assertNotSame(files, workspace.getFiles());
        workspace.setFiles(null);
        assertNotNull(workspace.getFiles());

        assertNotNull(workspace.getMembers());
        workspace.setMembers(members);
        assertEquals(members, workspace.getMembers());
        assertNotSame(members, workspace.getMembers());
        workspace.setMembers(null);
        assertNotNull(workspace.getMembers());

        assertNotNull(workspace.getMessages());
        workspace.setMessages(messages);
        assertEquals(messages, workspace.getMessages());
        assertNotSame(messages, workspace.getMessages());
        workspace.setMessages(null);
        assertNotNull(workspace.getMessages());
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {

        String name = "Reclaiming Anniera";
        String description = "Return to the Shining Isle";
        int status = 56;
        SecretKey key = new SecretKeySpec(new byte[16], "AES");
        long guid = 12345L;
        long lastModified = 123452465;
        List<Member> members = new ArrayList<Member>();
        List<File> files = new ArrayList<File>();
        List<Message> messages = new ArrayList<Message>();

        Workspace workspace1 = new Workspace(name, description, status, key, guid, lastModified, members, files, messages);
        Workspace workspace2 = new Workspace(name, description, status, key, guid, lastModified, members, files, messages);

        assertEquals(workspace1, workspace1);
        assertEquals(workspace2, workspace2);
        assertEquals(workspace1, workspace2);
        assertEquals(workspace2, workspace1);

        assertEquals(workspace1.hashCode(), workspace2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {

        Workspace workspace1 = new Workspace();
        Workspace workspace2 = new Workspace();

        assertEquals(workspace1, workspace1);
        assertEquals(workspace2, workspace2);
        assertEquals(workspace1, workspace2);
        assertEquals(workspace2, workspace1);

        assertEquals(workspace1.hashCode(), workspace2.hashCode());
    }


}
