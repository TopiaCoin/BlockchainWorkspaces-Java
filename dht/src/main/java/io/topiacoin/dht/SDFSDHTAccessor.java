package io.topiacoin.dht;

import io.topiacoin.core.Configuration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.dht.config.DHTConfiguration;
import io.topiacoin.dht.network.Node;
import io.topiacoin.dht.network.NodeID;
import io.topiacoin.dht.util.Utilities;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.UserNode;
import io.topiacoin.model.exceptions.NoSuchUserException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SDFSDHTAccessor {

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

	public void submitUserNode(UserNode node) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		_dht.storeContent(me.getUserID() + "-usernodes", Utilities.objectToString(node));
	}

	public void removeUserNode(UserNode node) throws NoSuchUserException {
		CurrentUser me = _model.getCurrentUser();
		_dht.removeContent(me.getUserID() + "-usernodes", Utilities.objectToString(node));
	}

	public List<UserNode> listUserNodes(String userID) {
		Set<String> userNodeStrings = _dht.fetchContent(userID + "-usernodes");
		List<UserNode> userNodes = new ArrayList<>();
		for(String nodeString : userNodeStrings) {
			Object nodeObj = Utilities.objectFromString(nodeString);
			userNodes.add((UserNode) nodeObj);
		}
		return userNodes;
	}


}
