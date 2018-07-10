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
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

public class SQLiteDataModelProvider implements DataModelProvider {
	private SqlJetDb _db;
	private static final String CREATE_WORKSPACES_TABLE = "CREATE TABLE Workspaces ("
			+ "guid INTEGER NON NULL PRIMARY KEY, "
			+ "name TEXT, "
			+ "description TEXT, "
			+ "status INTEGER, "
			+ "lastModified INTEGER, "
			+ "workspaceKey BLOB)";

	private Workspace rowToWorkspace(ISqlJetCursor cursor) {
		Workspace tr = null;
		try {
			SecretKey workspaceKey = null;//cursor.getBlobAsArray("workspaceKey");
			long guid = cursor.getInteger("guid");
			tr = new Workspace(cursor.getString("name"), cursor.getString("description"), (int) cursor.getInteger("status"), workspaceKey, guid, cursor.getInteger("lastModified"), null, null, null);
		} catch (SqlJetException e) {
			e.printStackTrace();
		}
		return tr;
	}

	private Member rowToMember(ISqlJetCursor cursor) {
		return null;
	}

	private static final String CREATE_FILES_TABLE = "CREATE TABLE Files ("
			+ "entryID TEXT NON NULL PRIMARY KEY, "
			+ "containerID INTEGER NON NULL, "
			+ "name TEXT, "
			+ "mimeType TEXT, "
			+ "parentID TEXT, "
			+ "status INTEGER, "
			+ "isFolder INTEGER, "
			+ "lockOwner TEXT)";
	private static final String CREATE_FILE_VERSIONS_TABLE = "CREATE TABLE FileVersions ("
			+ "entryID TEXT NON NULL PRIMARY KEY, "
			+ "versionID TEXT NON NULL, "
			+ "ownerID TEXT, "
			+ "size INTEGER, "
			+ "date INTEGER, "
			+ "uploadDate INTEGER, "
			+ "fileHash TEXT, "
			+ "status TEXT, "
			+ "lockOwner TEXT)";
	private static final String CREATE_FILE_VERSION_RECEIPTS_TABLE = "CREATE TABLE FileVersionReceipts ("
			+ "entryID TEXT NON NULL PRIMARY KEY, "
			+ "versionID TEXT, "
			+ "recipientID TEXT, "
			+ "date INTEGER)";
	private static final String CREATE_FILE_CHUNKS_TABLE = "CREATE TABLE FileChunks ("
			+ "chunkID TEXT NON NULL PRIMARY KEY, "
			+ "index INTEGER, "
			+ "cipherTextSize INTEGER, "
			+ "clearTextSize INTEGER, "
			+ "chunkKey BLOB, "
			+ "initializationVector BLOB, "
			+ "cipherTextHash TEXT, "
			+ "clearTextHash TEXT, "
			+ "compressionAlgorithm TEXT)";
	private static final String CREATE_FILE_TAGS_TABLE = "CREATE TABLE FileTags ("
			+ "scope TEXT, "
			+ "value TEXT)";
	private static final String CREATE_USER_NODES_TABLE = "CREATE TABLE UserNodes ("
			+ "userId TEXT NON NULL, "
			+ "hostname TEXT, "
			+ "port INTEGER, "
			+ "publicKey BLOB)";
	private static final String CREATE_MEMBERS_TABLE = "CREATE TABLE Members ("
			+ "userId TEXT NON NULL, "
			+ "status INTEGER, "
			+ "inviteDate INTEGER, "
			+ "inviterID TEXT, "
			+ "authToken TEXT, "
			+ "lockOwner TEXT, "
			+ "parentWorkspace INTEGER)";
	private static final String CREATE_MESSAGES_TABLE = "CREATE TABLE Messages ("
			+ "authorID TEXT NON NULL, "
			+ "messageID TEXT NON NULL, "
			+ "workspaceGuid INTEGER, "
			+ "seq INTEGER, "
			+ "timestamp INTEGER, "
			+ "text TEXT NON NULL, "
			+ "mimeType TEXT NON NULL, "
			+ "digitalSignature BLOB)";

	public SQLiteDataModelProvider(Configuration config) {
		String dbLoc = config.getConfigurationOption("model.sqllite.location");
		java.io.File dbFile = new java.io.File(dbLoc);
		System.out.println("Testing SQLite db at " + dbFile.getAbsolutePath());
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
		} catch (SqlJetException e) {
			e.printStackTrace();
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("Workspaces");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_WORKSPACES_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("Files");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_FILES_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("FileVersions");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_FILE_VERSIONS_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("FileVersionReceipts");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_FILE_VERSION_RECEIPTS_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("FileChunks");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_FILE_CHUNKS_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("FileTags");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_FILE_TAGS_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("UserNodes");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_USER_NODES_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("Members");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_MEMBERS_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
		try {
			_db.beginTransaction(SqlJetTransactionMode.WRITE);
			_db.getTable("Messages");
		} catch (SqlJetException e) {
			try {
				_db.createTable(CREATE_MESSAGES_TABLE);
				_db.commit();
			} catch (SqlJetException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Failed to initialize", e1);
			}
		}
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
	}

	@Override public void close() {
		try {
			_db.commit();
		} catch (SqlJetException e) {
			e.printStackTrace();
		}
		try {
			_db.close();
		} catch (SqlJetException e) {
			e.printStackTrace();
		}
	}

	@Override public List<Workspace> getWorkspaces() {
		try {
			_db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			ISqlJetTable table = _db.getTable("Workspaces");
			List<Workspace> tr = new ArrayList<>();
			try {
				ISqlJetCursor cursor = table.order(table.getPrimaryKeyIndexName());
				try {
					if (!cursor.eof()) {
						do {
							tr.add(rowToWorkspace(cursor));
						} while(cursor.next());
					}
					return tr;
				} finally {
					cursor.close();
				}
			} finally {
				_db.commit();
			}
		} catch (SqlJetException e) {
			e.printStackTrace();
		}
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
		return getMembersInWorkspace(workspaceID, true);
	}

	private List<Member> getMembersInWorkspace(long workspaceID, boolean useTransaction) {
		try {
			if(useTransaction) {
				_db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			}
			ISqlJetTable table = _db.getTable("Members");
			List<Member> tr = new ArrayList<>();
			try {
				ISqlJetCursor cursor = table.lookup("parentWorkspace", workspaceID);
				try {
					if (!cursor.eof()) {
						do {
							tr.add(rowToMember(cursor));
						} while(cursor.next());
					}
					return tr;
				} finally {
					cursor.close();
				}
			} finally {
				if(useTransaction) {
					_db.commit();
				}
			}
		} catch (SqlJetException e) {
			e.printStackTrace();
		}
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
