package io.topiacoin.dht;

import io.topiacoin.core.Configuration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.crypto.HashUtils;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.util.Utilities;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.Member;
import io.topiacoin.model.MemberNode;
import io.topiacoin.model.User;
import io.topiacoin.model.UserNode;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.NoSuchMemberException;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SDFSDHTAccessor {
	private static final Log _log = LogFactory.getLog(SDFSDHTAccessor.class);
	private DHTConfiguration _dhtConfig;
	private DHT _dht;
	private DataModel _model;

	public SDFSDHTAccessor(Configuration config, DataModel model) {
		_dhtConfig = new DHTConfiguration(config);
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
		for(String encryptedWksID : encryptedWorkspaceIDs) {
			try {
				String[] parts = encryptedWksID.split("\n");
				if(parts.length == 2) {
					String wID = new String(CryptoUtils.decryptWithPrivateKey(parts[1], me.getPrivateKey()));
					workspaceIDs.add(wID);
				} else {
					_log.warn("Found a malformed WorkspaceID - removing it");
					_dht.removeContent(dhtKey, encryptedWksID);
				}
			} catch (CryptographicException e) {
				_log.warn("Could not decrypt a WorkspaceID - removing it");
				_dht.removeContent(dhtKey, encryptedWksID);
			}
		}
		return workspaceIDs;
	}

	public Map<String, SecretKey> fetchWorkspaceNodeKeys(List<String> workspaceIDs) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		Map<String, SecretKey> workspaceNodeKeys = new HashMap<String, SecretKey>();
		for(String workspaceID : workspaceIDs) {
			String dhtKey = HashUtils.sha256String(workspaceID + publicKeyToString(me.getPublicKey()));
			Set<String> encWksNodeKeyStrs = _dht.fetchContent(dhtKey);
			Iterator<String> encWksNodeKeyStrsIterator = encWksNodeKeyStrs.iterator();
			SecretKey wksNodeKey = null;
			while(wksNodeKey == null) {
				if (!encWksNodeKeyStrsIterator.hasNext()) {
					_log.warn("Couldn't find a Workspace Node Key for " + workspaceID + ", skipping");
					break;
				} else {
					String ewnks = encWksNodeKeyStrsIterator.next();
					try {
						wksNodeKey = CryptoUtils.getAESKeyFromEncodedBytes(CryptoUtils.decryptWithPrivateKey(ewnks, me.getPrivateKey()));
					} catch (CryptographicException e) {
						_log.warn("Found an invalid Workspace Node Key for " + workspaceID + " - removing it");
						_dht.removeContent(dhtKey, ewnks);
					}
				}
			}
			if(wksNodeKey != null) {
				if (encWksNodeKeyStrsIterator.hasNext()) {
					_log.warn("Found more than one Workspace Node Key for " + workspaceID + ", but I was only expecting one. Selecting one at random...");
				}
				workspaceNodeKeys.put(workspaceID, wksNodeKey);
			}
		}
		return workspaceNodeKeys;
	}

	public List<MemberNode> fetchMemberNodes(String workspaceID, SecretKey workspaceNodeKey) {
		String dhtKey = HashUtils.sha256String(workspaceID);
		Set<String> encryptedMemberNodes = _dht.fetchContent(dhtKey);
		List<MemberNode> memberNodes = new ArrayList<>();
		for(String encryptedMemberNode : encryptedMemberNodes) {
			try {
				String[] emnParts = encryptedMemberNode.split("\n");
				if(emnParts.length == 2) {
					memberNodes.add(new MemberNode(CryptoUtils.decryptStringWithSecretKey(emnParts[1], workspaceNodeKey)));
				} else {
					_log.warn("Found a malformed member node - removing it");
					_dht.removeContent(dhtKey, encryptedMemberNode);
				}
			} catch (CryptographicException e) {
				_log.warn("Could not decrypt a Member Node - removing it");
				_dht.removeContent(dhtKey, encryptedMemberNode);
			}
		}
		return memberNodes;
	}

	public SecretKey addMyMemberNode(String workspaceID, MemberNode memberNode, List<Member> workspaceMembers) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		if(!me.getUserID().equals(memberNode.getUserID())) {
			throw new IllegalArgumentException("Will not add a MemberNode for somebody other than myself - THAT WOULD BE EVIL");
		}
		String dhtKey = HashUtils.sha256String(workspaceID);
		String hash = HashUtils.sha256String(HashUtils.sha256String(me.getUserID()));
		SecretKey newWorkspaceNodeKey;
		try {
			newWorkspaceNodeKey = CryptoUtils.generateAESKey();
			String encryptedMemberNode = CryptoUtils.encryptStringWithSecretKey(memberNode.toDHTString(), newWorkspaceNodeKey);
			_dht.storeContent(dhtKey, hash + "\n" + encryptedMemberNode);
			for(Member member : workspaceMembers) {
				User user = _model.getUserByID(member.getUserID());
				try {
					if (user != null) {
						dhtKey = HashUtils.sha256String(workspaceID + publicKeyToString(user.getPublicKey()));
						String dhtValue = Base64.getEncoder().encodeToString(CryptoUtils.encryptWithPublicKey(newWorkspaceNodeKey.getEncoded(), user.getPublicKey()));
						_dht.storeContent(dhtKey, dhtValue);
					} else {
						_log.warn("I don't have a User record for " + member.getUserID() + " - cannot list my MemberNode for them");
					}
				} catch (CryptographicException e) {
					_log.warn("Failed to encrypt NodeKey for " + user.getUserID() + " - skipping");
				}
			}
		} catch (CryptographicException e) {
			_log.error("Internal error adding member node", e);
			return null;
		}
		return newWorkspaceNodeKey;
	}

	public boolean removeMyMemberNode(String workspaceID, MemberNode memberNode, SecretKey workspaceNodeKey) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		if(!me.getUserID().equals(memberNode.getUserID())) {
			throw new IllegalArgumentException("Will not remove a MemberNode for somebody other than myself - ONLY VILLAINS DO THAT");
		}
		boolean tr = false;
		String dhtKey = HashUtils.sha256String(workspaceID);
		Set<String> encryptedMemberNodes = _dht.fetchContent(dhtKey);
		for(String encryptedMemberNode : encryptedMemberNodes) {
			try {
				String[] emnParts = encryptedMemberNode.split("\n");
				if(emnParts.length == 2) {
					MemberNode thisNode = new MemberNode(CryptoUtils.decryptStringWithSecretKey(emnParts[1], workspaceNodeKey));
					if(thisNode.equals(memberNode)) {
						_dht.removeContent(dhtKey, encryptedMemberNode);
						tr = true;
					}
				} else {
					_log.warn("Found a malformed member node - removing it");
					_dht.removeContent(dhtKey, encryptedMemberNode);
				}
			} catch (CryptographicException e) {
				_log.warn("Could not decrypt a Member Node(1) - removing it");
				_dht.removeContent(dhtKey, encryptedMemberNode);
			}
		}
		dhtKey = HashUtils.sha256String(workspaceID + publicKeyToString(me.getPublicKey()));
		Set<String> encWksNodeKeyStrs = _dht.fetchContent(dhtKey);
		for (String encWksNodeKeyStr : encWksNodeKeyStrs) {
			_dht.removeContent(dhtKey, encWksNodeKeyStr);
		}
		return tr;
	}

	public SecretKey createWorkspace(String workspaceID) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		String dhtKey = HashUtils.sha256String(workspaceID + publicKeyToString(me.getPublicKey()));
		SecretKey newWorkspaceNodeKey;
		try {
			newWorkspaceNodeKey = CryptoUtils.generateAESKey();
			String dhtValue = Base64.getEncoder().encodeToString(CryptoUtils.encryptWithPublicKey(newWorkspaceNodeKey.getEncoded(), me.getPublicKey()));
			_dht.storeContent(dhtKey, dhtValue);
			dhtKey = HashUtils.sha256String(me.getUserID());
			dhtValue = CryptoUtils.encryptWithPublicKeyToString(workspaceID, me.getPublicKey());
			String hash = HashUtils.sha256String(HashUtils.sha256String(workspaceID));
			_dht.storeContent(dhtKey, hash + "\n" + dhtValue);
			return newWorkspaceNodeKey;
		} catch (CryptographicException e) {
			_log.error("Internal error creating workspace", e);
			return null;
		}
	}

	public void addInvitation(String workspaceID, User invitee, SecretKey workspaceNodeKey) throws CryptographicException {
		String dhtKey = HashUtils.sha256String(workspaceID + publicKeyToString(invitee.getPublicKey()));
		String dhtValue = Base64.getEncoder().encodeToString(CryptoUtils.encryptWithPublicKey(workspaceNodeKey.getEncoded(), invitee.getPublicKey()));
		_dht.storeContent(dhtKey, dhtValue);
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
		}
	}

	public void removeMemberFromWorkspace(String workspaceID, Member member) throws NoSuchUserException {
		User user = _model.getUserByID(member.getUserID());
		if(user != null) {
			String dhtKey = HashUtils.sha256String(user.getUserID());
			String hash = HashUtils.sha256String(HashUtils.sha256String(workspaceID));
			Set<String> values = _dht.fetchContent(dhtKey);
			for(String value : values) {
				String[] split = value.split("\n");
				if(split.length == 2) {
					if(split[0].equals(hash)) {
						_dht.removeContent(dhtKey, value);
					}
				} else {
					_log.warn("Found invalid data in the DHT - removing it");
					_dht.removeContent(dhtKey, value);
				}
			}
			dhtKey = HashUtils.sha256String(workspaceID + publicKeyToString(user.getPublicKey()));
			Set<String> encWksNodeKeyStrs = _dht.fetchContent(dhtKey);
			for(String keyStr : encWksNodeKeyStrs) {
				_dht.removeContent(dhtKey, keyStr);
			}
			dhtKey = HashUtils.sha256String(workspaceID);
			hash = HashUtils.sha256String(HashUtils.sha256String(user.getUserID()));
			values = _dht.fetchContent(dhtKey);
			for(String value : values) {
				String[] split = value.split("\n");
				if(split.length == 2) {
					if(split[0].equals(hash)) {
						_dht.removeContent(dhtKey, value);
					}
				} else {
					_log.warn("Found invalid data in the DHT - removing it");
					_dht.removeContent(dhtKey, value);
				}
			}
		}
	}

	private String publicKeyToString(PublicKey pubKey) {
		return Base64.getEncoder().encodeToString(pubKey.getEncoded());
	}
}
