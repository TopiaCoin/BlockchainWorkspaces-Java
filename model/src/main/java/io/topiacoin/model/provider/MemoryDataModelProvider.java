package io.topiacoin.model.provider;

import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.File;
import io.topiacoin.model.FileChunk;
import io.topiacoin.model.FileTag;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.FileVersionReceipt;
import io.topiacoin.model.Member;
import io.topiacoin.model.UserNode;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MemoryDataModelProvider implements DataModelProvider {

	private Map<Long, Workspace> _workspaceMap;
	private Map<Long, List<Member>> _workspaceMemberMap;
	private Map<Long, List<Message>> _workspaceMessageMap;
	private Map<Long, List<File>> _workspaceFileMap;
	private Map<Long, Message> _masterMessageMap;
	private Map<String, File> _masterFileMap;
	private Map<String, List<FileVersion>> _fileVersionsMap;
	private Map<String, List<FileVersionReceipt>> _fileVersionsReceiptMap;
	private Map<String, List<FileChunk>> _fileChunkMap;
	private Map<String, List<FileTag>> _fileVersionsTagMap;
	private Map<String, List<UserNode>> _userIDtoUserNodeMap;

	private Map<String, User> _userMap;
	private CurrentUser _currentUser = null;

	public MemoryDataModelProvider() {
		_workspaceMap = new HashMap<>();
		_workspaceMemberMap = new HashMap<>();
		_workspaceMessageMap = new HashMap<>();
		_workspaceFileMap = new HashMap<>();
		_masterMessageMap = new HashMap<>();
		_masterFileMap = new HashMap<String, File>();
		_fileVersionsMap = new HashMap<String, List<FileVersion>>();
		_fileVersionsReceiptMap = new HashMap<String, List<FileVersionReceipt>>();
		_fileChunkMap = new HashMap<String, List<FileChunk>>();
		_fileVersionsTagMap = new HashMap<String, List<FileTag>>();
		_userIDtoUserNodeMap = new HashMap<>();

		_userMap = new HashMap<String, User>();
	}

	// -------- Workspace Accessor Methods --------

	public List<Workspace> getWorkspaces() {
		List<Workspace> workspaces = new ArrayList<Workspace>();

		for (Workspace workspace : _workspaceMap.values()) {
			workspaces.add(new Workspace(workspace));
		}

		return workspaces;
	}

	public List<Workspace> getWorkspacesWithStatus(int workspaceStatus) {
		List<Workspace> workspaces = new ArrayList<Workspace>();

		for (Workspace workspace : getWorkspaces()) {
			if (workspace.getStatus() == workspaceStatus)
				workspaces.add(new Workspace(workspace));
		}

		return workspaces;
	}

	public Workspace getWorkspace(long workspaceID)
			throws NoSuchWorkspaceException {
		if (!_workspaceMap.containsKey(workspaceID)) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}
		return new Workspace(_workspaceMap.get(workspaceID));
	}

	public void addWorkspace(Workspace workspace)
			throws WorkspaceAlreadyExistsException {
		if (_workspaceMap.containsKey(workspace.getGuid())) {
			throw new WorkspaceAlreadyExistsException("A workspace with the specified ID already exists");
		}
		Workspace workspaceToSave = new Workspace(workspace);
		List<Member> workspaceMembers = new ArrayList<Member>();
		List<Message> workspaceMessages = new ArrayList<Message>();
		List<File> workspaceFiles = new ArrayList<File>();
		_workspaceMap.put(workspace.getGuid(), workspaceToSave);
		_workspaceMemberMap.put(workspace.getGuid(), workspaceMembers);
		_workspaceMessageMap.put(workspace.getGuid(), workspaceMessages);
		_workspaceFileMap.put(workspace.getGuid(), workspaceFiles);
	}

	public void updateWorkspace(Workspace workspace)
			throws NoSuchWorkspaceException {
		if (!_workspaceMap.containsKey(workspace.getGuid())) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}
		Workspace workspaceToSave = new Workspace(workspace);
		_workspaceMap.put(workspace.getGuid(), workspaceToSave);
	}

	public void removeWorkspace(long workspaceID)
			throws NoSuchWorkspaceException {
		if (!_workspaceMap.containsKey(workspaceID)) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}
		_workspaceMap.remove(workspaceID);
		_workspaceMemberMap.remove(workspaceID);
	}

	// -------- Member Accessor Methods --------

	public List<Member> getMembersInWorkspace(long workspaceID)
			throws NoSuchWorkspaceException {
		List<Member> retMembers = new ArrayList<Member>();

		List<Member> members = _workspaceMemberMap.get(workspaceID);
		if (members == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		for (Member member : members) {
			retMembers.add(new Member(member));
		}

		return retMembers;
	}

	public Member getMemberInWorkspace(long workspaceID, String userID)
			throws NoSuchWorkspaceException, NoSuchMemberException {

		Member retMember = null;

		List<Member> members = _workspaceMemberMap.get(workspaceID);
		if (members == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		boolean memberFound = false;
		Iterator<Member> iterator = members.iterator();
		;
		while (!memberFound && iterator.hasNext()) {
			Member curMember = iterator.next();
			if (curMember.getUserID().equals(userID)) {
				memberFound = true;
				retMember = curMember;
			}
		}

		if (retMember == null) {
			throw new NoSuchMemberException("The requested member is not in this workspace");
		}

		return new Member(retMember);
	}

	public void addMemberToWorkspace(long workspaceID, Member member)
			throws NoSuchWorkspaceException, MemberAlreadyExistsException {
		List<Member> members = _workspaceMemberMap.get(workspaceID);
		if (members == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		if (members.contains(member)) {
			throw new MemberAlreadyExistsException("The specified member already exists in this workspace");
		}

		members.add(new Member(member));
	}

	public void updateMemberInWorkspace(long workspaceID, Member member)
			throws NoSuchWorkspaceException, NoSuchMemberException {
		boolean memberFound = false;

		List<Member> members = _workspaceMemberMap.get(workspaceID);
		if (members == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		Iterator<Member> iterator = members.iterator();
		;
		while (!memberFound && iterator.hasNext()) {
			Member curMember = iterator.next();
			if (curMember.getUserID().equals(member.getUserID())) {
				memberFound = true;
				iterator.remove();
				members.add(new Member(member));
			}
		}

		if (!memberFound) {
			throw new NoSuchMemberException("The requested member is not in this workspace");
		}

	}

	public void removeMemberFromWorkspace(long workspaceID, Member member)
			throws NoSuchWorkspaceException, NoSuchMemberException {
		boolean memberFound = false;

		List<Member> members = _workspaceMemberMap.get(workspaceID);
		if (members == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		Iterator<Member> iterator = members.iterator();
		while (!memberFound && iterator.hasNext()) {
			Member curMember = iterator.next();
			if (curMember.getUserID().equals(member.getUserID())) {
				memberFound = true;
				iterator.remove();
			}
		}

		if (!memberFound) {
			throw new NoSuchMemberException("The requested member is not in this workspace");
		}

	}

	// -------- Message Accessor Methods --------

	public List<Message> getMessagesInWorkspace(long workspaceID)
			throws NoSuchWorkspaceException {
		List<Message> messages = _workspaceMessageMap.get(workspaceID);
		if (messages == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		List<Message> retMessages = new ArrayList<Message>();

		Iterator<Message> iterator = messages.iterator();
		while (iterator.hasNext()) {
			retMessages.add(new Message(iterator.next()));
		}

		return retMessages;
	}

	public Message getMessage(long messageID)
			throws NoSuchMessageException {

		if (!_masterMessageMap.containsKey(messageID)) {
			throw new NoSuchMessageException("No message exists with the requested ID");
		}

		return new Message(_masterMessageMap.get(messageID));
	}

	public void addMessageToWorkspace(long workspaceID, Message message)
			throws NoSuchWorkspaceException, MessageAlreadyExistsException {

		List<Message> messages = _workspaceMessageMap.get(workspaceID);
		if (messages == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		if (_masterMessageMap.containsKey(message.getGuid())) {
			throw new MessageAlreadyExistsException("This message already exists");
		}

		Message messageToAdd = new Message(message);
		_masterMessageMap.put(message.getGuid(), messageToAdd);
		messages.add(messageToAdd);
	}

	public void updateMessageInWorkspace(long workspaceID, Message message)
			throws NoSuchWorkspaceException, NoSuchMessageException {

		List<Message> messages = _workspaceMessageMap.get(workspaceID);
		if (messages == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		boolean messageFound = false;
		Iterator<Message> iterator = messages.iterator();
		while (iterator.hasNext()) {
			Message curMessage = iterator.next();
			if (curMessage.getGuid() == message.getGuid()) {
				iterator.remove();
				Message messageToAdd = new Message(message);
				_masterMessageMap.put(message.getGuid(), messageToAdd);
				messages.add(messageToAdd);
				messageFound = true;
				break;
			}
		}

		if (!messageFound) {
			throw new NoSuchMessageException("The requested message is not in this workspace ");
		}
	}

	public void removeMessageFromWorkspace(long workspaceID, Message message)
			throws NoSuchWorkspaceException, NoSuchMessageException {
		List<Message> messages = _workspaceMessageMap.get(workspaceID);
		if (messages == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the requested ID");
		}

		if (!_masterMessageMap.containsKey(message.getGuid())) {
			throw new NoSuchMessageException("No message exists with the requested ID");
		}

		_masterMessageMap.remove(message.getGuid());
		messages.remove(message);
	}

	// -------- File Accessor Methods --------

	public List<File> getFilesInWorkspace(long workspaceID)
			throws NoSuchWorkspaceException {

		List<File> files = _workspaceFileMap.get(workspaceID);
		if (files == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the specified ID");
		}

		List<File> retFiles = new ArrayList<File>();
		for (File curFile : files) {
			retFiles.add(new File(curFile));
		}

		return retFiles;
	}

	public List<File> getFilesInWorkspace(long workspaceID, String parentID)
			throws NoSuchWorkspaceException {
		List<File> files = _workspaceFileMap.get(workspaceID);
		if (files == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the specified ID");
		}

		List<File> retFiles = new ArrayList<File>();
		for (File curFile : files) {
			if ((curFile.getParentID() != null && curFile.getParentID().equals(parentID)) ||
					(curFile.getParentID() == null && parentID == null)) {
				retFiles.add(new File(curFile));
			}
		}

		return retFiles;
	}

	public File getFile(String fileID)
			throws NoSuchFileException {
		File retFile;

		retFile = _masterFileMap.get(fileID);

		if (retFile == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		return retFile;
	}

	public void addFileToWorkspace(long workspaceID, File file)
			throws NoSuchWorkspaceException, FileAlreadyExistsException {
		List<File> files = _workspaceFileMap.get(workspaceID);
		if (files == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the specified ID");
		}

		if (_masterFileMap.containsKey(file.getEntryID())) {
			throw new FileAlreadyExistsException("The file already exists");
		}

		File fileToAdd = new File(file);
		files.add(file);
		_masterFileMap.put(file.getEntryID(), fileToAdd);
		_fileVersionsMap.put(file.getEntryID(), new ArrayList<FileVersion>());
	}

	public void updateFileInWorkspace(long workspaceID, File file)
			throws NoSuchWorkspaceException, NoSuchFileException {
		List<File> files = _workspaceFileMap.get(workspaceID);
		if (files == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the specified ID");
		}

		_masterFileMap.remove(file.getEntryID());

		boolean fileFound = false;
		Iterator<File> iterator = files.iterator();
		while (iterator.hasNext()) {
			File curFile = iterator.next();
			if (curFile.getEntryID().equals(file.getEntryID())) {
				iterator.remove();
				_masterFileMap.put(file.getEntryID(), file);
				files.add(file);
				fileFound = true;
				break;
			}
		}

		if (!fileFound) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}
	}

	public void removeFileFromWorkspace(long workspaceID, String fileID)
			throws NoSuchWorkspaceException, NoSuchFileException {
		List<File> files = _workspaceFileMap.get(workspaceID);
		if (files == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the specified ID");
		}

		File file = _masterFileMap.remove(fileID);

		if (file == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		boolean fileDeleted = false;
		Iterator<File> iterator = files.iterator();
		while (iterator.hasNext()) {
			File curFile = iterator.next();
			if (curFile.getEntryID().equals(fileID)) {
				iterator.remove();
				fileDeleted = true;
				break;
			}
		}

		if (!fileDeleted) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}
	}

	public void removeFileFromWorkspace(long workspaceID, File file)
			throws NoSuchWorkspaceException, NoSuchFileException {
		List<File> files = _workspaceFileMap.get(workspaceID);
		if (files == null) {
			throw new NoSuchWorkspaceException("No workspace exists with the specified ID");
		}

		_masterFileMap.remove(file.getEntryID());

		boolean fileDeleted = false;
		Iterator<File> iterator = files.iterator();
		while (iterator.hasNext()) {
			File curFile = iterator.next();
			if (curFile.getEntryID().equals(file.getEntryID())) {
				iterator.remove();
				fileDeleted = true;
				break;
			}
		}

		if (!fileDeleted) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}
	}

	// -------- File Version Accessor Methods --------

	public List<String> getAvailableVersionsOfFile(String fileID)
			throws NoSuchFileException {

		List<FileVersion> fileVersions = _fileVersionsMap.get(fileID);
		if (fileVersions == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		List<String> versionIDs = new ArrayList<String>();

		for (FileVersion curVersion : fileVersions) {
			versionIDs.add(curVersion.getVersionID());
		}

		return versionIDs;
	}

	public List<FileVersion> getFileVersionsForFile(String fileID)
			throws NoSuchFileException {

		List<FileVersion> fileVersions = _fileVersionsMap.get(fileID);
		if (fileVersions == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		List<FileVersion> versionToReturn = new ArrayList<FileVersion>();

		for (FileVersion curVersion : fileVersions) {
			versionToReturn.add(new FileVersion(curVersion));
		}

		return versionToReturn;
	}

	public FileVersion getFileVersion(String fileID, String versionID)
			throws NoSuchFileException, NoSuchFileVersionException {

		List<FileVersion> fileVersions = _fileVersionsMap.get(fileID);
		if (fileVersions == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		FileVersion versionToReturn = null;

		boolean versionFound = false;
		for (FileVersion curVersion : fileVersions) {
			if (curVersion.getVersionID().equals(versionID)) {
				versionToReturn = curVersion;
				versionFound = true;
				break;
			}
		}

		if (!versionFound) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		return new FileVersion(versionToReturn);
	}

	public void addFileVersion(String fileID, FileVersion fileVersion)
			throws NoSuchFileException, FileVersionAlreadyExistsException {

		List<FileVersion> fileVersions = _fileVersionsMap.get(fileID);
		if (fileVersions == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		boolean versionFound = false;
		for (FileVersion curVersion : fileVersions) {
			if (curVersion.getVersionID().equals(fileVersion.getVersionID())) {
				versionFound = true;
				break;
			}
		}

		if (versionFound) {
			throw new FileVersionAlreadyExistsException("The specified file version already exists");
		}

		fileVersions.add(new FileVersion(fileVersion));
		_fileChunkMap.put(createVersionKey(fileID, fileVersion.getVersionID()), new ArrayList<FileChunk>());
		_fileVersionsReceiptMap.put(createVersionKey(fileID, fileVersion.getVersionID()), new ArrayList<FileVersionReceipt>());
		_fileVersionsTagMap.put(createVersionKey(fileID, fileVersion.getVersionID()), new ArrayList<FileTag>());
	}

	public void updateFileVersion(String fileID, FileVersion fileVersion)
			throws NoSuchFileException, NoSuchFileVersionException {

		List<FileVersion> fileVersions = _fileVersionsMap.get(fileID);
		if (fileVersions == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		FileVersion versionToReturn = null;

		boolean versionUpdated = false;
		Iterator<FileVersion> iterator = fileVersions.iterator();
		while (iterator.hasNext()) {
			FileVersion curVersion = iterator.next();
			if (curVersion.getVersionID().equals(fileVersion.getVersionID())) {
				iterator.remove();
				fileVersions.add(new FileVersion(fileVersion));
				versionUpdated = true;
				break;
			}
		}

		if (!versionUpdated) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}
	}

	public void removeFileVersion(String fileID, String versionID)
			throws NoSuchFileException, NoSuchFileVersionException {

		List<FileVersion> fileVersions = _fileVersionsMap.get(fileID);
		if (fileVersions == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		FileVersion versionToReturn = null;

		boolean versionRemoved = false;
		Iterator<FileVersion> iterator = fileVersions.iterator();
		while (iterator.hasNext()) {
			FileVersion curVersion = iterator.next();
			if (curVersion.getVersionID().equals(versionID)) {
				iterator.remove();
				String versionKey = createVersionKey(fileID, versionID);
				_fileChunkMap.remove(versionKey);
				_fileVersionsReceiptMap.remove(versionKey);
				_fileVersionsTagMap.remove(versionKey);
				versionRemoved = true;
				break;
			}
		}

		if (!versionRemoved) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}
	}

	public void removeFileVersion(String fileID, FileVersion fileVersion)
			throws NoSuchFileException, NoSuchFileVersionException {

		List<FileVersion> fileVersions = _fileVersionsMap.get(fileID);
		if (fileVersions == null) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		FileVersion versionToReturn = null;

		boolean versionRemoved = false;
		Iterator<FileVersion> iterator = fileVersions.iterator();
		while (iterator.hasNext()) {
			FileVersion curVersion = iterator.next();
			if (curVersion.getVersionID().equals(fileVersion.getVersionID())) {
				iterator.remove();
				String versionKey = createVersionKey(fileID, fileVersion.getVersionID());
				_fileChunkMap.remove(versionKey);
				_fileVersionsReceiptMap.remove(versionKey);
				_fileVersionsTagMap.remove(versionKey);
				versionRemoved = true;
				break;
			}
		}

		if (!versionRemoved) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}
	}

	// -------- File Version Receipt Accessor Methods --------

	public List<FileVersionReceipt> getFileVersionReceipts(String fileID, String versionID)
			throws NoSuchFileException, NoSuchFileVersionException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileVersionReceipt> receipts = _fileVersionsReceiptMap.get(versionKey);

		if (receipts == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		List<FileVersionReceipt> receiptsToReturn = new ArrayList<FileVersionReceipt>();

		for (FileVersionReceipt receipt : receipts) {
			receiptsToReturn.add(new FileVersionReceipt(receipt));
		}

		return receipts;
	}

	public void addFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
			throws NoSuchFileException, NoSuchFileVersionException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileVersionReceipt> receipts = _fileVersionsReceiptMap.get(versionKey);

		if (receipts == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		receipts.add(new FileVersionReceipt(receipt));
	}

	public void updateFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
			throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileVersionReceipt> receipts = _fileVersionsReceiptMap.get(versionKey);

		if (receipts == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		boolean receiptFound = false;
		Iterator<FileVersionReceipt> iterator = receipts.iterator();
		while (iterator.hasNext()) {
			FileVersionReceipt curReceipt = iterator.next();
			if (curReceipt.getRecipientID().equals(receipt.getRecipientID())) {
				iterator.remove();
				receipts.add(new FileVersionReceipt(receipt));
				receiptFound = true;
				break;
			}
		}

		if (!receiptFound) {
			throw new NoSuchFileVersionReceiptException("The specified receipt does not exist");
		}
	}

	public void removeFileVersionReceipt(String fileID, String versionID, FileVersionReceipt receipt)
			throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileVersionReceiptException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileVersionReceipt> receipts = _fileVersionsReceiptMap.get(versionKey);

		if (receipts == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		boolean receiptRemoved = false;
		Iterator<FileVersionReceipt> iterator = receipts.iterator();
		while (iterator.hasNext()) {
			FileVersionReceipt curReceipt = iterator.next();
			if (curReceipt.getRecipientID().equals(receipt.getRecipientID())) {
				iterator.remove();
				receiptRemoved = true;
				break;
			}
		}

		if (!receiptRemoved) {
			throw new NoSuchFileVersionReceiptException("The specified receipt does not exist");
		}
	}

	// -------- File Chunk Accessor Methods --------

	public List<FileChunk> getChunksForFileVersion(String fileID, String versionID)
			throws NoSuchFileException, NoSuchFileVersionException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileChunk> chunks = _fileChunkMap.get(versionKey);
		if (chunks == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		List<FileChunk> retChunks = new ArrayList<FileChunk>();
		for (FileChunk curChunk : chunks) {
			retChunks.add(new FileChunk(curChunk));
		}

		return retChunks;
	}

	public void addChunkForFile(String fileID, String versionID, FileChunk chunk)
			throws NoSuchFileException, NoSuchFileVersionException, FileChunkAlreadyExistsException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileChunk> chunks = _fileChunkMap.get(versionKey);
		if (chunks == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		boolean chunkFound = false;
		Iterator<FileChunk> iterator = chunks.iterator();
		while (iterator.hasNext()) {
			FileChunk curChunk = iterator.next();
			if (curChunk.getChunkID().equals(chunk.getChunkID())) {
				chunkFound = true;
				break;
			}
		}

		if (chunkFound) {
			throw new FileChunkAlreadyExistsException("The specified file chunk already exists");
		}

		chunks.add(new FileChunk(chunk));
	}

	public void updateChunkForFile(String fileID, String versionID, FileChunk chunk)
			throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileChunk> chunks = _fileChunkMap.get(versionKey);
		if (chunks == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		boolean chunkUpdated = false;
		Iterator<FileChunk> iterator = chunks.iterator();
		while (iterator.hasNext()) {
			FileChunk curChunk = iterator.next();
			if (curChunk.getChunkID().equals(chunk.getChunkID())) {
				iterator.remove();
				chunks.add(new FileChunk(chunk));
				chunkUpdated = true;
				break;
			}
		}

		if (!chunkUpdated) {
			throw new NoSuchFileChunkException("No chunk exists with the specified ID");
		}
	}

	public void removeChunkForFile(String fileID, String versionID, FileChunk chunk)
			throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileChunkException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileChunk> chunks = _fileChunkMap.get(versionKey);
		if (chunks == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		boolean chunkRemoved = false;
		Iterator<FileChunk> iterator = chunks.iterator();
		while (iterator.hasNext()) {
			FileChunk curChunk = iterator.next();
			if (curChunk.getChunkID().equals(chunk.getChunkID())) {
				iterator.remove();
				chunkRemoved = true;
				break;
			}
		}

		if (!chunkRemoved) {
			throw new NoSuchFileChunkException("No chunk exists with the specified ID");
		}
	}

	// -------- File Tag Accessor Methods --------

	public List<FileTag> getTagsForFileVersion(String fileID, String versionID)
			throws NoSuchFileException, NoSuchFileVersionException {

		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileTag> tags = _fileVersionsTagMap.get(versionKey);
		if (tags == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		List<FileTag> tagsToReturn = new ArrayList<FileTag>();
		for (FileTag curTag : tags) {
			tagsToReturn.add(new FileTag(curTag));
		}

		return tagsToReturn;
	}

	public void addTagForFile(String fileID, String versionID, FileTag tag)
			throws NoSuchFileException, NoSuchFileVersionException, FileTagAlreadyExistsException {
		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileTag> tags = _fileVersionsTagMap.get(versionKey);
		if (tags == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		boolean tagFound = false;
		for (FileTag curTag : tags) {
			if (curTag.equals(tag)) {
				tagFound = true;
				break;
			}
		}

		if (tagFound) {
			throw new FileTagAlreadyExistsException("The specified tag already exists");
		}

		tags.add(new FileTag(tag));
	}

	public void removeTagForFile(String fileID, String versionID, FileTag tag)
			throws NoSuchFileException, NoSuchFileVersionException, NoSuchFileTagException {
		if (!_masterFileMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		if (!_fileVersionsMap.containsKey(fileID)) {
			throw new NoSuchFileException("No file exists with the specified ID");
		}

		String versionKey = createVersionKey(fileID, versionID);
		List<FileTag> tags = _fileVersionsTagMap.get(versionKey);
		if (tags == null) {
			throw new NoSuchFileVersionException("No file version exists with the specified ID");
		}

		boolean tagFound = false;
		Iterator<FileTag> iterator = tags.iterator();
		while (iterator.hasNext()) {
			FileTag curTag = iterator.next();
			if (curTag.equals(tag)) {
				iterator.remove();
				tagFound = true;
				break;
			}
		}

		if (!tagFound) {
			throw new NoSuchFileTagException("No file tag with the specified ID exists");
		}
	}

	// -------- User Accessor Methods --------

	public List<User> getUsers() {

		List<User> usersToReturn = new ArrayList<User>();

		for (User curUser : _userMap.values()) {
			usersToReturn.add(new User(curUser));
		}

		return usersToReturn;
	}

	public User getUserByID(String userID)
			throws NoSuchUserException {

		User user = _userMap.get(userID);

		if (user == null) {
			throw new NoSuchUserException("No user exists with the specified ID");
		}

		return new User(user);
	}

	public User getUserByEmail(String email)
			throws NoSuchUserException {

		User user = null;

		for (User curUser : _userMap.values()) {
			if (curUser.getEmail().equals(email)) {
				user = curUser;
				break;
			}
		}

		if (user == null) {
			throw new NoSuchUserException("No user exists with the specified ID");
		}

		return new User(user);
	}

	public void addUser(User user)
			throws UserAlreadyExistsException {

		if (_userMap.containsKey(user.getUserID())) {
			throw new UserAlreadyExistsException("The specified user already exists");
		}

		_userMap.put(user.getUserID(), new User(user));
	}

	public void updateUser(User user)
			throws NoSuchUserException {

		boolean userFound = false;
		Iterator<User> iterator = _userMap.values().iterator();
		while (iterator.hasNext()) {
			User curUser = iterator.next();
			if (curUser.getUserID().equals(user.getUserID())) {
				_userMap.put(user.getUserID(), new User(user));
				userFound = true;
				break;
			}
		}

		if (!userFound) {
			throw new NoSuchUserException("No user exists with the specified ID");
		}
	}

	public void removeUser(User user)
			throws NoSuchUserException {

		if (_userMap.remove(user.getUserID()) == null) {
			throw new NoSuchUserException("No user exists with the specified ID");
		}
	}

	public void removeUser(String userID)
			throws NoSuchUserException {

		User user = null;

		if (_userMap.remove(userID) == null) {
			throw new NoSuchUserException("No user exists with the specified ID");
		}
	}

	@Override public CurrentUser getCurrentUser() throws NoSuchUserException {
		if (_currentUser != null) {
			return _currentUser;
		}
		throw new NoSuchUserException();
	}

	@Override public void setCurrentUser(CurrentUser user) {
		_currentUser = user;
	}

	@Override public void removeCurrentUser() {
		_currentUser = null;
	}

	@Override public void addUserNode(UserNode userNode) {
		List<UserNode> memberNodes = _userIDtoUserNodeMap.get(userNode.getUserID());
		if (memberNodes == null) {
			memberNodes = new ArrayList<UserNode>();
		}
		memberNodes.add(userNode);
		_userIDtoUserNodeMap.put(userNode.getUserID(), memberNodes);
	}

	@Override public void removeUserNode(String userID, UserNode userNode) {
		List<UserNode> memberNodes = _userIDtoUserNodeMap.get(userID);
		if (memberNodes != null) {
			memberNodes.remove(userNode);
		}
	}

	public List<UserNode> getUserNodesForUserID(String userID) {
		return _userIDtoUserNodeMap.get(userID);
	}

	@Override public Workspace getWorkspaceByMyAuthToken(String authToken) throws BadAuthTokenException {
		for (Long workspace : _workspaceMemberMap.keySet()) {
			for (Member m : _workspaceMemberMap.get(workspace)) {
				if (m.getAuthToken().equals(authToken) && m.getUserID().equals(_currentUser.getUserID())) {
					try {
						return getWorkspace(workspace);
					} catch (NoSuchWorkspaceException e) {
						e.printStackTrace();
					}
				}
			}
		}
		throw new BadAuthTokenException();
	}

	@Override public boolean hasChunkInWorkspace(String chunkID, long workspaceGuid) {
		try {
			List<File> files = getFilesInWorkspace(workspaceGuid);
			for (File file : files) {
				List<FileVersion> versions = getFileVersionsForFile(file.getEntryID());
				for (FileVersion version : versions) {
					List<FileChunk> chunks = getChunksForFileVersion(file.getEntryID(), version.getVersionID());
					for (FileChunk chunk : chunks) {
						if (chunk.getChunkID().equals(chunkID)) {
							return true;
						}
					}
				}
			}
		} catch (NoSuchWorkspaceException | NoSuchFileException | NoSuchFileVersionException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override public List<String> hasChunksInWorkspace(List<String> chunkIDs, long workspaceGuid) {
		Set<String> allChunkIDsIHave = new HashSet<String>();
		try {
			List<File> files = getFilesInWorkspace(workspaceGuid);
			for (File file : files) {
				List<FileVersion> versions = getFileVersionsForFile(file.getEntryID());
				for (FileVersion version : versions) {
					List<FileChunk> chunks = getChunksForFileVersion(file.getEntryID(), version.getVersionID());
					for (FileChunk chunk : chunks) {
						allChunkIDsIHave.add(chunk.getChunkID());
					}
				}
			}
			List<String> toReturn = new ArrayList<String>(chunkIDs);
			toReturn.retainAll(allChunkIDsIHave);
			return toReturn;
		} catch (NoSuchWorkspaceException | NoSuchFileException | NoSuchFileVersionException e) {
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}

	// -------- Private Methods --------

	private String createVersionKey(String fileID, String versionID) {
		return fileID + ":" + versionID;
	}
}
