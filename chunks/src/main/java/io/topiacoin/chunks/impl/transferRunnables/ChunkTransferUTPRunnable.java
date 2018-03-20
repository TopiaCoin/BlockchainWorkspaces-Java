package io.topiacoin.chunks.impl.transferRunnables;

import io.topiacoin.chunks.impl.transferRunnables.utp.UTPSocket;
import io.topiacoin.chunks.intf.ChunkTransferHandler;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ChunkTransferUTPRunnable implements Runnable {

	private ChunkTransferHandler _handler;
	private String _location;
	private String _chunkID;

	public ChunkTransferUTPRunnable(ChunkTransferHandler handler, String location, String chunkID) {
		_handler = handler;
		_location = location;
		_chunkID = chunkID;
	}

	@Override public void run() {
		try {
			InetAddress addr = InetAddress.getByName(_location);
			UTPSocket socket = new UTPSocket(addr, 7777);

			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(_location);
		httpGet.setHeader("X-Chunk-ID", _chunkID);
		CloseableHttpResponse chunkResponse = null;
		try {
			chunkResponse = httpclient.execute(httpGet);
			if(chunkResponse.getStatusLine().getStatusCode() == 200) {
				byte[] chunkdata = IOUtils.toByteArray(chunkResponse.getEntity().getContent());
				_handler.didFetchChunk(_chunkID, chunkdata);
			} else {
				_handler.failedToFetchChunk(_chunkID, "Failed to fetch chunk: " + chunkResponse.getStatusLine().getStatusCode(), null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(chunkResponse != null) {
				try {
					chunkResponse.close();
				} catch (IOException e) {
					//NOP
				}
			}
		}
	}
}
