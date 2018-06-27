package io.topiacoin.workspace.blockchain;

import io.topiacoin.eosrpcadapter.exceptions.ChainException;
import io.topiacoin.model.File;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.Member;
import io.topiacoin.model.Message;
import io.topiacoin.workspace.blockchain.eos.EOSAdapter;
import io.topiacoin.workspace.blockchain.eos.Files;
import io.topiacoin.workspace.blockchain.eos.Members;
import io.topiacoin.workspace.blockchain.eos.Messages;
import io.topiacoin.workspace.blockchain.eos.WorkspaceInfo;
import io.topiacoin.workspace.blockchain.exceptions.BlockchainException;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

@Ignore
public class EOSAdapterTest {

    @Test
    public void testInitializeWorkspace() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String name = "Test Workspace";
        String description = "This is a Test Workspace";
        String newDescription = "This is a Beta Workspace";
        String ownerKey = "fakeKey";

        WorkspaceInfo workspaceInfo = null;

        try {
            // Initialize the new workspace
            adapter.initializeWorkspace(guid, owner, name, description, ownerKey);

            // Verify that the workspace info matches what was passed in.
            workspaceInfo = adapter.getWorkspaceInfo(guid);
            assertNotNull(workspaceInfo) ;
            assertEquals(guid, workspaceInfo.getGuid());
            assertEquals(name, workspaceInfo.getWorkspaceName());
            assertEquals(description, workspaceInfo.getWorkspaceDescription());
            assertEquals(owner, workspaceInfo.getOwner());

            // Set the workspace description.
            adapter.setWorkspaceDescription(guid, owner, newDescription);

            // Verify that the description is updated, but all other info remains the same.
            workspaceInfo = adapter.getWorkspaceInfo(guid);
            assertNotNull(workspaceInfo) ;
            assertEquals(guid, workspaceInfo.getGuid());
            assertEquals(name, workspaceInfo.getWorkspaceName());
            assertEquals(newDescription, workspaceInfo.getWorkspaceDescription());
            assertEquals(owner, workspaceInfo.getOwner());

        } finally {
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testAcceptInvitation() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        System.out.println("GUID: " + guid);

        Members members = null ;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Verify that the workspace has only one member
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertNotNull(members);
            assertNotNull(members.getMembers());
            assertEquals(1, members.getMembers().size());

            // Add the New Owner
            Thread.sleep(100);
            adapter.addMember(guid, owner, otherMember, "fakeKey");

            // Verify that the workspace has two members, and that the invitee is pending
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertEquals(2, members.getMembers().size());
            assertEquals(1, members.getMembers().get(0).getStatus());
            assertEquals(0, members.getMembers().get(1).getStatus());

            // Have new owner accept invitation
            Thread.sleep(100);
            adapter.acceptInvitation(guid, otherMember);

            // Verify that the workspace has two members, and that both are active
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertEquals(2, members.getMembers().size());
            assertEquals(1, members.getMembers().get(0).getStatus());
            assertEquals(1, members.getMembers().get(1).getStatus());

            // Remove the second member
            Thread.sleep(100);
            adapter.removeMember(guid, owner, otherMember);

            // Verify that the workspace has only one member.
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertNotNull(members);
            assertNotNull(members.getMembers());
            assertEquals(1, members.getMembers().size());
        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testDeclineInvitation() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        System.out.println("GUID: " + guid);

        Members members = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Verify that the workspace has only one member
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertNotNull(members);
            assertNotNull(members.getMembers());
            assertEquals(1, members.getMembers().size());

            // Add the New Owner
            Thread.sleep(100);
            adapter.addMember(guid, owner, otherMember, "fakeKey");

            // Verify that the workspace has two members, and that the invitee is pending
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertEquals(2, members.getMembers().size());
            assertEquals(1, members.getMembers().get(0).getStatus());
            assertEquals(0, members.getMembers().get(1).getStatus());

            // Have new owner accept invitation
            Thread.sleep(100);
            adapter.declineInvitation(guid, otherMember);

            // Verify that the workspace has only one member.
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertNotNull(members);
            assertNotNull(members.getMembers());
            assertEquals(1, members.getMembers().size());
        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testOwnershipTransfer() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String newOwner = "sampledb";
        String currentOwner = owner;

        System.out.println("GUID: " + guid);

        WorkspaceInfo workspaceInfo = null ;
        Members members = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Verify that the workspace Info show the original owner
            Thread.sleep(100);
            workspaceInfo = adapter.getWorkspaceInfo(guid);
            assertEquals(owner, workspaceInfo.getOwner());
            currentOwner = workspaceInfo.getOwner();

            // Verify that the workspace has only one member
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertNotNull(members);
            assertNotNull(members.getMembers());
            assertEquals(1, members.getMembers().size());

            // Add the New Owner
            Thread.sleep(100);
            adapter.addMember(guid, owner, newOwner, "fakeKey");

            // Verify that the workspace has two members, and that the invitee is pending
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertEquals(2, members.getMembers().size());
            assertEquals(1, members.getMembers().get(0).getStatus());
            assertEquals(0, members.getMembers().get(1).getStatus());

            // Have new owner accept invitation
            Thread.sleep(100);
            adapter.acceptInvitation(guid, newOwner);

            // Verify that the workspace has two members, and that both are active
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertEquals(2, members.getMembers().size());
            assertEquals(1, members.getMembers().get(0).getStatus());
            assertEquals(1, members.getMembers().get(1).getStatus());

            // Offer ownership to the other user, rescind the invitation, and then try to accept
            Thread.sleep(100);
            adapter.offerOwnership(guid, owner, newOwner);

            // Verify that the workspace Info show the ownership offer
            Thread.sleep(100);
            workspaceInfo = adapter.getWorkspaceInfo(guid);
            assertEquals(owner, workspaceInfo.getOwner());
            assertEquals(newOwner, workspaceInfo.getNewOwner());
            currentOwner = workspaceInfo.getOwner();

            Thread.sleep(100);
            adapter.rescindOwnership(guid, owner);
            try {
                Thread.sleep(100);
                adapter.acceptOwnership(guid, newOwner);
                fail("Accept Ownership should fail after the offer is rescinded.");
            } catch (BlockchainException e) {
                // NOOP - Expected an exception
            }

            // TODO - Verify that the workspace Info show the original owner
            Thread.sleep(100);
            workspaceInfo = adapter.getWorkspaceInfo(guid);
            assertEquals(owner, workspaceInfo.getOwner());
            assertEquals("", workspaceInfo.getNewOwner());
            currentOwner = workspaceInfo.getOwner();

            // Offer ownership to the other user and then accept it.
            Thread.sleep(100);
            adapter.offerOwnership(guid, owner, newOwner);
            Thread.sleep(100);
            adapter.acceptOwnership(guid, newOwner);

            // TODO - Verify that the workspace Info show the new owner
            Thread.sleep(100);
            workspaceInfo = adapter.getWorkspaceInfo(guid);
            assertEquals(newOwner, workspaceInfo.getOwner());
            assertEquals("", workspaceInfo.getNewOwner());
            currentOwner = workspaceInfo.getOwner();

            // Give ownership back to the original owner.
            Thread.sleep(100);
            adapter.offerOwnership(guid, newOwner, owner);
            Thread.sleep(100);
            adapter.acceptOwnership(guid, owner);

            // TODO - Verify that the workspace Info show the original owner
            Thread.sleep(100);
            workspaceInfo = adapter.getWorkspaceInfo(guid);
            assertEquals(owner, workspaceInfo.getOwner());
            assertEquals("", workspaceInfo.getNewOwner());
            currentOwner = workspaceInfo.getOwner();
        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, currentOwner);
        }
    }

    @Test
    public void testLockUnlockMember() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        System.out.println("GUID: " + guid);

        Members members = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Add the New Member
            Thread.sleep(100);
            adapter.addMember(guid, owner, otherMember, "fakeKey");

            // Have new owner accept invitation
            Thread.sleep(100);
            adapter.acceptInvitation(guid, otherMember);

            //Lock the new Member
            Thread.sleep(100);
            adapter.lockMember(guid, owner, otherMember);

            // Remove the second member
            Thread.sleep(100);
            try {
                adapter.removeMember(guid, owner, otherMember);
                fail("Should not have been able to remove a locked member");
            } catch (BlockchainException e) {
                // NOOP - Expected the remove to fail.
            }

            // Verify that the workspace has still has two members, since locked members cannot be removed
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertNotNull(members);
            assertNotNull(members.getMembers());
            assertEquals(2, members.getMembers().size());

            // Unlock the new Member
            Thread.sleep(100);
            adapter.unlockMember(guid, owner, otherMember);

            // Remove the second member
            Thread.sleep(100);
            adapter.removeMember(guid, owner, otherMember);

            // Verify that the workspace has now has one member again.
            Thread.sleep(100);
            members = adapter.getMembers(guid);
            assertNotNull(members);
            assertNotNull(members.getMembers());
            assertEquals(1, members.getMembers().size());
        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testAddRemoveFiles() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        String name = "Example.jpg";
        String mimeType = "application/octet-stream";
        String parentID = "0x00000000000000000000000000000000";
        String fileID = "0x0123456789abcdef0000000000000000";
        String versionID = "0x0123456789abcdef0000000000000001";
        String noVersionID = "0x00000000000000000000000000000000";
        List<String> ancestorIDs = new ArrayList<>();
        String metadata = "Fake Metadata";

        FileVersion version = new FileVersion(fileID,
                versionID,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version));

        System.out.println("GUID: " + guid);

        Files files = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Verify that there are no files in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertNotNull(files.getFiles());
            assertEquals(0, files.getFiles().size());

            // Add a new File
            Thread.sleep(100);
            adapter.addFile(guid, owner, file);

            // Verify that there is one file in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertEquals(1, files.getFiles().size());

            // Remove the file
            Thread.sleep(100);
            adapter.removeFile(guid, owner, fileID, noVersionID);

            // Verify that there are no files in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertEquals(0, files.getFiles().size());

        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testAddRemoveFileVersions() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        String name = "Example.jpg";
        String mimeType = "application/octet-stream";

        String parentID = "0x00000000000000000000000000000000";
        String fileID = "0x0123456789abcdef0000000000000000";
        String versionID1 = "0x0123456789abcdef0000000000000001";
        String versionID2 = "0x0123456789abcdef0000000000000002";
        String versionID3 = "0x0123456789abcdef0000000000000003";
        String noVersionID = "0x00000000000000000000000000000000";
        List<String> ancestorIDs = new ArrayList<>();
        String metadata = "Fake Metadata";

        FileVersion version1 = new FileVersion(fileID,
                versionID1,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file1 = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version1));

        FileVersion version2 = new FileVersion(fileID,
                versionID2,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file2 = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version2));

        FileVersion version3 = new FileVersion(fileID,
                versionID3,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file3 = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version3));

        System.out.println("GUID: " + guid);

        Files files = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Verify that there are no files in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertEquals(0, files.getFiles().size());

            // Add a new File
            Thread.sleep(100);
            adapter.addFile(guid, owner, file1);

            // Verify that there is one file in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertEquals(1, files.getFiles().size());

            // Add a new File Version
            Thread.sleep(100);
            adapter.addFile(guid, owner, file2);

            // Verify that there are no files in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertEquals(2, files.getFiles().size());

            // Remove the file
            Thread.sleep(100);
            adapter.removeFile(guid, owner, fileID, versionID1);

            // Verify that there are no files in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertEquals(1, files.getFiles().size());

            // Add a new File Version
            Thread.sleep(100);
            adapter.addFile(guid, owner, file3);

            // Verify that there are no files in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertEquals(2, files.getFiles().size());

            // Get a specific file version
            Thread.sleep(100);
            File file = adapter.getFile(guid, owner, fileID, versionID3);
            assertNotNull ( file ) ;

            // Remove the file
            Thread.sleep(100);
            adapter.removeFile(guid, owner, fileID, noVersionID);

            // Verify that there are no files in the workspace
            Thread.sleep(100);
            files = adapter.getFiles(guid);
            assertNotNull(files);
            assertEquals(0, files.getFiles().size());

        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testRemoveFileVersionUpdatesParentVersions() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        String name = "Example.jpg";
        String mimeType = "application/octet-stream";

        String parentID = "0x00000000000000000000000000000000";
        String fileID = "0x0123456789abcdef0000000000000000";
        String versionID1 = "0x0123456789abcdef0000000000000001";
        String versionID2 = "0x0123456789abcdef0000000000000002";
        String versionID3 = "0x0123456789abcdef0000000000000003";
        String versionID4 = "0x0123456789abcdef0000000000000004";
        String noVersionID = "0x00000000000000000000000000000000";
        List<String> ancestorIDs = new ArrayList<>();
        String metadata = "Fake Metadata";

        FileVersion version1 = new FileVersion(fileID,
                versionID1,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file1 = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version1));

        FileVersion version2 = new FileVersion(fileID,
                versionID2,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file2 = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version2));

        FileVersion version3 = new FileVersion(fileID,
                versionID3,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file3 = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version3));

        FileVersion version4 = new FileVersion(fileID,
                versionID4,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file4 = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version4));

        System.out.println("GUID: " + guid);

        Files files = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Add a new File
            Thread.sleep(100);
            adapter.addFile(guid, owner, file1);

            Thread.sleep(100);
            files = adapter.getFiles(guid);

            // Add a new File Version
            Thread.sleep(100);
            adapter.addFile(guid, owner, file2);

            Thread.sleep(100);
            files = adapter.getFiles(guid);

            // Add a new File Version
            Thread.sleep(100);
            adapter.addFile(guid, owner, file3);

            Thread.sleep(100);
            files = adapter.getFiles(guid);

            // Add a new File Version
            Thread.sleep(100);
            adapter.addFile(guid, owner, file4);

            Thread.sleep(100);
            files = adapter.getFiles(guid);

            // Remove the file
            Thread.sleep(100);
            adapter.removeFile(guid, owner, fileID, versionID1);

            Thread.sleep(100);
            files = adapter.getFiles(guid);

            // Remove the file
            Thread.sleep(100);
            adapter.removeFile(guid, owner, fileID, versionID3);

            Thread.sleep(100);
            files = adapter.getFiles(guid);


            // TODO - Add some assertions to this test case.
        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testMessages() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        String parentID = "0x00000000000000000000000000000000";
        String fileID = "0x0123456789abcdef0000000000000000";
        String versionID1 = "0x0123456789abcdef0000000000000001";
        String versionID2 = "0x0123456789abcdef0000000000000002";
        String versionID3 = "0x0123456789abcdef0000000000000003";
        String noVersionID = "0x00000000000000000000000000000000";
        List<String> ancestorIDs = new ArrayList<>();
        String metadata = "Fake Metadata";

        System.out.println("GUID: " + guid);

        Messages messages =null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Get Messages and verify there are none
            Thread.sleep(100);
            messages = adapter.getMessages(guid);
            assertNotNull(messages);
            assertEquals ( 0, messages.getMessages().size());

            // Add a Message
            Thread.sleep(100);
            String message = "Four Square and Seven Circles Ago";
            String mimeType = "text/plain";
            adapter.addMessage(guid, owner, message, mimeType);

            // Get Messages and verify there is now one.
            Thread.sleep(100);
            messages = adapter.getMessages(guid);
            assertNotNull(messages);
            assertEquals ( 1, messages.getMessages().size());

            long msgID = messages.getMessages().get(0).getGuid();

            // Acknowledge the Message
            Thread.sleep(100);
            adapter.acknowledgeMessage(guid, owner, msgID);
        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testFileLocking() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        String name = "Example.jpg";
        String mimeType = "application/octet-stream";

        String parentID = "0x00000000000000000000000000000000";
        String fileID = "0x0123456789abcdef0000000000000000";
        String versionID = "0x0123456789abcdef0000000000000001";
        String noVersionID = "0x00000000000000000000000000000000";
        List<String> ancestorIDs = new ArrayList<>();
        String metadata = "Fake Metadata";

        FileVersion version = new FileVersion(fileID,
                versionID,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version));

        System.out.println("GUID: " + guid);

        List<File> files = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Add a new File
            Thread.sleep(100);
            adapter.addFile(guid, owner, file);

            // Lock the File
            Thread.sleep(100);
            adapter.lockFile(guid,owner, fileID);

            // Attempt to Remove the file
            try {
                Thread.sleep(100);
                adapter.removeFile(guid, owner, fileID, noVersionID);
                fail ( "Remove File should have failed because the file was locked");
            } catch ( BlockchainException e ){
                // NOOP - Expected Exception
            }

            // Attempt to Remove the file version
            try {
                Thread.sleep(100);
                adapter.removeFile(guid, owner, fileID, versionID);
                fail ( "Remove File Version should have failed because the file was locked");
            } catch ( BlockchainException e ){
                // NOOP - Expected Exception
            }

            // Unlock the File
            Thread.sleep(100);
            adapter.unlockFile(guid,owner, fileID);

            // Attempt to Remove the file
            Thread.sleep(100);
            adapter.removeFile(guid, owner, fileID, noVersionID);
        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testVersionLocking() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        String name = "Example.jpg";
        String mimeType = "application/octet-stream";

        String parentID = "0x00000000000000000000000000000000";
        String fileID = "0x0123456789abcdef0000000000000000";
        String versionID = "0x0123456789abcdef0000000000000001";
        String noVersionID = "0x00000000000000000000000000000000";
        List<String> ancestorIDs = new ArrayList<>();
        String metadata = "Fake Metadata";

        FileVersion version = new FileVersion(fileID,
                versionID,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version));

        System.out.println("GUID: " + guid);

        List<File> files = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Add a new File
            Thread.sleep(100);
            adapter.addFile(guid, owner, file);

            // Lock the File
            Thread.sleep(100);
            adapter.lockFileVersion(guid,owner, fileID, versionID);

            // Attempt to Remove the file version
            try {
                Thread.sleep(100);
                adapter.removeFile(guid, owner, fileID, versionID);
                fail ( "Remove File Version should have failed because the file version was locked");
            } catch ( BlockchainException e ){
                // NOOP - Expected Exception
            }

            // Attempt to Remove the file
            try {
                Thread.sleep(100);
                adapter.removeFile(guid, owner, fileID, noVersionID);
                fail ( "Remove File should have failed because a file version was locked");
            } catch ( BlockchainException e ){
                // NOOP - Expected Exception
            }

            // Unlock the File
            Thread.sleep(100);
            adapter.unlockFileVersion(guid,owner, fileID, versionID);

            // Attempt to Remove the file
            Thread.sleep(100);
            adapter.removeFile(guid, owner, fileID, noVersionID);
        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }

    @Test
    public void testFileTags() throws Exception {
        EOSAdapter adapter = new EOSAdapter("http://localhost:8889/", "http://localhost:8899/");
        adapter.initialize();

        long guid = 0;
        while (guid <= 0) {
            // We want the GUID to be positive for ease of management later.
            guid = new Random().nextLong();
        }
        String owner = "inita";
        String otherMember = "sampledb";

        String name = "Example.jpg";
        String mimeType = "application/octet-stream";

        String parentID = "0x00000000000000000000000000000000";
        String fileID = "0x0123456789abcdef0000000000000000";
        String versionID = "0x0123456789abcdef0000000000000001";
        String noVersionID = "0x00000000000000000000000000000000";
        List<String> ancestorIDs = new ArrayList<>();
        String metadata = "Fake Metadata";

        FileVersion version = new FileVersion(fileID,
                versionID,
                owner,
                1,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "foo",
                "ACTIVE",
                null,
                null,
                null,
                null,
                ancestorIDs);
        File file = new File(name, mimeType, fileID, guid, parentID, false, 1, null, Arrays.asList(version));

        String tagValue = "OogaBooga";

        System.out.println("GUID: " + guid);

        List<File> files = null;

        // Create the new workspace
        adapter.initializeWorkspace(guid, owner, "Test Workspace", "Test Workspace", "fakeKey");

        try {
            // Add the New Owner
            Thread.sleep(100);
            adapter.addMember(guid, owner, otherMember, "fakeKey");

            // Have new owner accept invitation
            Thread.sleep(100);
            adapter.acceptInvitation(guid, otherMember);

            // Add a new File
            Thread.sleep(100);
            adapter.addFile(guid, owner, file);

            // Add Tag to File
            Thread.sleep(100);
            adapter.addFileTag(guid, owner, fileID, versionID, tagValue, true);

            // Fetch the File and verify it has the tag
            // TODO - Implement this at some point

            // Remove Tag from File
            Thread.sleep(100);
            adapter.removeFileTag(guid, owner, fileID, versionID, tagValue, true);

            // Fetch the File and verify it no longer as the tag
            // TODO - Implement this at some point

            // Add Private Tag to File
            Thread.sleep(100);
            adapter.addFileTag(guid, owner, fileID, versionID, tagValue, false);

            // Fetch the File and verify it has the tag
            // TODO - Implement this at some point

            // Attempt to Remove Private Tag from File using other Member
            adapter.removeFileTag(guid, owner, fileID, versionID, tagValue, false);
            try {
                Thread.sleep(100);
                adapter.removeFileTag(guid, otherMember, fileID, versionID, tagValue, false);
                fail ( "Other Member should not be able to remove non-public tag");
            } catch ( BlockchainException e) {
                // NOOP - Expected Path
            }


        } finally {
            Thread.sleep(100);
            adapter.destroy(guid, owner);
        }
    }
}
