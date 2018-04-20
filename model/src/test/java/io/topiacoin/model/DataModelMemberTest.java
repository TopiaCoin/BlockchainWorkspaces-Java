package io.topiacoin.model;

import io.topiacoin.model.exceptions.MemberAlreadyExistsException;
import io.topiacoin.model.exceptions.NoSuchMemberException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class DataModelMemberTest {

    // -------- Member Tests --------

    @Test
    public void testMemberCRUD() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        dataModel.addWorkspace(workspace);

        members = dataModel.getMembersInWorkspace(workspaceID);
        assertNotNull(members);
        assertEquals(0, members.size());

        dataModel.addMemberToWorkspace(workspaceID, member);

        members = dataModel.getMembersInWorkspace(workspaceID);
        assertNotNull(members);
        assertEquals(1, members.size());
        assertEquals(member, members.get(0));

        Member fetchedMember = dataModel.getMemberInWorkspace(workspaceID, userID);
        assertNotNull(fetchedMember);
        assertEquals(member, fetchedMember);

        member.setStatus(2);
        dataModel.updateMemberInWorkspace(workspaceID, member);

        members = dataModel.getMembersInWorkspace(workspaceID);
        assertNotNull(members);
        assertEquals(1, members.size());
        assertEquals(member, members.get(0));

        dataModel.removeMemberFromWorkspace(workspaceID, member);

        members = dataModel.getMembersInWorkspace(workspaceID);
        assertNotNull(members);
        assertEquals(0, members.size());
    }

    @Test
    public void testChangingAddedMemberDoesNotChangeModel() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        dataModel.addWorkspace(workspace);

        dataModel.addMemberToWorkspace(workspaceID, member);

        member.setStatus(12);

        members = dataModel.getMembersInWorkspace(workspaceID);
        assertNotNull(members);
        assertEquals(1, members.size());
        assertNotEquals(member.getStatus(), members.get(0).getStatus());

    }

    @Test
    public void testChangingFetchedMemberDoesNotChangeModel() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        dataModel.addWorkspace(workspace);

        dataModel.addMemberToWorkspace(workspaceID, member);

        Member fetchedMember = dataModel.getMemberInWorkspace(workspaceID, userID);
        assertNotNull(fetchedMember);

        fetchedMember.setStatus(12);

        Member fetchedMember2 = dataModel.getMemberInWorkspace(workspaceID, userID);
        assertNotNull(fetchedMember2);
        assertNotEquals(fetchedMember.getStatus(), fetchedMember2.getStatus());
        assertEquals(member, fetchedMember2);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testFetchMembersFromNonExistentWorkspace() throws Exception {
        String fakeID = UUID.randomUUID().toString();

        DataModel dataModel = new DataModel();
        List<Member> members;

        // Expecting a NoSuchWorkspaceException
        members = dataModel.getMembersInWorkspace(fakeID);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testFetchMemberFromNonExistentWorkspace() throws Exception {
        String fakeID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();

        DataModel dataModel = new DataModel();
        Member member;

        // Expecting a NoSuchWorkspaceException
        member = dataModel.getMemberInWorkspace(fakeID, userID);
    }

    @Test(expected = NoSuchMemberException.class)
    public void testFetchNonExistentMemberFromWorkspace() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);

        Member member;

        // Expecting a NoSuchMemberException
        member = dataModel.getMemberInWorkspace(workspaceID, userID);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testCreateMemberInNonExistentWorkspace() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        // Expecting a NoSuchWorkspaceException
        dataModel.addMemberToWorkspace(workspaceID, member);
    }

    @Test(expected = MemberAlreadyExistsException.class)
    public void testCreateDuplicateMemberInWorkspace() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        dataModel.addWorkspace(workspace);

        dataModel.addMemberToWorkspace(workspaceID, member);

        // Expecting a MemberAlreadyExistsException
        dataModel.addMemberToWorkspace(workspaceID, member);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testUpdateMemberInNonExistentWorkspace() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        // Expecting a NoSuchWorkspaceException
        dataModel.updateMemberInWorkspace(workspaceID, member);
    }

    @Test(expected = NoSuchMemberException.class)
    public void testUpdateNonExistentMemberInWorkspace() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        dataModel.addWorkspace(workspace);

        // Expecting a NoSuchMemberException
        dataModel.updateMemberInWorkspace(workspaceID, member);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testRemoveMemberFromNonExistentWorkspace() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        // Expecting a NoSuchWorkspaceException
        dataModel.removeMemberFromWorkspace(workspaceID, member);
    }

    @Test(expected = NoSuchMemberException.class)
    public void testRemoveNonExistentMemberFromWorkspace() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String userID = UUID.randomUUID().toString();
        String inviterID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Member member = new Member();
        member.setUserID(userID);
        member.setInviterID(inviterID);
        member.setStatus(1);
        member.setInviteDate(System.currentTimeMillis());

        DataModel dataModel = new DataModel();
        List<Member> members;

        dataModel.addWorkspace(workspace);

        // Expecting a NoSuchMemberException
        dataModel.removeMemberFromWorkspace(workspaceID, member);
    }

}
