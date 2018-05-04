package io.topiacoin.model.provider;

import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.File;
import io.topiacoin.model.FileChunk;
import io.topiacoin.model.FileTag;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.FileVersionReceipt;
import io.topiacoin.model.Member;
import io.topiacoin.model.MemberNode;
import io.topiacoin.model.Message;
import io.topiacoin.model.User;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.BadAuthTokenException;
import io.topiacoin.model.exceptions.FileAlreadyExistsException;
import io.topiacoin.model.exceptions.FileChunkAlreadyExistsException;
import io.topiacoin.model.exceptions.FileTagAlreadyExistsException;
import io.topiacoin.model.exceptions.FileVersionAlreadyExistsException;
import io.topiacoin.model.exceptions.MemberAlreadyExistsException;
import io.topiacoin.model.exceptions.MessageAlreadyExistsException;
import io.topiacoin.model.exceptions.NoSuchFileChunkException;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchFileTagException;
import io.topiacoin.model.exceptions.NoSuchFileVersionException;
import io.topiacoin.model.exceptions.NoSuchFileVersionReceiptException;
import io.topiacoin.model.exceptions.NoSuchMemberException;
import io.topiacoin.model.exceptions.NoSuchMessageException;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import io.topiacoin.model.exceptions.UserAlreadyExistsException;
import io.topiacoin.model.exceptions.WorkspaceAlreadyExistsException;

import java.util.List;

public interface DataModelProvider {

    // -------- Workspace Accessor Methods --------

    List<Workspace> getWorkspaces();

    List<Workspace> getWorkspacesWithStatus(int workspaceStatus);

    Workspace getWorkspace(String workspaceID)
            throws NoSuchWorkspaceException;

    void addWorkspace(Workspace workspace)
            throws WorkspaceAlreadyExistsException;

    void updateWorkspace(Workspace workspace)
            throws NoSuchWorkspaceException;

    void removeWorkspace(String workspaceID)
            throws NoSuchWorkspaceException;



    // -------- Member Accessor Methods --------

    List<Member> getMembersInWorkspace(String workspaceID)
            throws NoSuchWorkspaceException;

    Member getMemberInWorkspace(String workspaceID, String userID)
            throws NoSuchWorkspaceException, NoSuchMemberException;

    void addMemberToWorkspace(String workspaceID, Member member)
            throws NoSuchWorkspaceException, MemberAlreadyExistsException;

    void updateMemberInWorkspace(String workspaceID, Member member)
            throws NoSuchWorkspaceException, NoSuchMemberException;

    void removeMemberFromWorkspace(String workspaceID, Member member)
            throws NoSuchWorkspaceException, NoSuchMemberException;



    // -------- Message Accessor Methods --------

    List<Message> getMessagesInWorkspace(String workspaceID)
            throws NoSuchWorkspaceException;

    Message getMessage(String messageID)
            throws NoSuchMessageException;

    void addMessageToWorkspace(String workspaceID, Message message)
            throws NoSuchWorkspaceException, MessageAlreadyExistsException;

    void updateMessageInWorkspace(String workspaceID, Message message)
            throws NoSuchWorkspaceException, NoSuchMessageException;

    void removeMessageFromWorkspace(String workspaceID, Message message)
            throws NoSuchWorkspaceException, NoSuchMessageException;



    // -------- File Accessor Methods --------

    List<File> getFilesInWorkspace(String workspaceID)
            throws NoSuchWorkspaceException;

    List<File> getFilesInWorkspace(String workspaceID, String parentID)
            throws NoSuchWorkspaceException;

    File getFile(String fileID)
            throws NoSuchFileException;

    void addFileToWorkspace(String workspaceID, File file)
            throws NoSuchWorkspaceException, FileAlreadyExistsException;

    void updateFileInWorkspace(String workspaceID, File file)
            throws NoSuchWorkspaceException, NoSuchFileException;

    void removeFileFromWorkspace(String workspaceID, String fileID)
            throws NoSuchWorkspaceException, NoSuchFileException;

    void removeFileFromWorkspace(String workspaceID, File file)
            throws NoSuchWorkspaceException, NoSuchFileException;



    // -------- File Version Accessor Methods --------

    List<String> getAvailableVersionsOfFile(String fileID)
            throws NoSuchFileException;

    List<FileVersion> getFileVersionsForFile(String fileID)
            throws NoSuchFileException;

    FileVersion getFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException;

    void addFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, FileVersionAlreadyExistsException;

    void updateFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, NoSuchFileVersionException;

    void removeFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException;

    void removeFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, NoSuchFileVersionException;



    // -------- File Version Receipt Accessor Methods --------

    List<FileVersionReceipt> getFileVersionReceipts(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException;

    void addFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException;

    void updateFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException;

    void removeFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException;



    // -------- File Chunk Accessor Methods --------

    List<FileChunk> getChunksForFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException;

    void addChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, FileChunkAlreadyExistsException;

    void updateChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException;

    void removeChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException;



    // -------- File Tag Accessor Methods --------

    List<FileTag> getTagsForFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException;

    void addTagForFile(String fileID, String versionID, FileTag tag)
            throws NoSuchFileException, NoSuchFileVersionException, FileTagAlreadyExistsException;

    void removeTagForFile(String fileID, String versionID, FileTag tag)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileTagException;



    // -------- User Accessor Methods --------

    List<User> getUsers();

    User getUserByID(String userID)
            throws NoSuchUserException;

    User getUserByEmail(String email)
            throws NoSuchUserException;

    void addUser(User User)
            throws UserAlreadyExistsException;

    void updateUser(User user)
            throws NoSuchUserException;

    void removeUser(User user)
            throws NoSuchUserException;

    void removeUser(String userID)
            throws NoSuchUserException;

    CurrentUser getCurrentUser() throws NoSuchUserException;

    void setCurrentUser(CurrentUser user);

    void removeCurrentUser();

    void addMemberNode(String containerID, MemberNode memberNode);

    void removeMemberNode(String containerID, MemberNode memberNode);

    List<MemberNode> getMemberNodesForContainer(String containerID);

    Workspace getWorkspaceByMyAuthToken(String authToken) throws BadAuthTokenException;

    boolean hasChunkInWorkspace(String chunkID, String workspaceGuid);

    List<String> hasChunksInWorkspace(List<String> chunkIDs, String workspaceGuid);
}
