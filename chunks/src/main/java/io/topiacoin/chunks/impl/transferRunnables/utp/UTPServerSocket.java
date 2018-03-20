/*
Created by Peter Lipay. University of Washington. June 2010.
*/
package io.topiacoin.chunks.impl.transferRunnables.utp;
import io.topiacoin.chunks.intf.ProtocolServerSocket;
import io.topiacoin.chunks.intf.ProtocolSocket;

import java.net.*;
import java.io.*;
import java.util.BitSet;
import java.util.Random;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;

/*
This class sits on the server and waits for incoming UTP connections
*/
class UTPServerSocket implements ProtocolServerSocket {

	//This is the base UDP socket
	private DatagramSocket basesocket;
	//This is the version
	int version;
	boolean run = true;
	
	//Takes the port number to listen on
	public UTPServerSocket(int port) throws SocketException, IOException {
		basesocket = new DatagramSocket(port);
		version = 1;
	}
	
	//Will wait for incoming UTP connections. If a connection is established,
	//the UTPSocket corresponding to the connection will be returned to the server
	public ProtocolSocket accept() throws IOException {
		Random rand = new Random(System.nanoTime());
		while (run) {
			DatagramPacket getConnectionRequest = new DatagramPacket(new byte[20], 20);
			//Wait for incoming UDP packets
			basesocket.receive(getConnectionRequest);
			//If the packet is at least 20 bytes in length (the minimum UTP header size)
			//We assume it's a UTP Packet and try to establish a connection
			if (getConnectionRequest.getLength() >= 20) {
				UTPPacket utpConnectionRequest = new UTPPacket(getConnectionRequest); 
				int clientversion = utpConnectionRequest.getVersion();
				int clienttype = utpConnectionRequest.getType();
				//Make sure this is a SYN packet, and that the version numbers match up
				if (version == clientversion && clienttype == UTPSocket.ST_SYN) {
					byte [] connectionIDSendBytes = utpConnectionRequest.getConnectionID();
					byte [] connectionIDReceiveBytes = new byte[2];
					byte [] temp = new byte[4];
					temp = intToBytes(bytesToInt(connectionIDSendBytes)+1);
					connectionIDReceiveBytes[0] = temp[2];
					connectionIDReceiveBytes[1] = temp[3];
					int acknumber = utpConnectionRequest.getAckNumber();
					int secnumber = 0; 
					//Now we try to confirm the connection, this will involve sending a connection confirm packet
				   //and waiting for an ACK (similar to TCP's 3-way handshake). If we are unsuccessful, 
					//we'll loop back and wait for incoming packets again
					try {
						UTPSocket socket = new UTPSocket(getConnectionRequest.getAddress(),getConnectionRequest.getPort(),connectionIDSendBytes,connectionIDReceiveBytes,acknumber,secnumber, version);
						return new UTPProtocolSocket(socket);
					} catch (SocketTimeoutException e) {
					}
				}
			}
		}
		return null;
	}

	public void close() {
		run = false;
	}
	
	//Converts an integer to a byte array
	private byte [] intToBytes (int input) {
		byte[] result = new byte[4];
		result[0] = (byte)(input >>> 24);
		result[1] = (byte)(input >>> 16);
		result[2] = (byte)(input >>> 8);
		result[3] = (byte) input; 
		return result;
	}
	
	//Converts a byte array to an integer
	private int bytesToInt(byte [] input) {
		if (input == null || input.length <= 0) {
			return 0;
		} else if (input.length == 1) {
			return  (input[0] & 0xFF);
		} else if (input.length == 2) {
			return (input[0] & 0xFF) << 8 | (input[1] & 0xFF);
		} else if (input.length == 3) {
			return (input[0] & 0xFF) << 16 | (input[1] & 0xFF) << 8 | (input[2] & 0xFF);
		} else {
			return (input[0] & 0xFF) << 24 | (input[1] & 0xFF) << 16 | (input[2] & 0xFF) << 8 | (input[3] & 0xFF);
		}
	}

}