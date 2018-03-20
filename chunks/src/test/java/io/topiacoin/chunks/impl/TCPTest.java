package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.impl.transferRunnables.tcp.TCPListener;
import io.topiacoin.chunks.impl.transferRunnables.tcp.TCPSender;
import io.topiacoin.chunks.intf.AbstractProtocolTest;
import io.topiacoin.chunks.intf.ProtocolListener;
import io.topiacoin.chunks.intf.ProtocolSender;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TCPTest extends AbstractProtocolTest {

	@Override protected ProtocolListener getProtocolListener(int port, PublicKey pubKey, PrivateKey privKey) {
		return new TCPListener(port, pubKey, privKey);
	}

	@Override protected ProtocolSender getProtocolSender(PublicKey pubKey, PrivateKey privKey) {
		return new TCPSender(pubKey, privKey);
	}
}
