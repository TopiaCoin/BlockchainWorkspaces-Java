package io.topiacoin.model;

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
import io.topiacoin.model.provider.DataModelProvider;
import io.topiacoin.model.provider.MemoryDataModelProvider;

import java.util.List;

public class DataModel {

    private static DataModel __instance;

    private DataModelProvider _provider;

    private DataModel() {
        // TODO - Replace this with code that loads the provider based on some configuration
        _provider = new MemoryDataModelProvider() ;
    }

    public static synchronized DataModel getInstance() {
        if (__instance != null) {
            __instance = new DataModel();
        }
        return __instance;
    }


    // -------- Workspace Accessor Methods --------

    public List<Workspace> getWorkspaces() {
        return null;
    }

    public List<Workspace> getWorkspacesWithStatus(int workspaceStatus) {
        return null;
    }

    public Workspace getWorkspace(String workspaceID)
            throws NoSuchWorkspaceException {
        return null;
    }

    public void addWorkspace(Workspace workspace)
            throws WorkspaceAlreadyExistsException {

    }

    public void updateWorkspace(Workspace workspace)
            throws NoSuchWorkspaceException {

    }

    public void removeWorkspace(String workspaceID)
            throws NoSuchWorkspaceException {

    }


    // -------- Member Accessor Methods --------

    public List<Member> getMembersInWorkspace(String workspaceID)
            throws NoSuchWorkspaceException {
        return null;
    }

    public Member getMemberInWorkspace(String workspaceID, String userID)
            throws NoSuchWorkspaceException, NoSuchMemberException {
        return null;
    }

    public void addMemberToWorkspace(String workspaceID, Member member)
            throws NoSuchWorkspaceException, MemberAlreadyExistsException {

    }

    public void updateMemberInWorkspace(String workspaceID, Member member)
            throws NoSuchWorkspaceException, NoSuchMemberException {

    }

    public void removeMemberFromWorkspace(String workspaceID, Member member)
            throws NoSuchWorkspaceException, NoSuchMemberException {

    }


    // -------- Message Accessor Methods --------

    public List<Message> getMessagesInWorkspace(String workspaceID)
            throws NoSuchWorkspaceException {
        return null;
    }

    public Message getMessage(String messageID)
            throws NoSuchMessageException {
        return null;
    }

    public void addMessageToWorkspace(String workspaceID, Message message)
            throws NoSuchWorkspaceException, MessageAlreadyExistsException {

    }

    public void updateMessageInWorkspace(String workspaceID, Message message)
            throws NoSuchWorkspaceException, NoSuchMessageException {

    }

    public void removeMessageFromWorkspace(String workspaceID, Message message)
            throws NoSuchWorkspaceException, NoSuchMessageException {

    }


    // -------- File Accessor Methods --------

    public List<File> getFilesInWorkspace(String workspaceID)
            throws NoSuchWorkspaceException {
        return null;
    }

    public List<File> getFilesInWorkspace(String workspaceID, String parentID)
            throws NoSuchWorkspaceException {
        return null;
    }

    public File getFile(String fileID)
            throws NoSuchFileException {
        return null;
    }

    public void addFileToWorkspace(String workspaceID, File file)
            throws NoSuchWorkspaceException, FileAlreadyExistsException {

    }

    public void updateFileInWorkspace(String workspaceID, File file)
            throws NoSuchWorkspaceException, NoSuchFileException {

    }

    public void removeFileFromWorkspace(String workspaceID, String fileID)
            throws NoSuchWorkspaceException, NoSuchFileException {

    }

    public void removeFileFromWorkspace(String workspaceID, File file)
            throws NoSuchWorkspaceException, NoSuchFileException {

    }


    // -------- File Version Accessor Methods --------

    public List<String> getAvailableVersionsOfFile(String fileID)
            throws NoSuchFileException {
        return null;
    }

    public List<FileVersion> getFileVersionsForFile(String fileID)
            throws NoSuchFileException {
        return null;
    }

    public FileVersion getFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        return null;
    }

    public void addFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, FileVersionAlreadyExistsException {

    }

    public void updateFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, NoSuchFileVersionException {

    }

    public void removeFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {

    }

    public void removeFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, NoSuchFileVersionException {

    }


    // -------- File Version Receipt Accessor Methods --------

    public List<FileVersionReceipt> getFileVersionReceipts(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        return null;
    }

    public void addFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException {

    }

    public void updateFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {

    }

    public void removeFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {

    }


    // -------- File Chunk Accessor Methods --------

    public List<FileChunk> getChunksForFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        return null;
    }

    public void addChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, FileChunkAlreadyExistsException {

    }

    public void updateChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {

    }

    public void removeChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {

    }


    // -------- File Tag Accessor Methods --------

    public List<FileTag> getTagsForFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        return null;
    }

    public void addTagForFile(String fileID, String versionID, FileTag tag)
            throws NoSuchFileException, NoSuchFileVersionException, FileTagAlreadyExistsException {

    }

    public void updateTagForFile(String fileID, String versionID, FileTag tag)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileTagException {

    }

    public void removeTagForFile(String fileID, String versionID, FileTag tag)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileTagException {

    }


    // -------- User Accessor Methods --------

    public List<User> getUsers() {
        return null;
    }

    public User getUserByID(String userID)
            throws NoSuchUserException {
        return null;
    }

    public User getUserByEmail(String email)
            throws NoSuchUserException {
        return null;
    }

    public void addUser(User User)
            throws UserAlreadyExistsException {

    }

    public void updateUser(User user)
            throws NoSuchUserException {

    }

    public void removeUser(User user)
            throws NoSuchUserException {

    }

    public void removeUser(String userID)
            throws NoSuchUserException {

    }
}
