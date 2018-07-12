package io.topiacoin.model.provider;

import io.topiacoin.core.Configuration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileNotFoundException;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class SQLiteDataModelProvider implements DataModelProvider {
	private String _dbPath;
	private static final String CREATE_WORKSPACES_TABLE = "CREATE TABLE IF NOT EXISTS Workspaces ("
			+ "guid INTEGER NON NULL PRIMARY KEY, "
			+ "name TEXT, "
			+ "description TEXT, "
			+ "status INTEGER, "
			+ "lastModified INTEGER, "
			+ "workspaceKey BLOB)";

	private static final String CREATE_FILES_TABLE = "CREATE TABLE IF NOT EXISTS Files ("
			+ "entryID TEXT NON NULL PRIMARY KEY, "
			+ "containerID INTEGER NON NULL, "
			+ "name TEXT, "
			+ "mimeType TEXT, "
			+ "parentID TEXT, "
			+ "status INTEGER, "
			+ "isFolder INTEGER, "
			+ "lockOwner TEXT)";

	private static final String CREATE_FILE_VERSIONS_TABLE = "CREATE TABLE IF NOT EXISTS FileVersions ("
			+ "entryID TEXT NON NULL PRIMARY KEY, "
			+ "versionID TEXT NON NULL, "
			+ "ownerID TEXT, "
			+ "size INTEGER, "
			+ "date INTEGER, "
			+ "uploadDate INTEGER, "
			+ "fileHash TEXT, "
			+ "status TEXT, "
			+ "lockOwner TEXT)";

	private static final String CREATE_FILE_VERSION_RECEIPTS_TABLE = "CREATE TABLE IF NOT EXISTS FileVersionReceipts ("
			+ "entryID TEXT NON NULL PRIMARY KEY, "
			+ "versionID TEXT, "
			+ "recipientID TEXT, "
			+ "date INTEGER)";

	private static final String CREATE_FILE_CHUNKS_TABLE = "CREATE TABLE IF NOT EXISTS FileChunks ("
			+ "chunkID TEXT NON NULL PRIMARY KEY, "
			+ "chunkIndex INTEGER, "
			+ "cipherTextSize INTEGER, "
			+ "clearTextSize INTEGER, "
			+ "chunkKey BLOB, "
			+ "initializationVector BLOB, "
			+ "cipherTextHash TEXT, "
			+ "clearTextHash TEXT, "
			+ "compressionAlgorithm TEXT)";

	private static final String CREATE_FILE_TAGS_TABLE = "CREATE TABLE IF NOT EXISTS FileTags ("
			+ "versionID TEXT, "
			+ "scope TEXT, "
			+ "value TEXT)";

	private static final String CREATE_USER_NODES_TABLE = "CREATE TABLE IF NOT EXISTS UserNodes ("
			+ "userID TEXT NON NULL, "
			+ "hostname TEXT, "
			+ "port INTEGER, "
			+ "publicKey BLOB)";

	private static final String CREATE_MEMBERS_TABLE = "CREATE TABLE IF NOT EXISTS Members ("
			+ "userID TEXT NON NULL, "
			+ "status INTEGER, "
			+ "inviteDate INTEGER, "
			+ "inviterID TEXT, "
			+ "authToken TEXT, "
			+ "lockOwner TEXT, "
			+ "parentWorkspace INTEGER)";

	private static final String CREATE_MESSAGES_TABLE = "CREATE TABLE IF NOT EXISTS Messages ("
			+ "authorID TEXT NON NULL, "
			+ "messageID TEXT NON NULL, "
			+ "workspaceGuid INTEGER, "
			+ "seq INTEGER, "
			+ "timestamp INTEGER, "
			+ "text TEXT NON NULL, "
			+ "mimeType TEXT NON NULL, "
			+ "digitalSignature BLOB)";

	private static final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS Users ("
			+ "userID TEXT NON NULL, "
			+ "email TEXT, "
			+ "publicKey BLOB)";

	private static final String CREATE_VERSION_CHUNKS_TABLE = "CREATE TABLE IF NOT EXISTS VersionChunks ("
			+ "entryID TEXT NON NULL, "
			+ "versionID TEXT, "
			+ "chunkID TEXT)";

	private static final String FETCH_WORKSPACE_SQL = "SELECT * FROM Workspaces WHERE guid = ?";
	private static final String FETCH_FILE_SQL = "SELECT * FROM Files WHERE entryID = ?";
	private static final String FETCH_FILEVERSION_SQL = "SELECT * FROM FileVersions WHERE entryID = ? AND versionID = ?";
	private static final String FETCH_FILECHUNK_SQL = "SELECT * FROM FileChunks WHERE chunkID = ?";
	private static final String FETCH_MESSAGE_SQL = "SELECT * FROM Messages WHERE messageID = ?";

	private CurrentUser _currentUser = null;

	public SQLiteDataModelProvider(Configuration config) {
		String dbLoc = config.getConfigurationOption("model.sqllite.location");
		java.io.File dbFile = new java.io.File(dbLoc);
		dbFile.getParentFile().mkdirs();
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		_dbPath = "jdbc:sqlite:" + dbFile.getAbsolutePath().replaceAll("\\\\", "/");
		System.out.println("SQLite db at " + _dbPath);
		try (Connection conn = DriverManager.getConnection(_dbPath); Statement stmt = conn.createStatement()) {
			conn.setAutoCommit(false);
			stmt.execute(CREATE_WORKSPACES_TABLE);
			stmt.execute(CREATE_FILES_TABLE);
			stmt.execute(CREATE_FILE_VERSIONS_TABLE);
			stmt.execute(CREATE_FILE_VERSION_RECEIPTS_TABLE);
			stmt.execute(CREATE_FILE_CHUNKS_TABLE);
			stmt.execute(CREATE_FILE_TAGS_TABLE);
			stmt.execute(CREATE_USER_NODES_TABLE);
			stmt.execute(CREATE_MEMBERS_TABLE);
			stmt.execute(CREATE_MESSAGES_TABLE);
			stmt.execute(CREATE_USERS_TABLE);
			stmt.execute(CREATE_VERSION_CHUNKS_TABLE);
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override public void close() {
	}

	@Override public List<Workspace> getWorkspaces() {
		try (Connection conn = DriverManager.getConnection(_dbPath); Statement stmt = conn.createStatement()) {
			List<Workspace> tr = new ArrayList<>();
			try (ResultSet results = stmt.executeQuery("SELECT * FROM Workspaces")) {
				while (results.next()) {
					try {
						tr.add(rowToWorkspace(results));
					} catch (NoSuchWorkspaceException e) {
						//NOP
					}
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public List<Workspace> getWorkspacesWithStatus(int workspaceStatus) {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Workspaces WHERE status = ?")) {
			List<Workspace> tr = new ArrayList<>();
			stmt.setInt(1, workspaceStatus);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					try {
						tr.add(rowToWorkspace(results));
					} catch (NoSuchWorkspaceException e) {
						//NOP
					}
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public Workspace getWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(FETCH_WORKSPACE_SQL)) {
			stmt.setLong(1, workspaceID);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					return rowToWorkspace(results);
				} else {
					throw new NoSuchWorkspaceException();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addWorkspace(Workspace workspace) throws WorkspaceAlreadyExistsException {
		try {
			getWorkspace(workspace.getGuid());
			throw new WorkspaceAlreadyExistsException();
		} catch (NoSuchWorkspaceException e) {
			String sql = "INSERT INTO Workspaces (name, description, status, lastModified, workspaceKey, guid) VALUES (?,?,?,?,?,?)";
			try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, workspace.getName());
				stmt.setString(2, workspace.getDescription());
				stmt.setInt(3, workspace.getStatus());
				stmt.setLong(4, workspace.getLastModified());
				if(workspace.getWorkspaceKey() == null) {
					stmt.setNull(5, Types.BLOB);
				} else {
					stmt.setBytes(5, workspace.getWorkspaceKey().getEncoded());
				}
				stmt.setLong(6, workspace.getGuid());
				stmt.execute();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override public void updateWorkspace(Workspace workspace) throws NoSuchWorkspaceException {
		String sql = "UPDATE Workspaces SET name = ?, description = ?, status = ?, lastModified = ?, workspaceKey = ? WHERE guid = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			workspaceExists(conn, workspace.getGuid());
			stmt.setString(1, workspace.getName());
			stmt.setString(2, workspace.getDescription());
			stmt.setInt(3, workspace.getStatus());
			stmt.setLong(4, workspace.getLastModified());
			if(workspace.getWorkspaceKey() == null) {
				stmt.setNull(5, Types.BLOB);
			} else {
				stmt.setBytes(5, workspace.getWorkspaceKey().getEncoded());
			}
			stmt.setLong(6, workspace.getGuid());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		try (Connection conn = DriverManager.getConnection(_dbPath);
				PreparedStatement stmt = conn.prepareStatement("DELETE FROM Workspaces WHERE guid = ?");
				PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM Members WHERE parentWorkspace = ?");
				PreparedStatement wksstmt = conn.prepareStatement(FETCH_WORKSPACE_SQL)) {
			wksstmt.setLong(1, workspaceID);
			try (ResultSet results = wksstmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchWorkspaceException();
				}
			}
			stmt.setLong(1, workspaceID);
			stmt2.setLong(1, workspaceID);
			stmt.execute();
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public List<Member> getMembersInWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		String sql2 = "SELECT * FROM Members WHERE parentWorkspace = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql2)) {
			workspaceExists(conn, workspaceID);
			List<Member> tr = new ArrayList<>();
			stmt.setLong(1, workspaceID);
			try (ResultSet results2 = stmt.executeQuery()) {
				while (results2.next()) {
					tr.add(rowToMember(results2));
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public Member getMemberInWorkspace(long workspaceID, String userID) throws NoSuchWorkspaceException, NoSuchMemberException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Members WHERE userID = ? AND parentWorkspace = ?")) {
			workspaceExists(conn, workspaceID);
			stmt.setString(1, userID);
			stmt.setLong(2, workspaceID);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					return rowToMember(results);
				}
			}
			throw new NoSuchMemberException();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addMemberToWorkspace(long workspaceID, Member member) throws NoSuchWorkspaceException, MemberAlreadyExistsException {
		try {
			getMemberInWorkspace(workspaceID, member.getUserID());
			throw new MemberAlreadyExistsException();
		} catch (NoSuchMemberException e) {
			String sql = "INSERT INTO Members (status, inviteDate, inviterID, authToken, lockOwner, parentWorkspace, userID) VALUES (?,?,?,?,?,?,?)";
			try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, member.getStatus());
				stmt.setLong(2, member.getInviteDate());
				stmt.setString(3, member.getInviterID());
				stmt.setString(4, member.getAuthToken());
				stmt.setString(5, member.getLockOwner());
				stmt.setLong(6, workspaceID);
				stmt.setString(7, member.getUserID());
				stmt.execute();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override public void updateMemberInWorkspace(long workspaceID, Member member) throws NoSuchWorkspaceException, NoSuchMemberException {
		getMemberInWorkspace(workspaceID, member.getUserID());
		String sql = "UPDATE Members SET status = ?, inviteDate = ?, inviterID = ?, authToken = ?, lockOwner = ?, parentWorkspace = ? WHERE userID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, member.getStatus());
			stmt.setLong(2, member.getInviteDate());
			stmt.setString(3, member.getInviterID());
			stmt.setString(4, member.getAuthToken());
			stmt.setString(5, member.getLockOwner());
			stmt.setLong(6, workspaceID);
			stmt.setString(7, member.getUserID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeMemberFromWorkspace(long workspaceID, Member member) throws NoSuchWorkspaceException, NoSuchMemberException {
		getMemberInWorkspace(workspaceID, member.getUserID());
		String sql = "DELETE FROM Members WHERE parentWorkspace = ? AND userID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, workspaceID);
			stmt.setString(2, member.getUserID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public List<Message> getMessagesInWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Messages WHERE workspaceGuid = ?")) {
			workspaceExists(conn, workspaceID);
			List<Message> tr = new ArrayList<>();
			stmt.setLong(1, workspaceID);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					tr.add(rowToMessage(results));
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public Message getMessage(String messageID) throws NoSuchMessageException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Messages WHERE messageID = ?")) {
			stmt.setString(1, messageID);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					return rowToMessage(results);
				}
			}
			throw new NoSuchMessageException();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addMessageToWorkspace(long workspaceID, Message message) throws NoSuchWorkspaceException, MessageAlreadyExistsException {
		String sql = "INSERT INTO Messages (authorID, workspaceGuid, seq, timestamp, text, mimeType, digitalSignature, messageID) VALUES (?,?,?,?,?,?,?,?)";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			workspaceExists(conn, workspaceID);
			try {
				messageExists(conn, message.getMessageID());
				throw new MessageAlreadyExistsException();
			} catch (NoSuchMessageException e) {
				stmt.setString(1, message.getAuthorID());
				stmt.setLong(2, message.getWorkspaceGuid());
				stmt.setLong(3, message.getSeq());
				stmt.setLong(4, message.getTimestamp());
				stmt.setString(5, message.getText());
				stmt.setString(6, message.getMimeType());
				stmt.setBytes(7, message.getDigitalSignature());
				stmt.setString(8, message.getMessageID());
				stmt.execute();
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void updateMessageInWorkspace(long workspaceID, Message message) throws NoSuchWorkspaceException, NoSuchMessageException {
		String sql = "UPDATE Messages SET authorID = ?, workspaceGuid = ?, seq = ?, timestamp = ?, text = ?, mimeType = ?, digitalSignature = ? WHERE messageID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			workspaceExists(conn, workspaceID);
			messageExists(conn, message.getMessageID());
			stmt.setString(1, message.getAuthorID());
			stmt.setLong(2, message.getWorkspaceGuid());
			stmt.setLong(3, message.getSeq());
			stmt.setLong(4, message.getTimestamp());
			stmt.setString(5, message.getText());
			stmt.setString(6, message.getMimeType());
			stmt.setBytes(7, message.getDigitalSignature());
			stmt.setString(8, message.getMessageID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeMessageFromWorkspace(long workspaceID, Message message) throws NoSuchWorkspaceException, NoSuchMessageException {
		String sql = "DELETE FROM Messages WHERE messageID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			workspaceExists(conn, workspaceID);
			messageExists(conn, message.getMessageID());
			stmt.setString(1, message.getMessageID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public List<File> getFilesInWorkspace(long workspaceID) throws NoSuchWorkspaceException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Files WHERE containerID = ?")) {
			workspaceExists(conn, workspaceID);
			List<File> tr = new ArrayList<>();
			stmt.setLong(1, workspaceID);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					try {
						tr.add(rowToFile(results));
					} catch (NoSuchFileException e) {
						//NOP, that's weird
					}
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public List<File> getFilesInWorkspace(long workspaceID, String parentID) throws NoSuchWorkspaceException {
		if(parentID == null) {
			parentID = "";
		}
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Files WHERE containerID = ? AND parentID = ?")) {
			workspaceExists(conn, workspaceID);
			List<File> tr = new ArrayList<>();
			stmt.setLong(1, workspaceID);
			stmt.setString(2, parentID);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					try {
						tr.add(rowToFile(results));
					} catch (NoSuchFileException e) {
						//NOP, that's weird
					}
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public File getFile(String fileID) throws NoSuchFileException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(FETCH_FILE_SQL)) {
			stmt.setString(1, fileID);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					return rowToFile(results);
				}
			}
			throw new NoSuchFileException();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addFileToWorkspace(long workspaceID, File file) throws NoSuchWorkspaceException, FileAlreadyExistsException {
		if(file.getContainerID() != workspaceID) {
			throw new IllegalArgumentException("file.getContainerID() != workspaceID");
		}
		try {
			getFile(file.getEntryID());
			throw new FileAlreadyExistsException();
		} catch (NoSuchFileException e) {
			String sql = "INSERT INTO Files (containerID, name, mimeType, parentID, status, isFolder, lockOwner, entryID) VALUES (?,?,?,?,?,?,?,?)";
			try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
				workspaceExists(conn, workspaceID);
				stmt.setLong(1, file.getContainerID());
				stmt.setString(2, file.getName());
				stmt.setString(3, file.getMimeType());
				stmt.setString(4, file.getParentID() == null ? "" : file.getParentID());
				stmt.setInt(5, file.getStatus());
				stmt.setInt(6, file.isFolder() ? 1 : 0);
				stmt.setString(7, file.getLockOwner());
				stmt.setString(8, file.getEntryID());
				stmt.execute();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override public void updateFileInWorkspace(long workspaceID, File file) throws NoSuchWorkspaceException, NoSuchFileException {
		String sql = "UPDATE Files SET containerID = ?, name = ?, mimeType = ?, parentID = ?, status = ?, isFolder = ?, lockOwner = ? WHERE entryID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			workspaceExists(conn, workspaceID);
			fileExists(conn, file.getEntryID());
			stmt.setLong(1, file.getContainerID());
			stmt.setString(2, file.getName());
			stmt.setString(3, file.getMimeType());
			stmt.setString(4, file.getParentID());
			stmt.setInt(5, file.getStatus());
			stmt.setInt(6, file.isFolder() ? 1 : 0);
			stmt.setString(7, file.getLockOwner());
			stmt.setString(8, file.getEntryID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeFileFromWorkspace(long workspaceID, String fileID) throws NoSuchWorkspaceException, NoSuchFileException {
		String sql = "DELETE FROM Files WHERE entryID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			workspaceExists(conn, workspaceID);
			fileExists(conn, fileID);
			stmt.setString(1, fileID);
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeFileFromWorkspace(long workspaceID, File file) throws NoSuchWorkspaceException, NoSuchFileException {
		removeFileFromWorkspace(workspaceID, file.getEntryID());
	}

	@Override public List<String> getAvailableVersionsOfFile(String fileID) throws NoSuchFileException {
		List<FileVersion> versions = getFileVersionsForFile(fileID);
		List<String> tr = new ArrayList<>(versions.size());
		for(FileVersion v : versions) {
			tr.add(v.getVersionID());
		}
		return tr;
	}

	@Override public List<FileVersion> getFileVersionsForFile(String fileID) throws NoSuchFileException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM FileVersions WHERE entryID = ?")) {
			fileExists(conn, fileID);
			List<FileVersion> tr = new ArrayList<>();
			stmt.setString(1, fileID);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					try {
						tr.add(rowToFileVersion(results));
					} catch (NoSuchFileVersionException e) {
						//NOP, that's weird
					}
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public FileVersion getFileVersion(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(FETCH_FILEVERSION_SQL)) {
			fileExists(conn, fileID);
			stmt.setString(1, fileID);
			stmt.setString(2, versionID);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					return rowToFileVersion(results);
				}
			}
			throw new NoSuchFileVersionException();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addFileVersion(String fileID, FileVersion fileVersion) throws NoSuchFileException, FileVersionAlreadyExistsException {
		try {
			getFileVersion(fileID, fileVersion.getVersionID());
			throw new FileVersionAlreadyExistsException();
		} catch (NoSuchFileVersionException e) {
			String sql = "INSERT INTO FileVersions (entryID, ownerID, size, date, uploadDate, fileHash, status, lockOwner, versionID) VALUES (?,?,?,?,?,?,?,?,?)";
			try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
				fileExists(conn, fileID);
				stmt.setString(1, fileVersion.getEntryID());
				stmt.setString(2, fileVersion.getOwnerID());
				stmt.setLong(3, fileVersion.getSize());
				stmt.setLong(4, fileVersion.getDate());
				stmt.setLong(5, fileVersion.getUploadDate());
				stmt.setString(6, fileVersion.getFileHash());
				stmt.setString(7, fileVersion.getStatus());
				stmt.setString(8, fileVersion.getLockOwner());
				stmt.setString(9, fileVersion.getVersionID());
				stmt.execute();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override public void updateFileVersion(String fileID, FileVersion fileVersion) throws NoSuchFileException, NoSuchFileVersionException {
		getFileVersion(fileID, fileVersion.getVersionID());
		String sql = "UPDATE FileVersions SET entryID = ?, ownerID = ?, size = ?, date = ?, uploadDate = ?, fileHash = ?, status = ?, lockOwner = ? WHERE versionID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			fileExists(conn, fileID);
			stmt.setString(1, fileVersion.getEntryID());
			stmt.setString(2, fileVersion.getOwnerID());
			stmt.setLong(3, fileVersion.getSize());
			stmt.setLong(4, fileVersion.getDate());
			stmt.setLong(5, fileVersion.getUploadDate());
			stmt.setString(6, fileVersion.getFileHash());
			stmt.setString(7, fileVersion.getStatus());
			stmt.setString(8, fileVersion.getLockOwner());
			stmt.setString(9, fileVersion.getVersionID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeFileVersion(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		String sql = "DELETE FROM FileVersions WHERE entryID = ? AND versionID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			stmt.setString(1, fileID);
			stmt.setString(2, versionID);
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeFileVersion(String fileID, FileVersion fileVersion) throws NoSuchFileException, NoSuchFileVersionException {
		removeFileVersion(fileID, fileVersion.getVersionID());
	}

	@Override public List<FileVersionReceipt> getFileVersionReceipts(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM FileVersionReceipts WHERE entryID = ? AND versionID = ?")) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			List<FileVersionReceipt> tr = new ArrayList<>();
			stmt.setString(1, fileID);
			stmt.setString(2, versionID);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					tr.add(rowToFileVersionReceipt(results));
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt) throws NoSuchFileException, NoSuchFileVersionException {
		String sql = "INSERT INTO FileVersionReceipts (entryID, recipientID, date, versionID) VALUES (?,?,?,?)";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			stmt.setString(1, receipt.getEntryID());
			stmt.setString(2, receipt.getRecipientID());
			stmt.setLong(3, receipt.getDate());
			stmt.setString(4, receipt.getVersionID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void updateFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {
		String sql = "SELECT * FROM FileVersionReceipts WHERE entryID = ? AND versionID = ?";
		String sql2 = "UPDATE FileVersionReceipts SET recipientID = ?, date = ? WHERE entryID = ? AND versionID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			stmt.setString(1, receipt.getEntryID());
			stmt.setString(2, receipt.getVersionID());
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchFileVersionReceiptException();
				}
			}
			stmt2.setString(1, receipt.getRecipientID());
			stmt2.setLong(2, receipt.getDate());
			stmt2.setString(3, receipt.getEntryID());
			stmt2.setString(4, receipt.getVersionID());
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {
		String sql = "SELECT * FROM FileVersionReceipts WHERE entryID = ? AND versionID = ?";
		String sql2 = "DELETE FROM FileVersionReceipts WHERE entryID = ? AND versionID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			stmt.setString(1, receipt.getEntryID());
			stmt.setString(2, receipt.getVersionID());
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchFileVersionReceiptException();
				}
			}
			stmt2.setString(1, receipt.getEntryID());
			stmt2.setString(2, receipt.getVersionID());
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public List<FileChunk> getChunksForFileVersion(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		String sql = "SELECT * FROM FileChunks WHERE chunkID in (SELECT chunkID FROM VersionChunks WHERE entryID = ? AND versionID = ?)";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			List<FileChunk> tr = new ArrayList<>();
			stmt.setString(1, fileID);
			stmt.setString(2, versionID);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					tr.add(rowToFileChunk(results));
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addChunkForFile(String fileID, String versionID, FileChunk chunk) throws NoSuchFileException, NoSuchFileVersionException, FileChunkAlreadyExistsException {
		String sql = "INSERT INTO VersionChunks (entryID, versionID, chunkID) VALUES (?,?,?)";
		String sql2 = "INSERT INTO FileChunks (chunkIndex, cipherTextSize, clearTextSize, chunkKey, initializationVector, cipherTextHash, clearTextHash, compressionAlgorithm, chunkID) VALUES (?,?,?,?,?,?,?,?,?)";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			try {
				fileChunkExists(conn, chunk.getChunkID());
				throw new FileChunkAlreadyExistsException();
			} catch (NoSuchFileChunkException e) {
				stmt.setString(1, fileID);
				stmt.setString(2, versionID);
				stmt.setString(3, chunk.getChunkID());
				stmt.execute();

				stmt2.setLong(1, chunk.getIndex());
				stmt2.setLong(2, chunk.getCipherTextSize());
				stmt2.setLong(3, chunk.getClearTextSize());
				if(chunk.getChunkKey() == null) {
					stmt2.setNull(4, Types.BLOB);
				} else {
					stmt2.setBytes(4, chunk.getChunkKey().getEncoded());
				}
				stmt2.setBytes(5, chunk.getInitializationVector());
				stmt2.setString(6, chunk.getCipherTextHash());
				stmt2.setString(7, chunk.getClearTextHash());
				stmt2.setString(8, chunk.getCompressionAlgorithm());
				stmt2.setString(9, chunk.getChunkID());
				stmt2.execute();
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void updateChunkForFile(String fileID, String versionID, FileChunk chunk) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {
		String sql = "UPDATE FileChunks SET chunkIndex = ?, cipherTextSize = ?, clearTextSize = ?, chunkKey = ?, initializationVector = ?, cipherTextHash = ?, clearTextHash = ?, compressionAlgorithm = ? WHERE chunkID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			fileChunkExists(conn, chunk.getChunkID());
			stmt.setLong(1, chunk.getIndex());
			stmt.setLong(2, chunk.getCipherTextSize());
			stmt.setLong(3, chunk.getClearTextSize());
			if(chunk.getChunkKey() == null) {
				stmt.setNull(4, Types.BLOB);
			} else {
				stmt.setBytes(4, chunk.getChunkKey().getEncoded());
			}
			stmt.setBytes(5, chunk.getInitializationVector());
			stmt.setString(6, chunk.getCipherTextHash());
			stmt.setString(7, chunk.getClearTextHash());
			stmt.setString(8, chunk.getCompressionAlgorithm());
			stmt.setString(9, chunk.getChunkID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeChunkForFile(String fileID, String versionID, FileChunk chunk) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {
		String sql = "DELETE FROM FileChunks WHERE chunkID = ?";
		String sql2 = "DELETE FROM VersionChunks WHERE entryID = ? AND versionID = ? AND chunkID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			fileChunkExists(conn, chunk.getChunkID());
			stmt.setString(1, chunk.getChunkID());
			stmt.execute();

			stmt2.setString(1, fileID);
			stmt2.setString(2, versionID);
			stmt2.setString(3, chunk.getChunkID());
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public List<FileTag> getTagsForFileVersion(String fileID, String versionID) throws NoSuchFileException, NoSuchFileVersionException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM FileTags WHERE versionID = ?")) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			List<FileTag> tr = new ArrayList<>();
			stmt.setString(1, versionID);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					tr.add(rowToFileTag(results));
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addTagForFile(String fileID, String versionID, FileTag tag) throws NoSuchFileException, NoSuchFileVersionException, FileTagAlreadyExistsException {
		String sql = "SELECT * FROM FileTags WHERE scope = ? AND value = ? AND versionID = ?";
		String sql2 = "INSERT INTO FileTags (scope, value, versionID) VALUES (?,?,?)";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			stmt.setString(1, tag.getScope());
			stmt.setString(2, tag.getValue());
			stmt.setString(3, versionID);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					throw new FileTagAlreadyExistsException();
				}
			}
			stmt2.setString(1, tag.getScope());
			stmt2.setString(2, tag.getValue());
			stmt2.setString(3, versionID);
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeTagForFile(String fileID, String versionID, FileTag tag) throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileTagException {
		String sql = "SELECT * FROM FileTags WHERE scope = ? AND value = ? AND versionID = ?";
		String sql2 = "DELETE FROM FileTags WHERE scope = ? AND value = ? AND versionID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			fileExists(conn, fileID);
			fileVersionExists(conn, fileID, versionID);
			stmt.setString(1, tag.getScope());
			stmt.setString(2, tag.getValue());
			stmt.setString(3, versionID);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchFileTagException();
				}
			}
			stmt2.setString(1, tag.getScope());
			stmt2.setString(2, tag.getValue());
			stmt2.setString(3, versionID);
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public List<User> getUsers() {
		try (Connection conn = DriverManager.getConnection(_dbPath); Statement stmt = conn.createStatement()) {
			List<User> tr = new ArrayList<>();
			try (ResultSet results = stmt.executeQuery("SELECT * FROM Users")) {
				while (results.next()) {
					try {
						tr.add(rowToUser(results));
					} catch (CryptographicException e) {
						//NOP
					}
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public User getUserByID(String userID) throws NoSuchUserException {
		return getUser(userID, "userID");
	}

	@Override public User getUserByEmail(String email) throws NoSuchUserException {
		return getUser(email, "email");
	}

	private User getUser(String identifier, String column) throws NoSuchUserException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Users WHERE "+column+" = ?")) {
			stmt.setString(1, identifier);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					return rowToUser(results);
				}
			}
			throw new NoSuchUserException();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (CryptographicException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public void addUser(User user) throws UserAlreadyExistsException {
		if(user.getPublicKey() == null) {
			throw new IllegalArgumentException("Public Key cannot be null");
		}
		String sql = "SELECT * FROM Users WHERE userID = ?";
		String sql2 = "INSERT INTO Users (email, publicKey, userID) VALUES (?,?,?)";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			stmt.setString(1, user.getUserID());
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					throw new UserAlreadyExistsException();
				}
			}
			stmt2.setString(1, user.getEmail());
			stmt2.setBytes(2, user.getPublicKey().getEncoded());
			stmt2.setString(3, user.getUserID());
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void updateUser(User user) throws NoSuchUserException {
		String sql = "SELECT * FROM Users WHERE userID = ?";
		String sql2 = "UPDATE Users SET email = ?, publicKey = ? WHERE userID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			stmt.setString(1, user.getUserID());
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchUserException();
				}
			}
			stmt2.setString(1, user.getEmail());
			stmt2.setBytes(2, user.getPublicKey().getEncoded());
			stmt2.setString(3, user.getUserID());
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeUser(User user) throws NoSuchUserException {
		removeUser(user.getUserID());
	}

	@Override public void removeUser(String userID) throws NoSuchUserException {
		String sql = "SELECT * FROM Users WHERE userID = ?";
		String sql2 = "DELETE FROM Users WHERE userID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
			stmt.setString(1, userID);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchUserException();
				}
			}
			stmt2.setString(1, userID);
			stmt2.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public CurrentUser getCurrentUser() throws NoSuchUserException {
		if(_currentUser == null) {
			throw new NoSuchUserException();
		}
		return _currentUser;
	}

	@Override public void setCurrentUser(CurrentUser user) {
		_currentUser = user;
	}

	@Override public void removeCurrentUser() {
		_currentUser = null;
	}

	@Override public void addUserNode(UserNode memberNode) {
		String sql = "INSERT INTO UserNodes (hostname, port, publicKey, userID) VALUES (?,?,?,?)";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, memberNode.getHostname());
			stmt.setInt(2, memberNode.getPort());
			stmt.setBytes(3, memberNode.getPublicKey());
			stmt.setString(4, memberNode.getUserID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public void removeUserNode(String userID, UserNode memberNode) {
		String sql = "DELETE FROM UserNodes WHERE hostname = ? AND port = ? AND publicKey = ? ANd userID = ?";
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, memberNode.getHostname());
			stmt.setInt(2, memberNode.getPort());
			stmt.setBytes(3, memberNode.getPublicKey());
			stmt.setString(4, memberNode.getUserID());
			stmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override public List<UserNode> getUserNodesForUserID(String userID) {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM UserNodes WHERE userID = ?")) {
			List<UserNode> tr = new ArrayList<>();
			stmt.setString(1, userID);
			try (ResultSet results = stmt.executeQuery()) {
				while (results.next()) {
					tr.add(rowToUserNode(results));
				}
			}
			return tr;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public Workspace getWorkspaceByMyAuthToken(String authToken) throws BadAuthTokenException {
		try (Connection conn = DriverManager.getConnection(_dbPath); PreparedStatement stmt = conn.prepareStatement("SELECT parentWorkspace FROM Members WHERE userID = ? AND authToken = ?")) {
			stmt.setString(1, _currentUser.getUserID());
			stmt.setString(2, authToken);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					long workspaceGuid = results.getLong("parentWorkspace");
					return getWorkspace(workspaceGuid);
				} else {
					throw new BadAuthTokenException();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoSuchWorkspaceException e) {
			throw new BadAuthTokenException(e);
		}
		return null;
	}

	@Override public boolean hasChunkInWorkspace(String chunkID, long workspaceGuid) {
		return false;
	}

	@Override public List<String> hasChunksInWorkspace(List<String> chunkIDs, long workspaceGuid) {
		return null;
	}

	private Workspace rowToWorkspace(ResultSet results) throws SQLException, NoSuchWorkspaceException {
		Workspace tr;
		byte[] keyBytes = results.getBytes("workspaceKey");
		SecretKey workspaceKey = keyBytes == null ? null : new SecretKeySpec(keyBytes, "AES");
		long guid = results.getLong("guid");
		tr = new Workspace(results.getString("name"), results.getString("description"), results.getInt("status"), workspaceKey, guid, results.getInt("lastModified"), getMembersInWorkspace(guid), getFilesInWorkspace(guid), getMessagesInWorkspace(guid));
		return tr;
	}

	private Member rowToMember(ResultSet results) throws SQLException {
		return new Member(results.getString("userID"), results.getInt("status"), results.getLong("inviteDate"), results.getString("inviterID"), results.getString("authToken"), results.getString("lockOwner"));
	}

	private File rowToFile(ResultSet results) throws SQLException, NoSuchFileException {
		String entryID = results.getString("entryID");
		return new File(results.getString("name"), results.getString("mimeType"), entryID, results.getLong("containerID"), results.getString("parentID"), results.getInt("isFolder") == 1, results.getInt("status"), results.getString("lockOwner"), getFileVersionsForFile(entryID));
	}

	private FileVersion rowToFileVersion(ResultSet results) throws SQLException, NoSuchFileException, NoSuchFileVersionException {
		String entryID = results.getString("entryID");
		String versionID = results.getString("versionID");
		List<String> ancestorIDs = null;
		return new FileVersion(entryID, versionID, results.getString("ownerID"), results.getLong("size"), results.getLong("date"), results.getLong("uploadDate"), results.getString("fileHash"), results.getString("status"), getTagsForFileVersion(entryID, versionID), getChunksForFileVersion(entryID, versionID), getFileVersionReceipts(entryID, versionID), ancestorIDs, results.getString("lockOwner"));
	}

	private FileVersionReceipt rowToFileVersionReceipt(ResultSet results) throws SQLException {
		return new FileVersionReceipt(results.getString("entryID"), results.getString("versionID"), results.getString("recipientID"), results.getLong("date"));
	}

	private FileChunk rowToFileChunk(ResultSet results) throws SQLException {
		byte[] keyBytes = results.getBytes("chunkKey");
		SecretKey chunkKey = keyBytes == null ? null : new SecretKeySpec(keyBytes, "AES");
		return new FileChunk(results.getString("chunkID"), results.getLong("chunkIndex"), results.getLong("cipherTextSize"), results.getLong("clearTextSize"), chunkKey, results.getBytes("initializationVector"), results.getString("cipherTextHash"), results.getString("clearTextHash"), results.getString("compressionAlgorithm"));
	}

	private FileTag rowToFileTag(ResultSet results) throws SQLException {
		return new FileTag(results.getString("scope"), results.getString("value"));
	}

	private UserNode rowToUserNode(ResultSet results) throws SQLException {
		return new UserNode(results.getString("userID"), results.getString("hostname"), results.getInt("port"), results.getBytes("publicKey"));
	}

	private Message rowToMessage(ResultSet results) throws SQLException {
		return new Message(results.getString("authorID"), results.getString("messageID"), results.getLong("workspaceGuid"), results.getLong("seq"), results.getLong("timestamp"), results.getString("text"), results.getString("mimeType"), results.getBytes("digitalSignature"));
	}

	private User rowToUser(ResultSet results) throws SQLException, CryptographicException {
		PublicKey publicKey = CryptoUtils.getECPublicKeyFromEncodedBytes(results.getBytes("publicKey"));
		return new User(results.getString("userID"), results.getString("email"), publicKey);
	}

	private void workspaceExists(Connection conn, long workspaceGuid) throws NoSuchWorkspaceException, SQLException {
		try(PreparedStatement wksstmt = conn.prepareStatement(FETCH_WORKSPACE_SQL)) {
			wksstmt.setLong(1, workspaceGuid);
			try (ResultSet wksResult = wksstmt.executeQuery()) {
				if (!wksResult.next()) {
					throw new NoSuchWorkspaceException();
				}
			}
		}
	}

	private void fileExists(Connection conn, String entryID) throws NoSuchFileException, SQLException {
		try(PreparedStatement filestmt = conn.prepareStatement(FETCH_FILE_SQL)) {
			filestmt.setString(1, entryID);
			try (ResultSet fileResult = filestmt.executeQuery()) {
				if (!fileResult.next()) {
					throw new NoSuchFileException();
				}
			}
		}
	}

	private void fileVersionExists(Connection conn, String fileID, String versionID) throws NoSuchFileVersionException, SQLException {
		try (PreparedStatement stmt = conn.prepareStatement(FETCH_FILEVERSION_SQL)) {
			stmt.setString(1, fileID);
			stmt.setString(2, versionID);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchFileVersionException();
				}
			}
		}
	}

	private void fileChunkExists(Connection conn, String chunkID) throws NoSuchFileChunkException, SQLException {
		try (PreparedStatement stmt = conn.prepareStatement(FETCH_FILECHUNK_SQL)) {
			stmt.setString(1, chunkID);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchFileChunkException();
				}
			}
		}
	}

	private void messageExists(Connection conn, String messageID) throws NoSuchMessageException, SQLException {
		try (PreparedStatement stmt = conn.prepareStatement(FETCH_MESSAGE_SQL)) {
			stmt.setString(1, messageID);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.next()) {
					throw new NoSuchMessageException();
				}
			}
		}
	}
}
