package io.topiacoin.model;

import java.util.Objects;

public class MemberNode implements Comparable<MemberNode> {
	private String userId;
	private String hostname;
	private int port;

	public MemberNode(String userId, String hostname, int port) {
		this.userId = userId;
		this.hostname = hostname;
		this.port = port;
	}

	public MemberNode(String dhtString) {
		String[] dhtParts = dhtString.split("\n");
		if(dhtParts.length != 3) {
			throw new IllegalArgumentException("Malformed DHT String");
		}
		this.userId = dhtParts[0];
		this.hostname = dhtParts[1];
		this.port = Integer.parseInt(dhtParts[2]);
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

	public String toDHTString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.userId);
		builder.append("\n");
		builder.append(this.hostname);
		builder.append("\n");
		builder.append(this.port);
		return builder.toString();
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MemberNode that = (MemberNode) o;
		return port == that.port &&
				Objects.equals(userId, that.userId) &&
				Objects.equals(hostname, that.hostname);
	}

	@Override public int hashCode() {
		return Objects.hash(userId, hostname, port);
	}

	@Override public int compareTo(MemberNode o) {
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
