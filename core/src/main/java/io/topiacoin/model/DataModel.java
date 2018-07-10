package io.topiacoin.model;

import io.topiacoin.core.Configuration;
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
import io.topiacoin.model.exceptions.NotInitializedException;
import io.topiacoin.model.exceptions.UserAlreadyExistsException;
import io.topiacoin.model.exceptions.WorkspaceAlreadyExistsException;
import io.topiacoin.model.provider.DataModelProvider;
import io.topiacoin.model.provider.MemoryDataModelProvider;
import io.topiacoin.model.provider.SQLiteDataModelProvider;

import java.util.List;

public class DataModel {

    private static DataModel __instance;

    private DataModelProvider _provider;

    protected DataModel(Configuration _config) {
        if(_config != null) {
            if (_config.getConfigurationOption("model.storage.type", "memory").equalsIgnoreCase("memory")) {
                _provider = new MemoryDataModelProvider();
            } else {
                _provider = new SQLiteDataModelProvider(_config);
            }
        } else {
            throw new NotInitializedException();
        }
    }

    public static synchronized DataModel getInstance() {
        if (__instance == null) {
            throw new NotInitializedException();
        }
        return __instance;
    }

    public static synchronized void initialize(Configuration config) {
        __instance = new DataModel(config);
    }


    // -------- Workspace Accessor Methods --------

    public List<Workspace> getWorkspaces() {
        return _provider.getWorkspaces();
    }

    public List<Workspace> getWorkspacesWithStatus(int workspaceStatus) {
        return _provider.getWorkspacesWithStatus(workspaceStatus);
    }

    public Workspace getWorkspace(long workspaceID)
            throws NoSuchWorkspaceException {
        return _provider.getWorkspace(workspaceID);
    }

    public void addWorkspace(Workspace workspace)
            throws WorkspaceAlreadyExistsException {
        _provider.addWorkspace(workspace);
    }

    public void updateWorkspace(Workspace workspace)
            throws NoSuchWorkspaceException {
        _provider.updateWorkspace(workspace);
    }

    public void removeWorkspace(long workspaceID)
            throws NoSuchWorkspaceException {
        _provider.removeWorkspace(workspaceID);
    }


    // -------- Member Accessor Methods --------

    public List<Member> getMembersInWorkspace(long workspaceID)
            throws NoSuchWorkspaceException {
        return _provider.getMembersInWorkspace(workspaceID);
    }

    public Member getMemberInWorkspace(long workspaceID, String userID)
            throws NoSuchWorkspaceException, NoSuchMemberException {
        return _provider.getMemberInWorkspace(workspaceID, userID);
    }

    public void addMemberToWorkspace(long workspaceID, Member member)
            throws NoSuchWorkspaceException, MemberAlreadyExistsException {
        _provider.addMemberToWorkspace(workspaceID, member);
    }

    public void updateMemberInWorkspace(long workspaceID, Member member)
            throws NoSuchWorkspaceException, NoSuchMemberException {
        _provider.updateMemberInWorkspace(workspaceID, member);
    }

    public void removeMemberFromWorkspace(long workspaceID, Member member)
            throws NoSuchWorkspaceException, NoSuchMemberException {
        _provider.removeMemberFromWorkspace(workspaceID, member);
    }


    // -------- Message Accessor Methods --------

    public List<Message> getMessagesInWorkspace(long workspaceID)
            throws NoSuchWorkspaceException {
        return _provider.getMessagesInWorkspace(workspaceID);
    }

    public Message getMessage(long messageID)
            throws NoSuchMessageException {
        return _provider.getMessage(messageID);
    }

    public void addMessageToWorkspace(long workspaceID, Message message)
            throws NoSuchWorkspaceException, MessageAlreadyExistsException {
        _provider.addMessageToWorkspace(workspaceID, message);
    }

    public void updateMessageInWorkspace(long workspaceID, Message message)
            throws NoSuchWorkspaceException, NoSuchMessageException {
        _provider.updateMessageInWorkspace(workspaceID, message);
    }

    public void removeMessageFromWorkspace(long workspaceID, Message message)
            throws NoSuchWorkspaceException, NoSuchMessageException {
        _provider.removeMessageFromWorkspace(workspaceID, message);
    }


    // -------- File Accessor Methods --------

    public List<File> getFilesInWorkspace(long workspaceID)
            throws NoSuchWorkspaceException {
        return _provider.getFilesInWorkspace(workspaceID);
    }

    public List<File> getFilesInWorkspace(long workspaceID, String parentID)
            throws NoSuchWorkspaceException {
        return _provider.getFilesInWorkspace(workspaceID, parentID);
    }

    public File getFile(String fileID)
            throws NoSuchFileException {
        return _provider.getFile(fileID);
    }

    public void addFileToWorkspace(long workspaceID, File file)
            throws NoSuchWorkspaceException, FileAlreadyExistsException {
        _provider.addFileToWorkspace(workspaceID, file);
    }

    public void updateFileInWorkspace(long workspaceID, File file)
            throws NoSuchWorkspaceException, NoSuchFileException {
        _provider.updateFileInWorkspace(workspaceID, file);
    }

