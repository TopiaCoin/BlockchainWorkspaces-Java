package io.topiacoin.model.provider;

import io.topiacoin.core.Configuration;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.File;
import io.topiacoin.model.FileChunk;
import io.topiacoin.model.FileTag;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.FileVersionReceipt;
import io.topiacoin.model.Member;
import io.topiacoin.model.Message;
import io.topiacoin.model.User;
import io.topiacoin.model.UserNode;
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
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import java.util.List;

public class SQLiteDataModelProvider implements DataModelProvider {
	SqlJetDb _db;

	public SQLiteDataModelProvider(Configuration config) {
		String dbLoc = config.getConfigurationOption("model.sqllite.location");
		java.io.File dbFile = new java.io.File(dbLoc);
		boolean dbExists = dbFile.exists();
		try {
			_db = SqlJetDb.open(dbFile, true);
			if(!dbExists) {
				_db.getOptions().setAutovacuum(true);
			}
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			try {
				_db.getOptions().setUserVersion(1);
			} finally {
				_db.commit();
			}
			ISqlJetTable workspaces = _db.getTable("Workspaces");
			/*private Map<Long, Workspace> _workspaceMap;
			private Map<Long, List<Member>> _workspaceMemberMap;
			private Map<Long, List<Message>> _workspaceMessageMap;
			private Map<Long, List<File>> _workspaceFileMap;
			private Map<Long, Message> _masterMessageMap;
			private Map<String, File> _masterFileMap;
			private Map<String, List<FileVersion>> _fileVersionsMap;
			private Map<String, List<FileVersionReceipt>> _fileVersionsReceiptMap;
			private Map<String, List<FileChunk>> _fileChunkMap;
			private Map<String, List<FileTag>> _fileVersionsTagMap;
			private Map<String, List<UserNode>> _userIDtoUserNodeMap;*/
		} catch (SqlJetException e) {
			e.printStackTrace();
		}

	}

	@Override public List<Workspace> getWorkspaces() {
		return null;
	}

	@Override public List<Workspace> getWorkspacesWithStatus(int workspaceStatus) {
		return null;
	}

