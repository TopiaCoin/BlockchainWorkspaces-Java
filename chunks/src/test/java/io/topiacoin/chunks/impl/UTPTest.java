package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.impl.transferRunnables.utp.UTPListener;
import io.topiacoin.chunks.impl.transferRunnables.utp.UTPSender;
import io.topiacoin.chunks.intf.AbstractProtocolTest;
import io.topiacoin.chunks.intf.ProtocolListener;
import io.topiacoin.chunks.intf.ProtocolSender;
import org.junit.Ignore;

import java.security.PrivateKey;
import java.security.PublicKey;

@Ignore
public class UTPTest extends AbstractProtocolTest {

	@Override protected ProtocolListener getProtocolListener(int port, PublicKey pubKey, PrivateKey privKey) {
		return new UTPListener(port, pubKey, privKey);
	}

	@Override protected ProtocolSender getProtocolSender(PublicKey pubKey, PrivateKey privKey) {
		return new UTPSender(pubKey, privKey);
	}
}