    public void removeFileFromWorkspace(long workspaceID, String fileID)
            throws NoSuchWorkspaceException, NoSuchFileException {
        _provider.removeFileFromWorkspace(workspaceID, fileID);
    }

    public void removeFileFromWorkspace(long workspaceID, File file)
            throws NoSuchWorkspaceException, NoSuchFileException {
        _provider.removeFileFromWorkspace(workspaceID, file);
    }


    // -------- File Version Accessor Methods --------

    public List<String> getAvailableVersionsOfFile(String fileID)
            throws NoSuchFileException {
        return _provider.getAvailableVersionsOfFile(fileID);
    }

    public List<FileVersion> getFileVersionsForFile(String fileID)
            throws NoSuchFileException {
        return _provider.getFileVersionsForFile(fileID);
    }

    public FileVersion getFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        return _provider.getFileVersion(fileID, versionID);
    }

    public void addFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, FileVersionAlreadyExistsException {
        _provider.addFileVersion(fileID, fileVersion);
    }

    public void updateFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, NoSuchFileVersionException {
        _provider.updateFileVersion(fileID, fileVersion);
    }

    public void removeFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        _provider.removeFileVersion(fileID, versionID);
    }

    public void removeFileVersion(String fileID, FileVersion fileVersion)
            throws NoSuchFileException, NoSuchFileVersionException {
        _provider.removeFileVersion(fileID, fileVersion);
    }


    // -------- File Version Receipt Accessor Methods --------

    public List<FileVersionReceipt> getFileVersionReceipts(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        return _provider.getFileVersionReceipts(fileID, versionID);
    }

    public void addFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException {
        _provider.addFileVersionReceipt(fileID, versionID, receipt);
    }

    public void updateFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {
        _provider.updateFileVersionReceipt(fileID, versionID, receipt);
    }

    public void removeFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {
        _provider.removeFileVersionReceipt(fileID, versionID, receipt);
    }


    // -------- File Chunk Accessor Methods --------

    public List<FileChunk> getChunksForFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        return _provider.getChunksForFileVersion(fileID, versionID);
    }

    public FileChunk getFileChunkWithClearHash(String clearChunkHash) {
        return null ;
    }

    public void addChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, FileChunkAlreadyExistsException {
        _provider.addChunkForFile(fileID, versionID, chunk);
    }

    public void updateChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {
        _provider.updateChunkForFile(fileID, versionID, chunk);
    }

    public void removeChunkForFile(String fileID, String versionID, FileChunk chunk)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {
        _provider.removeChunkForFile(fileID, versionID, chunk);
    }


    // -------- File Tag Accessor Methods --------

    public List<FileTag> getTagsForFileVersion(String fileID, String versionID)
            throws NoSuchFileException, NoSuchFileVersionException {
        return _provider.getTagsForFileVersion(fileID, versionID);
    }

    public void addTagForFile(String fileID, String versionID, FileTag tag)
            throws NoSuchFileException, NoSuchFileVersionException, FileTagAlreadyExistsException {
        _provider.addTagForFile(fileID, versionID, tag);
    }

    public void removeTagForFile(String fileID, String versionID, FileTag tag)
            throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileTagException {
        _provider.removeTagForFile(fileID, versionID, tag);
    }


    // -------- User Accessor Methods --------

    public List<User> getUsers() {
        return _provider.getUsers();
    }

    public User getUserByID(String userID)
            throws NoSuchUserException {
        return _provider.getUserByID(userID);
    }

    public User getUserByEmail(String email)
            throws NoSuchUserException {
        return _provider.getUserByEmail(email);
    }

    public void addUser(User user)
            throws UserAlreadyExistsException {
        _provider.addUser(user);
    }

    public void updateUser(User user)
            throws NoSuchUserException {
        _provider.updateUser(user);
    }

    public void removeUser(User user)
            throws NoSuchUserException {
        _provider.removeUser(user);
    }

    public void removeUser(String userID)
            throws NoSuchUserException {
        _provider.removeUser(userID);
    }

    public CurrentUser getCurrentUser() throws NoSuchUserException {
        return _provider.getCurrentUser();
    }

    public void setCurrentUser(CurrentUser user) {
        _provider.setCurrentUser(user);
    }

    public void removeCurrentUser() {
        _provider.removeCurrentUser();
    }

    public void addUserNode(UserNode userNode) {
        _provider.addUserNode(userNode);
    }

    public void removeUserNode(String containerID, UserNode userNode) {
        _provider.removeUserNode(containerID, userNode);
    }

    public List<UserNode> getUserNodesForUserID(String userID) {
        return _provider.getUserNodesForUserID(userID);
    }

    public Workspace getWorkspaceByMyAuthToken(String authToken) throws BadAuthTokenException {
        return _provider.getWorkspaceByMyAuthToken(authToken);
    }

    public boolean hasChunkInWorkspace(String chunkID, long workspaceGuid) {
        return _provider.hasChunkInWorkspace(chunkID, workspaceGuid);
    }

    public List<String> hasChunksInWorkspace(List<String> chunkIDs, long workspaceGuid) {
        return _provider.hasChunksInWorkspace(chunkIDs, workspaceGuid);
    }
}
