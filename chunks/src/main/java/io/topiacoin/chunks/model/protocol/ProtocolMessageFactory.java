package io.topiacoin.chunks.model.protocol;

import io.topiacoin.chunks.exceptions.UnknownMessageTypeException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtocolMessageFactory {

	private final Map<String, Byte> _messageTypes = new HashMap<>();
	private List<Byte> _requestMessageTypes = new ArrayList<>();
	private List<Byte> _errorMessageTypes = new ArrayList<>();

	public ProtocolMessageFactory() {
		byte b = 0x01;
		_messageTypes.put("QUERY_CHUNKS", b);
		_requestMessageTypes.add(b);
		b = 0x02;
		_messageTypes.put("HAVE_CHUNKS", b);
		b = 0x03;
		_messageTypes.put("REQUEST_CHUNK", b);
		_requestMessageTypes.add(b);
		b = 0x04;
		_messageTypes.put("GIVE_CHUNK", b);
		b = 0x09;
		_messageTypes.put("ERROR", b);
		_errorMessageTypes.add(b);
	}

	public ProtocolMessage getMessage(byte messageType, ByteBuffer data) throws UnknownMessageTypeException {
		ProtocolMessage toReturn;
		if (messageType == 0x01) {
			toReturn = new QueryChunksProtocolRequest();
		} else if (messageType == 0x02) {
			toReturn = new HaveChunksProtocolResponse();
		} else if (messageType == 0x03) {
			toReturn = new FetchChunkProtocolRequest();
		} else if (messageType == 0x04) {
			toReturn = new GiveChunkProtocolResponse();
		} else if (messageType == 0x09) {
			toReturn = new ErrorProtocolResponse();
		} else {
			throw new UnknownMessageTypeException("Unknown message type '" + messageType + "'");
		}
		toReturn.fromBytes(data);
		return toReturn;
	}

	public byte getMessageByteIdentifier(ProtocolMessage message) throws UnknownMessageTypeException {
		Byte tr = _messageTypes.get(message.getType());
		if(tr == null) {
			throw new UnknownMessageTypeException("Unknown Message Type '" + message.getType() + "'");
		}
		return tr;
	}

	public boolean isRequest(byte messageType) {
		return _requestMessageTypes.contains(messageType);
	}

	public boolean isError(byte messageType) {
		return _errorMessageTypes.contains(messageType);
	}
}
