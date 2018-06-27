package io.topiacoin.dht;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.exceptions.NotLoggedInException;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.crypto.HashUtils;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DHTWorkspaceEntry;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.Member;
import io.topiacoin.model.MemberNode;
import io.topiacoin.model.User;
import io.topiacoin.model.exceptions.NoSuchMemberException;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * The SDFS DHT Accessor is the middle layer between the raw DHT functions and the higher-order SDFS functionality. General usage is as follows:
 * When a User logs into SDFS, they will need to
 * 1) fetch the list of workspaces they belong to from the DHT via {@link #fetchMyDHTWorkspaces()}, which returns a list of {@link DHTWorkspaceEntry}
 * 2) Externally, they should connect to the blockchains and sync the workspaces' metadata using nodes found in {@link DHTWorkspaceEntry#getMemberNodes()}
 * 3) If they are a full member of the Workspace, they should add their Member Node to the DHT, via {@link #addMyMemberNode(DHTWorkspaceEntry, MemberNode)}
 * When the User wants to create a workspace, the {@link #addNewWorkspaceToDHT(long)} function should be called (which returns a {@link DHTWorkspaceEntry})
 * When the User wishes to invite another user to the Workspace, the {@link #addInvitation(DHTWorkspaceEntry, User)} function should be called
 * When the User wishes to remove a Member from the Workspace, the {@link #removeMemberFromWorkspace(DHTWorkspaceEntry, Member)} function should be called
 * When the User wishes to leave a Workspace, the {@link #leaveWorkspace(DHTWorkspaceEntry)} function should be called
 * When the User wants to log out of SDFS, {@link #stop()} should be called
 * If, for whatever reason, the User wants to disable Blockchain sharing, {@link #removeMyMemberNode(DHTWorkspaceEntry)} should be called
 */
public class SDFSDHTAccessor {
	private static final Log _log = LogFactory.getLog(SDFSDHTAccessor.class);
	private DHT _dht;
	private DataModel _model;
	private boolean running = true;
	private static SDFSDHTAccessor _instance = null;

	public static SDFSDHTAccessor getInstance(Configuration config, DataModel model) {
		synchronized (SDFSDHTAccessor.class) {
			if (_instance == null) {
				_instance = new SDFSDHTAccessor(config, model);
			}
		}
		return _instance;
	}

	SDFSDHTAccessor(Configuration config, DataModel model) {
		DHTConfiguration _dhtConfig = new DHTConfiguration(config);
		_model = model;
		try {
			KeyPair DHTKeyPair = CryptoUtils.generateECKeyPair();
			_dht = new DHT(0, DHTKeyPair, _dhtConfig);
			NodeID nid = new NodeID(_dhtConfig.getBootstrapNodeID(), null);
			Node node = new Node(nid, _dhtConfig.getBootstrapNodeHostname(), _dhtConfig.getBootstrapNodePort());
			_dht.bootstrap(node);
		} catch (CryptographicException | NoSuchAlgorithmException | IOException e) {
			throw new RuntimeException("Failed to initialize DHT", e);
		}
	}

	/**
	 * Stops the system, removing all of my Member Nodes before doing so.
	 */
	public void stop() {
		if (running) {
			running = false;
			try {
				List<DHTWorkspaceEntry> workspaces = fetchMyDHTWorkspaces();
				for (DHTWorkspaceEntry workspace : workspaces) {
					removeMyMemberNode(workspace);
				}
			} catch (NotLoggedInException e) {
				_log.error("Failed to clean up DHT", e);
			}
			_dht.shutdown(true);
		}
	}

	/**
	 * Fetches a list of all of the Workspaces I'm in, according to the DHT, expressed through the {@link DHTWorkspaceEntry} model object
	 * @return a list of all of the Workspaces I'm in, according to the DHT
	 * @throws NotLoggedInException If the current user cannot be ascertained (read: not logged in)
	 */
	public List<DHTWorkspaceEntry> fetchMyDHTWorkspaces() throws NotLoggedInException {
		try {
			List<Long> ids = fetchMyWorkspaceIDs();
			List<DHTWorkspaceEntry> tr = new ArrayList<>(ids.size());
			for (long id : ids) {
				SecretKey nodeKey = fetchMyWorkspaceNodeKey(id);
				List<MemberNode> nodes = fetchMemberNodes(id, nodeKey);
				tr.add(new DHTWorkspaceEntry(id, nodeKey, nodes));
			}
			return tr;
		} catch (NoSuchUserException e) {
			throw new NotLoggedInException(e);
		}
	}

	/**
	 * Fetches the Workspace whose ID is specified (if it exists and I'm a member of it), according to the DHT, expressed through the {@link DHTWorkspaceEntry} model object
	 * @return the Workspace whose ID is specified (if it exists and I'm a member of it), according to the DHT, or null
	 * @throws NotLoggedInException If the current user cannot be ascertained (read: not logged in)
	 */
	public DHTWorkspaceEntry fetchDHTWorkspace(long workspaceID) throws NotLoggedInException {
		try {
			SecretKey nodeKey = fetchMyWorkspaceNodeKey(workspaceID);
			if(nodeKey == null) {
				return null;
			}
			List<MemberNode> nodes = fetchMemberNodes(workspaceID, nodeKey);
			return new DHTWorkspaceEntry(workspaceID, nodeKey, nodes);
		} catch (NoSuchUserException e) {
			throw new NotLoggedInException(e);
		}
	}

	/**
	 * Adds a {@link MemberNode} for the given {@link DHTWorkspaceEntry}
	 * The Blockchain subsystem should be able to produce a MemberNode for a Workspace Blockchain - once it does, it should
	 * be added to the DHT via this method.
	 * @param dhtWorkspace The DHTWorkspaceEntry to add a MemberNode to
	 * @param myNode The MemberNode object containing information to connect to your Blockchain instance
	 * @return true if it was added successfully, false otherwise
	 * @throws NotLoggedInException If the current user cannot be ascertained (read: not logged in)
	 */
	public boolean addMyMemberNode(DHTWorkspaceEntry dhtWorkspace, MemberNode myNode) throws NotLoggedInException {
		try {
			return addMyMemberNode(dhtWorkspace.getWorkspaceID(), myNode, dhtWorkspace.getDhtKey());
		} catch (NoSuchUserException e) {
			throw new NotLoggedInException(e);
		}
	}

	/**
	 * Removes the current user's {@link MemberNode} for the given {@link DHTWorkspaceEntry}
	 * Should the Blockchain subsystem shut a workspace blockchain down, this method should be called.
	 * @param dhtWorkspace The DHTWorkspaceEntry to remove the current user's MemberNode from
	 * @return true if it was removed successfully, false otherwise
	 * @throws NotLoggedInException If the current user cannot be ascertained (read: not logged in)
	 */
	public boolean removeMyMemberNode(DHTWorkspaceEntry dhtWorkspace) throws NotLoggedInException {
		try {
			return removeMyMemberNode(dhtWorkspace.getWorkspaceID());
		} catch (NoSuchUserException e) {
			throw new NotLoggedInException(e);
		}
	}

	/**
	 * Given a WorkspaceID, adds a new {@link DHTWorkspaceEntry} to the DHT and returns it.
	 * This method should be called when a user wants to create a new Workspace.
	 * @param workspaceID The ID of the workspace
	 * @return the DHTWorkspaceEntry that was pushed onto the DHT
	 * @throws NotLoggedInException If the current user cannot be ascertained (read: not logged in)
	 */
	public DHTWorkspaceEntry addNewWorkspaceToDHT(long workspaceID) throws NotLoggedInException {
		try {
			SecretKey dhtKey = addNewWorkspaceToDHTInternal(workspaceID);
			List<MemberNode> nodes = fetchMemberNodes(workspaceID, dhtKey);
			return new DHTWorkspaceEntry(workspaceID, dhtKey, nodes);
		} catch (NoSuchUserException e) {
			throw new NotLoggedInException(e);
		}
	}

	/**
	 * Given a {@link DHTWorkspaceEntry} and a {@link User} model object, invite that User to the Workspace (in the context of the DHT)
	 * This method should be called when a user wants to invite another user to a Workspace
	 * @param dhtWorkspace the DHTWorkspaceEntry to invite the User to
	 * @param invitee the User to invite
	 * @throws IOException if the invitation add fails
	 */
	public void addInvitation(DHTWorkspaceEntry dhtWorkspace, User invitee) throws IOException {
		try {
			addInvitation(dhtWorkspace.getWorkspaceID(), invitee, dhtWorkspace.getDhtKey());
		} catch (CryptographicException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Given a {@link DHTWorkspaceEntry}, removes the current User from the workspace (in the context of the DHT)
	 * This method should be called when a user wants to leave a Workspace
	 * @param dhtWorkspace The workspace to Leave
	 * @throws NotLoggedInException If the current user cannot be ascertained (read: not logged in)
	 */
	public void leaveWorkspace(DHTWorkspaceEntry dhtWorkspace) throws NotLoggedInException {
		try {
			leaveWorkspace(dhtWorkspace.getWorkspaceID());
		} catch (NoSuchUserException e) {
			throw new NotLoggedInException(e);
		}
	}

	/**
	 * Given a {@link DHTWorkspaceEntry} and a Workspace {@link Member}, removes the member from the workspace (in the context of the DHT)
	 * This method should be called when a user wants to remove another member from the Workspace
	 * @param dhtWorkspace The workspace to remove the member from
	 * @param member The member to remove
	 * @throws NoSuchUserException If the member's User information cannot be found
	 */
	public void removeMemberFromWorkspace(DHTWorkspaceEntry dhtWorkspace, Member member) throws NoSuchUserException {
		removeMemberFromWorkspace(dhtWorkspace.getWorkspaceID(), member);
	}

	List<Long> fetchMyWorkspaceIDs() throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		String dhtKey = HashUtils.sha256String(me.getUserID());
		Set<String> encryptedWorkspaceIDs = _dht.fetchContent(dhtKey);
		List<Long> workspaceIDs = new ArrayList<>();
		for (String encryptedWksID : encryptedWorkspaceIDs) {
			try {
				String wID = new String(CryptoUtils.decryptWithPrivateKey(encryptedWksID.split("\n")[1], me.getPrivateKey()));
				workspaceIDs.add(Long.parseLong(wID));
			} catch (CryptographicException e) {
				_log.warn("Could not decrypt a WorkspaceID - removing it");
				_dht.removeContent(dhtKey, encryptedWksID);
			}
		}
		return workspaceIDs;
	}

	SecretKey addNewWorkspaceToDHTInternal(long workspaceID) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		try {
			SecretKey newWorkspaceNodeKey = CryptoUtils.generateAESKey();
			addInvitation(workspaceID, me, newWorkspaceNodeKey);
			return newWorkspaceNodeKey;
		} catch (CryptographicException e) {
			_log.error("Internal error creating workspace", e);
			return null;
		}
	}

	void addInvitation(long workspaceID, User invitee, SecretKey workspaceNodeKey) throws CryptographicException {
		//Store the nodeKey for the new user
		String dhtKey = buildWorkspaceNodeKeyDHTKey(workspaceID, invitee);
		String dhtValue = Base64.getEncoder().encodeToString(CryptoUtils.encryptWithPublicKey(workspaceNodeKey.getEncoded(), invitee.getPublicKey()));
		_dht.storeContent(dhtKey, dhtValue);
		//Store the workspaceID for the user
		dhtKey = HashUtils.sha256String(invitee.getUserID());
		dhtValue = CryptoUtils.encryptWithPublicKeyToString("" + workspaceID, invitee.getPublicKey());
		String hash = HashUtils.sha256String(HashUtils.sha256String("" + workspaceID));
		_dht.storeContent(dhtKey, hash + "\n" + dhtValue);
	}

	void leaveWorkspace(long workspaceID) throws NoSuchUserException {
		User me = _model.getCurrentUser();
		try {
			removeMemberFromWorkspace(workspaceID, _model.getMemberInWorkspace(workspaceID, me.getUserID()));
		} catch (NoSuchWorkspaceException | NoSuchMemberException e) {
			_log.warn("Encountered an issue leaving workspace", e);
			throw new IllegalArgumentException("No such Workspace", e);
		}
	}

	void removeMemberFromWorkspace(long workspaceID, Member member) throws NoSuchUserException {
		//Fetch the User's workspaceIDs and remove this workspace from the list
		User user = _model.getUserByID(member.getUserID());
		String dhtKey = HashUtils.sha256String(user.getUserID());
		String hash = HashUtils.sha256String(HashUtils.sha256String("" + workspaceID));
		Set<String> values = _dht.fetchContent(dhtKey);
		for (String value : values) {
			String[] split = value.split("\n");
			if (split.length == 2) {
				if (split[0].equals(hash)) {
					_dht.removeContent(dhtKey, value);
				}
			} else {
				_log.warn("Found invalid data in the DHT - removing it");
				_dht.removeContent(dhtKey, value);
			}
		}
		//Fetch the user's Node Key (or keys - but that would imply bigger issues...) and remove it
		dhtKey = buildWorkspaceNodeKeyDHTKey(workspaceID, user);
		Set<String> encWksNodeKeyStrs = _dht.fetchContent(dhtKey);
		for (String keyStr : encWksNodeKeyStrs) {
			_dht.removeContent(dhtKey, keyStr);
		}
		//Fetch the workspace's nodes, and remove this user's nodes from the list
		removeUserMemberNode(workspaceID, user);
	}

	List<MemberNode> fetchMemberNodes(long workspaceID, SecretKey workspaceNodeKey) {
		String dhtKey = HashUtils.sha256String("" + workspaceID);
		Set<String> encryptedMemberNodes = _dht.fetchContent(dhtKey);
		List<MemberNode> memberNodes = new ArrayList<>();
		for (String encryptedMemberNode : encryptedMemberNodes) {
			try {
				memberNodes.add(new MemberNode(CryptoUtils.decryptStringWithSecretKey(encryptedMemberNode.split("\n")[1], workspaceNodeKey)));
			} catch (CryptographicException e) {
				_log.warn("Could not decrypt a Member Node - removing it");
				_dht.removeContent(dhtKey, encryptedMemberNode);
			}
		}
		return memberNodes;
	}

	boolean addMyMemberNode(long workspaceID, MemberNode memberNode, SecretKey workspaceNodeKey) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		if (!me.getUserID().equals(memberNode.getUserID())) {
			throw new IllegalArgumentException("Will not add a MemberNode for somebody other than myself - THAT WOULD BE EVIL");
		}
		if (!fetchMemberNodes(workspaceID, workspaceNodeKey).contains(memberNode)) {
			String dhtKey = HashUtils.sha256String("" + workspaceID);
			String hash = HashUtils.sha256String(HashUtils.sha256String(me.getUserID()));
			try {
				String encryptedMemberNode = CryptoUtils.encryptStringWithSecretKey(memberNode.toDHTString(), workspaceNodeKey);
				_dht.storeContent(dhtKey, hash + "\n" + encryptedMemberNode);
				return true;
			} catch (CryptographicException e) {
				_log.error("Internal error adding member node", e);
			}
		}
		return false;
	}

	boolean removeMyMemberNode(long workspaceID) throws NoSuchUserException {
		return removeUserMemberNode(workspaceID, _model.getCurrentUser());
	}

	private boolean removeUserMemberNode(long workspaceID, User user) {
		boolean tr = false;
		String dhtKey = HashUtils.sha256String("" + workspaceID);
		String hash = HashUtils.sha256String(HashUtils.sha256String(user.getUserID()));
		Set<String> values = _dht.fetchContent(dhtKey);
		for (String value : values) {
			String[] split = value.split("\n");
			if (split.length == 2) {
				if (split[0].equals(hash)) {
					_dht.removeContent(dhtKey, value);
					tr = true;
				}
			} else {
				_log.warn("Found invalid data in the DHT - removing it");
				_dht.removeContent(dhtKey, value);
			}
		}
		return tr;
	}

	public SecretKey fetchMyWorkspaceNodeKey(long workspaceID) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		String dhtKey = buildWorkspaceNodeKeyDHTKey(workspaceID, _model.getCurrentUser());
		Set<String> encWksNodeKeyStrs = _dht.fetchContent(dhtKey);
		for (String encWksNodeKeyStr : encWksNodeKeyStrs) {
			try {
				return CryptoUtils.getAESKeyFromEncodedBytes(CryptoUtils.decryptWithPrivateKey(encWksNodeKeyStr, me.getPrivateKey()));
			} catch (CryptographicException e) {
				_log.warn("Found an invalid Workspace Node Key for " + workspaceID + " - removing it");
				_dht.removeContent(dhtKey, encWksNodeKeyStr);
			}
		}
		_log.error("Couldn't find a Workspace Node Key for " + workspaceID);
		return null;
	}

	private String buildWorkspaceNodeKeyDHTKey(long workspaceID, User user) {
		String pkString = Base64.getEncoder().encodeToString(user.getPublicKey().getEncoded());
		return HashUtils.sha256String("" + workspaceID + pkString);
	}
}
