package io.topiacoin.chunks.model;

import java.net.SocketAddress;

public class MessageID {
	private int _id;
	private SocketAddress _address;

	public MessageID(int id, SocketAddress address) {
		_id = id;
		_address = address;
	}

	public int getId() {
		return _id;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MessageID) {
			MessageID other = (MessageID) obj;
			return other._address.equals(this._address) && other._id == this._id;
		}
		return false;
	}
}
