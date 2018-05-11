package io.topiacoin.dht;

import io.topiacoin.core.Configuration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.crypto.HashUtils;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.model.CurrentUser;
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

public class SDFSDHTAccessor {
	private static final Log _log = LogFactory.getLog(SDFSDHTAccessor.class);
	private DHT _dht;
	private DataModel _model;

	public SDFSDHTAccessor(Configuration config, DataModel model) {
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

	public void stop() {
		_dht.shutdown(true);
	}

	public List<String> fetchMyWorkspaceIDs() throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		String dhtKey = HashUtils.sha256String(me.getUserID());
		Set<String> encryptedWorkspaceIDs = _dht.fetchContent(dhtKey);
		List<String> workspaceIDs = new ArrayList<>();
		for (String encryptedWksID : encryptedWorkspaceIDs) {
			try {
				String wID = new String(CryptoUtils.decryptWithPrivateKey(encryptedWksID.split("\n")[1], me.getPrivateKey()));
				workspaceIDs.add(wID);
			} catch (CryptographicException e) {
				_log.warn("Could not decrypt a WorkspaceID - removing it");
				_dht.removeContent(dhtKey, encryptedWksID);
			}
		}
		return workspaceIDs;
	}

	public SecretKey createWorkspace(String workspaceID) throws NoSuchUserException {
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

	public void addInvitation(String workspaceID, User invitee, SecretKey workspaceNodeKey) throws CryptographicException {
		//Store the nodeKey for the new user
		String dhtKey = buildWorkspaceNodeKeyDHTKey(workspaceID, invitee);
		String dhtValue = Base64.getEncoder().encodeToString(CryptoUtils.encryptWithPublicKey(workspaceNodeKey.getEncoded(), invitee.getPublicKey()));
		_dht.storeContent(dhtKey, dhtValue);
		//Store the workspaceID for the user
		dhtKey = HashUtils.sha256String(invitee.getUserID());
		dhtValue = CryptoUtils.encryptWithPublicKeyToString(workspaceID, invitee.getPublicKey());
		String hash = HashUtils.sha256String(HashUtils.sha256String(workspaceID));
		_dht.storeContent(dhtKey, hash + "\n" + dhtValue);
	}

	public void leaveWorkspace(String workspaceID) throws NoSuchUserException {
		User me = _model.getCurrentUser();
		try {
			removeMemberFromWorkspace(workspaceID, _model.getMemberInWorkspace(workspaceID, me.getUserID()));
		} catch (NoSuchWorkspaceException | NoSuchMemberException e) {
			_log.warn("Encountered an issue leaving workspace", e);
			throw new IllegalArgumentException("No such Workspace", e);
		}
	}

	public void removeMemberFromWorkspace(String workspaceID, Member member) throws NoSuchUserException {
		//Fetch the User's workspaceIDs and remove this workspace from the list
		User user = _model.getUserByID(member.getUserID());
		String dhtKey = HashUtils.sha256String(user.getUserID());
		String hash = HashUtils.sha256String(HashUtils.sha256String(workspaceID));
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
		dhtKey = HashUtils.sha256String(workspaceID);
		hash = HashUtils.sha256String(HashUtils.sha256String(user.getUserID()));
		values = _dht.fetchContent(dhtKey);
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
	}

	public List<MemberNode> fetchMemberNodes(String workspaceID, SecretKey workspaceNodeKey) {
		String dhtKey = HashUtils.sha256String(workspaceID);
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

	public boolean addMyMemberNode(String workspaceID, MemberNode memberNode, SecretKey workspaceNodeKey) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		if (!me.getUserID().equals(memberNode.getUserID())) {
			throw new IllegalArgumentException("Will not add a MemberNode for somebody other than myself - THAT WOULD BE EVIL");
		}
		if(!fetchMemberNodes(workspaceID, workspaceNodeKey).contains(memberNode)) {
			String dhtKey = HashUtils.sha256String(workspaceID);
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

	public boolean removeMyMemberNode(String workspaceID, MemberNode memberNode, SecretKey workspaceNodeKey) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		if (!me.getUserID().equals(memberNode.getUserID())) {
			throw new IllegalArgumentException("Will not remove a MemberNode for somebody other than myself - ONLY VILLAINS DO THAT");
		}
		boolean tr = false;
		String dhtKey = HashUtils.sha256String(workspaceID);
		Set<String> encryptedMemberNodes = _dht.fetchContent(dhtKey);
		for (String encryptedMemberNode : encryptedMemberNodes) {
			try {
				MemberNode thisNode = new MemberNode(CryptoUtils.decryptStringWithSecretKey(encryptedMemberNode.split("\n")[1], workspaceNodeKey));
				if (thisNode.equals(memberNode)) {
					_dht.removeContent(dhtKey, encryptedMemberNode);
					tr = true;
				}
			} catch (CryptographicException e) {
				_log.warn("Could not decrypt a Member Node(1) - removing it");
				_dht.removeContent(dhtKey, encryptedMemberNode);
			}
		}
		return tr;
	}

	public SecretKey fetchMyWorkspaceNodeKey(String workspaceID) throws NoSuchUserException {
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

	private String buildWorkspaceNodeKeyDHTKey(String workspaceID, User user) {
		String pkString = Base64.getEncoder().encodeToString(user.getPublicKey().getEncoded());
		return HashUtils.sha256String(workspaceID + pkString);
	}
}
