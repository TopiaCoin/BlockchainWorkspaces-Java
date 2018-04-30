package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.FailedToStartCommsListenerException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.InvalidMessageException;
import io.topiacoin.chunks.exceptions.InvalidMessageIDException;
import io.topiacoin.chunks.exceptions.InvalidReservationException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunkRetrievalStrategy;
import io.topiacoin.chunks.intf.ChunkRetrievalStrategyFactory;
import io.topiacoin.chunks.intf.ChunkStorage;
import io.topiacoin.chunks.intf.ChunkTransferer;
import io.topiacoin.chunks.intf.ChunksTransferHandler;
import io.topiacoin.chunks.intf.ProtocolCommsHandler;
import io.topiacoin.chunks.intf.ProtocolCommsResponseHandler;
import io.topiacoin.chunks.intf.ProtocolCommsService;
import io.topiacoin.chunks.model.ChunkRetrievalPlan;
import io.topiacoin.chunks.model.MessageID;
import io.topiacoin.chunks.model.protocol.ErrorProtocolResponse;
import io.topiacoin.chunks.model.protocol.FetchChunkProtocolRequest;
import io.topiacoin.chunks.model.protocol.GiveChunkProtocolResponse;
import io.topiacoin.chunks.model.protocol.HaveChunksProtocolResponse;
import io.topiacoin.chunks.model.protocol.ProtocolMessage;
import io.topiacoin.chunks.model.protocol.QueryChunksProtocolRequest;
import io.topiacoin.core.Configuration;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.MemberNode;
import io.topiacoin.model.exceptions.NoSuchUserException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SDFSChunkTransferer implements ChunkTransferer {
	private static final Log _log = LogFactory.getLog(SDFSChunkTransferer.class);
	private Configuration _configuration;
	private ChunkRetrievalStrategyFactory _chunkRetrievalStrategyFactory;
	private ProtocolCommsService _comms;
	private ChunkStorage _chunkStorage;
	private DataModel _model;
	private MemberNode _myMemberNode;

	public SDFSChunkTransferer(MemberNode myMemberNode, KeyPair chunkTransferPair) throws IOException, FailedToStartCommsListenerException {
		_myMemberNode = myMemberNode;
		_comms = new TCPProtocolCommsService(myMemberNode.getPort(), chunkTransferPair);
		_comms.setHandler(new StandardProtocolCommsResponder());
		_comms.startListener();
	}

	/**
	 *
	 * @param chunkIDs a List of ChunkIDs to be transferred. The items in this list will be used to determine which chunks should be downloaded
	 * @param containerID The ID of the container which these chunks belong to
	 * @param chunksHandler The Handler for notifying when each chunk succeeds/fails to transfer, and for when the whole list is done being processed
	 * @param state An opaque object that will be passed to the handler on fetch operation completion.  This can be used to carry state between the initiator of the fetch and the handler.
	 * @throws IllegalStateException if the data model doesn't contain any member nodes
	 */
	@Override public void fetchChunksRemotely(List<String> chunkIDs, String containerID, final ChunksTransferHandler chunksHandler, final Object state) {
		List<MemberNode> memberNodes = fetchMemberNodes(containerID);
		if (memberNodes == null || memberNodes.isEmpty()) {
			throw new IllegalStateException("MemberNodes cannot be null or blank");
		}
		final Map<MessageID, MemberNode> memberMessages = new HashMap<>();
		final ChunkRetrievalStrategy strategy = _chunkRetrievalStrategyFactory.createStrategy(chunkIDs);
		ProtocolCommsResponseHandler handler = new ProtocolCommsResponseHandler() {
			@Override public void responseReceived(ProtocolMessage response, MessageID messageID) {
				MemberNode memNode = memberMessages.remove(messageID);
				if (!strategy.isCompletePlan()) {
					if (response instanceof HaveChunksProtocolResponse) {
						strategy.submitLocationResponse((HaveChunksProtocolResponse) response, memNode);
						if (strategy.isCompletePlan()) {
							executeStrategy(strategy, chunksHandler, state);
						}
					} else if (response instanceof ErrorProtocolResponse) {
						strategy.submitLocationResponse((ErrorProtocolResponse) response, memNode);
					} else {
						_log.warn("Received an unxepected response type: " + response.getType());
					}
				}
				if (memberMessages.isEmpty()) {
					strategy.allResponsesSubmitted();
					if (!strategy.isCompletePlan()) {
						chunksHandler.failedToBuildFetchPlan();
					}
				}
			}

			@Override public void error(Throwable t, MessageID messageID) {
				_log.error("Error determining chunk locations", t);
			}

			@Override public void error(String message, boolean shouldReply, MessageID messageId) {
				_log.warn("Error determining chunk locations: " + message);
			}
		};
		QueryChunksProtocolRequest request;
		for (MemberNode memberNode : memberNodes) {
			request = new QueryChunksProtocolRequest(chunkIDs.toArray(new String[0]), memberNode);
			try {
				MessageID messageId = _comms.sendMessage(memberNode, request, handler);
				memberMessages.put(messageId, memberNode);
			} catch (InvalidKeyException | FailedToStartCommsListenerException | InvalidMessageException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<MemberNode> fetchMemberNodes(String containerID) {
		List<MemberNode> toReturn = _model.getMemberNodesForContainer(containerID);
		toReturn.remove(_myMemberNode);
		return toReturn;
	}

	@Override public void setConfiguration(Configuration configuration) {
		this._configuration = configuration;
	}

	@Override public void setChunkRetrievalStrategyFactory(ChunkRetrievalStrategyFactory stratFac) {
		this._chunkRetrievalStrategyFactory = stratFac;
	}

	@Override public void setChunkStorage(ChunkStorage storage) {
		this._chunkStorage = storage;
	}

	@Override public void setDataModel(DataModel model) {
		this._model = model;
	}

	@Override public void stop() {
		_comms.stop();
	}

	void executeStrategy(ChunkRetrievalStrategy strategy, ChunksTransferHandler chunksHandler, Object state) {
		final ChunkRetrievalPlan plan = strategy.getPlan();
		final Map<MessageID, String> chunkRequests = new HashMap<>();
		ChunkRetrievalPlan.PlanTask task;
		FetchChunkProtocolRequest request;
		ProtocolCommsResponseHandler handler = new ProtocolCommsResponseHandler() {
			private void executeNextTask() {
				ChunkRetrievalPlan.PlanTask task;
				while ((task = plan.getNextTask()) != null) {
					FetchChunkProtocolRequest request = new FetchChunkProtocolRequest(task.chunkID, task.source);
					try {
						MessageID messageID = _comms.sendMessage(task.source, request, this);
						chunkRequests.put(messageID, task.chunkID);
					} catch (InvalidKeyException | IOException | InvalidMessageException | FailedToStartCommsListenerException e) {
						_log.warn("Failed to fetch chunk", e);
						plan.markChunkAsFailed(task.chunkID);
					}
				}
				if (plan.isComplete()) {
					chunksHandler.fetchedAllChunks(state);
				}
			}

			@Override public void responseReceived(ProtocolMessage response, MessageID messageID) {
				String chunkID = chunkRequests.remove(messageID);
				if (response instanceof GiveChunkProtocolResponse) {
					GiveChunkProtocolResponse resp = (GiveChunkProtocolResponse) response;
					plan.markChunkAsFetched(chunkID);
					try {
						try {
							_chunkStorage.addChunk(chunkID, new ByteArrayInputStream(resp.getChunkData()), null, true);
						} catch (DuplicateChunkException e) {
							//Ok, whatever
						}
						chunksHandler.didFetchChunk(chunkID, state);
					} catch (InsufficientSpaceException | IOException | InvalidReservationException e) {
						_log.error("Failed to store chunk", e);
						plan.markChunkAsFailed(chunkID);
					}
				} else {
					plan.markChunkAsFailed(chunkID);
				}
				executeNextTask();
			}

			@Override public void error(Throwable t, MessageID messageID) {
				_log.error("Failed to fetch chunk", t);
				plan.markChunkAsFailed(chunkRequests.remove(messageID));
				executeNextTask();
			}

			@Override public void error(String message, boolean shouldReply, MessageID messageID) {
				_log.warn("Failed to fetch chunk: " + message);
				plan.markChunkAsFailed(chunkRequests.remove(messageID));
				executeNextTask();
			}
		};
		while ((task = plan.getNextTask()) != null) {
			request = new FetchChunkProtocolRequest(task.chunkID, task.source);
			try {
				MessageID messageID = _comms.sendMessage(task.source, request, handler);
				chunkRequests.put(messageID, task.chunkID);
			} catch (InvalidKeyException | IOException | InvalidMessageException | FailedToStartCommsListenerException e) {
				_log.warn("Failed to fetch chunk", e);
				plan.markChunkAsFailed(task.chunkID);
			}
		}
	}

	private class StandardProtocolCommsResponder implements ProtocolCommsHandler {
		@Override public void requestReceived(ProtocolMessage request, MessageID messageID) {
			try {
				ProtocolMessage response;
				CurrentUser me = _model.getCurrentUser();
				if (request instanceof FetchChunkProtocolRequest) {
					FetchChunkProtocolRequest req = (FetchChunkProtocolRequest) request;
					if (req.getAuthToken().equals(_myMemberNode.getAuthToken())) {
						if (_chunkStorage.hasChunk(req.getChunkID())) {
							try {
								byte[] data = _chunkStorage.getChunkData(req.getChunkID());
								response = new GiveChunkProtocolResponse(req.getChunkID(), data, me.getUserID());
							} catch (NoSuchChunkException | IOException e) {
								_log.error("Unexpected internal issue", e);
								response = new ErrorProtocolResponse("I don't have that chunk", me.getUserID());
							}
						} else {
							response = new ErrorProtocolResponse("I don't have that chunk", me.getUserID());
						}
					} else {
						response = new ErrorProtocolResponse("That's not my auth token", me.getUserID());
					}
				} else if (request instanceof QueryChunksProtocolRequest) {
					QueryChunksProtocolRequest req = (QueryChunksProtocolRequest) request;
					if (req.getAuthToken().equals(_myMemberNode.getAuthToken())) {
						List<String> chunksIHave = new ArrayList<>();
						for (String chunkID : req.getChunksRequired()) {
							if (_chunkStorage.hasChunk(chunkID)) {
								chunksIHave.add(chunkID);
							}
						}
						response = new HaveChunksProtocolResponse(chunksIHave.toArray(new String[0]), me.getUserID());
					} else {
						response = new ErrorProtocolResponse("That's not my auth token", me.getUserID());
					}
				} else {
					_log.warn("Received unknown request type " + request.getType() + ", not processing");
					response = new ErrorProtocolResponse("I don't understand", me.getUserID());
				}
				_comms.reply(response, messageID);
			} catch (NoSuchUserException e) {
				_log.warn("Cannot respond to messages if I'm not logged in");
			} catch (FailedToStartCommsListenerException | InvalidMessageException e) {
				_log.error("Internal Error", e);
			} catch (InvalidMessageIDException e) {
				_log.warn("Could not reply to request because I don't have a MessageID for it");
			}
		}

		@Override public void responseReceived(ProtocolMessage response, MessageID messageID) {
			_log.error("Received a response in the default handler - this should not be.");
		}

		@Override public void error(Throwable t) {
			_log.error("Unexpected error in default handler", t);
		}

		@Override public void error(String message, boolean shouldReply, MessageID messageId) {
			_log.error("Unexpected error in default handler: " + message);
			try {
				CurrentUser me = _model.getCurrentUser();
				if (shouldReply) {
					_comms.reply(new ErrorProtocolResponse(message, me.getUserID()), messageId);
				}
			} catch (NoSuchUserException e) {
				_log.warn("Can't reply if not logged in");
			} catch (FailedToStartCommsListenerException | InvalidMessageIDException | InvalidMessageException e) {
				_log.warn("Failed to send error response", e);
			}
		}
	}
}
