package io.topiacoin.model;

import java.io.Serializable;
import java.util.Objects;

public class UserNode implements Comparable<UserNode>, Serializable {
	private static final long serialVersionUID = 1;
	private String userId;
	private String hostname;
	private int port;
	private byte[] publicKey;

	public UserNode(String userId, String hostname, int port, byte[] transferPublicKey) {
		this.userId = userId;
		this.hostname = hostname;
		this.port = port;
		this.publicKey = transferPublicKey;
	}

	public String getUserID() {
		return userId;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		UserNode that = (UserNode) o;
		return port == that.port &&
				Objects.equals(userId, that.userId) &&
				Objects.equals(hostname, that.hostname);
	}

	@Override public int hashCode() {
		return Objects.hash(userId, hostname, port);
	}

	@Override public int compareTo(UserNode o) {
		if(o == null) {
			return 1;
		} else {
			int comp = userId.compareTo(o.userId);
			comp = comp == 0 ? hostname.compareTo(o.hostname) : comp;
			if(comp == 0) {
				if(port > o.port) {
					comp = 1;
				} else if(port < o.port) {
					comp = -1;
				}
			}
			return comp;
		}
	}
}
