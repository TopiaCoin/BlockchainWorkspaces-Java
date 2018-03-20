package io.topiacoin.chunks.impl.retrievalStrategies;

import io.topiacoin.chunks.intf.ChunkRetrievalStrategy;
import io.topiacoin.chunks.model.ChunkLocationResponse;
import io.topiacoin.chunks.model.ChunkRetrievalPlan;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

public class SimpleChunkRetrievalStrategy implements ChunkRetrievalStrategy {
	@Override public ChunkRetrievalPlan generateRetrievalPlan(ChunkLocationResponse[] chunkLocationResponses, KeyPair fetchPair, List<String> chunkIDs) {
		try {
			ChunkRetrievalPlan toReturn = new ChunkRetrievalPlan(chunkIDs);
			for (ChunkLocationResponse response : chunkLocationResponses) {
				try {
					KeyAgreement ka = KeyAgreement.getInstance("ECDH");
					ka.init(fetchPair.getPrivate());
					KeyFactory kf = KeyFactory.getInstance("EC");
					X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(response.pubKey.getBytes());
					PublicKey otherPublicKey = kf.generatePublic(pkSpec);
					ka.doPhase(otherPublicKey, true);

					SecretKey sharedSecret = ka.generateSecret("AES");

					toReturn.addKey(response.userID, sharedSecret);
				} catch (InvalidKeyException e) {
					//The key in the response is no good? Weird.
					e.printStackTrace();
				}
			}
			if(toReturn.isCompletePlan()) {
				return toReturn;
			} else {
				//uhhh
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}
}