	@Override public Workspace getWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		return null;
	}

	@Override public void addWorkspace(Workspace workspace) throws WorkspaceAlreadyExistsException {

	}

	@Override public void updateWorkspace(Workspace workspace) throws NoSuchWorkspaceException {

	}

	@Override public void removeWorkspace(long workspaceID) throws NoSuchWorkspaceException {

	}

	@Override public List<Member> getMembersInWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		return null;
	}

	@Override public Member getMemberInWorkspace(long workspaceID, String userID) throws NoSuchWorkspaceException, NoSuchMemberException {
		return null;
	}

	@Override public void addMemberToWorkspace(long workspaceID, Member member) throws NoSuchWorkspaceException, MemberAlreadyExistsException {

	}

	@Override public void updateMemberInWorkspace(long workspaceID, Member member) throws NoSuchWorkspaceException, NoSuchMemberException {

	}

	@Override public void removeMemberFromWorkspace(long workspaceID, Member member) throws NoSuchWorkspaceException, NoSuchMemberException {

	}

	@Override public List<Message> getMessagesInWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		return null;
	}

	@Override public Message getMessage(long messageID) throws NoSuchMessageException {
		return null;
	}

	@Override public void addMessageToWorkspace(long workspaceID, Message message) throws NoSuchWorkspaceException, MessageAlreadyExistsException {

	}

	@Override public void updateMessageInWorkspace(long workspaceID, Message message) throws NoSuchWorkspaceException, NoSuchMessageException {

	}

	@Override public void removeMessageFromWorkspace(long workspaceID, Message message) throws NoSuchWorkspaceException, NoSuchMessageException {

	}

	@Override public List<File> getFilesInWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		return null;
	}

	@Override public List<File> getFilesInWorkspace(long workspaceID, String parentID) throws NoSuchWorkspaceException {
		return null;
	}

	@Override public File getFile(String fileID) throws NoSuchFileException {
		return null;
	}

	@Override public void addFileToWorkspace(long workspaceID, File file) throws NoSuchWorkspaceException, FileAlreadyExistsException {

	}

	@Override public void updateFileInWorkspace(long workspaceID, File file) throws NoSuchWorkspaceException, NoSuchFileException {

	}

	@Override public void removeFileFromWorkspace(long workspaceID, String fileID) throws NoSuchWorkspaceException, NoSuchFileException {

	}

	@Override public void removeFileFromWorkspace(long workspaceID, File file) throws NoSuchWorkspaceException, NoSuchFileException {

	}

	@Override public List<String> getAvailableVersionsOfFile(String fileID) throws NoSuchFileException {
		return null;
	}

	@Override public List<FileVersion> getFileVersionsForFile(String fileID) throws NoSuchFileException {
		return null;
	}

	@Override public FileVersion getFileVersion(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		return null;
	}

	@Override public void addFileVersion(String fileID, FileVersion fileVersion) throws NoSuchFileException, FileVersionAlreadyExistsException {

	}

	@Override public void updateFileVersion(String fileID, FileVersion fileVersion) throws NoSuchFileException, NoSuchFileVersionException {

	}

	@Override public void removeFileVersion(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {

	}

	@Override public void removeFileVersion(String fileID, FileVersion fileVersion) throws NoSuchFileException, NoSuchFileVersionException {

	}

	@Override public List<FileVersionReceipt> getFileVersionReceipts(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		return null;
	}

	@Override public void addFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt) throws NoSuchFileException, NoSuchFileVersionException {

	}

	@Override public void updateFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {

	}

	@Override public void removeFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {

	}

	@Override public List<FileChunk> getChunksForFileVersion(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		return null;
	}

	@Override public void addChunkForFile(String fileID, String versionID, FileChunk chunk) throws NoSuchFileException, NoSuchFileVersionException, FileChunkAlreadyExistsException {

	}

	@Override public void updateChunkForFile(String fileID, String versionID, FileChunk chunk) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {

	}

	@Override public void removeChunkForFile(String fileID, String versionID, FileChunk chunk) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {

	}

	@Override public List<FileTag> getTagsForFileVersion(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		return null;
	}

	@Override public void addTagForFile(String fileID, String versionID, FileTag tag) throws NoSuchFileException, NoSuchFileVersionException, FileTagAlreadyExistsException {

	}

	@Override public void removeTagForFile(String fileID, String versionID, FileTag tag) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileTagException {

	}

	@Override public List<User> getUsers() {
		return null;
	}

	@Override public User getUserByID(String userID) throws NoSuchUserException {
		return null;
	}

	@Override public User getUserByEmail(String email) throws NoSuchUserException {
		return null;
	}

	@Override public void addUser(User User) throws UserAlreadyExistsException {

	}

	@Override public void updateUser(User user) throws NoSuchUserException {

	}

	@Override public void removeUser(User user) throws NoSuchUserException {

	}

	@Override public void removeUser(String userID) throws NoSuchUserException {

	}

	@Override public CurrentUser getCurrentUser() throws NoSuchUserException {
		return null;
	}

	@Override public void setCurrentUser(CurrentUser user) {

	}

	@Override public void removeCurrentUser() {

	}

	@Override public void addUserNode(UserNode memberNode) {

	}

	@Override public void removeUserNode(String userID, UserNode memberNode) {

	}

	@Override public List<UserNode> getUserNodesForUserID(String userID) {
		return null;
	}

	@Override public Workspace getWorkspaceByMyAuthToken(String authToken) throws BadAuthTokenException {
		return null;
	}

	@Override public boolean hasChunkInWorkspace(String chunkID, long workspaceGuid) {
		return false;
	}

	@Override public List<String> hasChunksInWorkspace(List<String> chunkIDs, long workspaceGuid) {
		return null;
	}
}
