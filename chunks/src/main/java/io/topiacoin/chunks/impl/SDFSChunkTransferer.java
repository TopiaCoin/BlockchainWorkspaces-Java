package io.topiacoin.chunks.impl;

import io.topiacoin.chunks.exceptions.CommsListenerNotStartedException;
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
import io.topiacoin.model.Member;
import io.topiacoin.model.MemberNode;
import io.topiacoin.model.User;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.NoSuchUserException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SDFSChunkTransferer implements ChunkTransferer {
	private static final Log _log = LogFactory.getLog(SDFSChunkTransferer.class);
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
	 * @throws IllegalStateException if the data model doesn't contain any member nodes, or if I can't infer the current logged in user
	 */
	@Override public void fetchChunksRemotely(List<String> chunkIDs, String containerID, final ChunksTransferHandler chunksHandler, final Object state) {
		CurrentUser me;
		try {
			me = _model.getCurrentUser();
		} catch (NoSuchUserException e) {
			throw new IllegalStateException("Cannot fetch chunks - cannot determine current logged in user", e);
		}
		List<MemberNode> memberNodes = fetchMemberNodes(containerID);
		if (memberNodes == null || memberNodes.isEmpty()) {
			throw new IllegalStateException("MemberNodes cannot be null or blank");
		}
		final Map<MessageID, MemberNode> memberMessages = new HashMap<>();
		final ChunkRetrievalStrategy strategy = _chunkRetrievalStrategyFactory.createStrategy(chunkIDs);
		ProtocolCommsResponseHandler handler = new ProtocolCommsResponseHandler() {
			boolean isExecuting = false;

			@Override public void responseReceived(ProtocolMessage response, MessageID messageID) {
				MemberNode memNode = memberMessages.remove(messageID);
				if (response instanceof HaveChunksProtocolResponse) {
					HaveChunksProtocolResponse responseImpl = (HaveChunksProtocolResponse) response;
					try {
						User sender = _model.getUserByID(responseImpl.getUserID());
						if (responseImpl.verify(sender.getPublicKey())) {
							strategy.submitLocationResponse(responseImpl, memNode);
						} else {
							_log.warn("Response from " + responseImpl.getUserID() + " had an invalid signature - ignoring message");
						}
					} catch (NoSuchUserException e) {
						_log.warn("Received a message from an unknown user - ignoring message");
					} catch (SignatureException e) {
						_log.warn("Response from " + responseImpl.getUserID() + " had an malformed signature - ignoring message");
					} catch (InvalidKeyException e) {
						_log.error(
								"Model info for " + responseImpl.getUserID() + " contains an invalid Public Key, cannot validate signature - ignoring message");
					}
				} else if (response instanceof ErrorProtocolResponse) {
					strategy.submitLocationResponse((ErrorProtocolResponse) response, memNode);
				} else {
					_log.warn("Received an unxepected response type: " + response.getType());
				}
				checkForCompletion();
			}

			@Override public void error(Throwable t, MessageID messageID) {
				memberMessages.remove(messageID);
				_log.error("Error determining chunk locations", t);
				checkForCompletion();
			}

			@Override public void error(String message, boolean shouldReply, MessageID messageID) {
				memberMessages.remove(messageID);
				_log.warn("Error determining chunk locations: " + message);
				checkForCompletion();
			}

			private void checkForCompletion() {
				boolean allMessagesFetched = memberMessages.isEmpty();
				if (allMessagesFetched) {
					strategy.allResponsesSubmitted();
				}
				if (strategy.isCompletePlan()) {
					if (!isExecuting) {
						isExecuting = true;
						executeStrategy(strategy, chunksHandler, state, me);
					}
				} else if (allMessagesFetched) {
					chunksHandler.failedToBuildFetchPlan();
				}
			}
		};
		QueryChunksProtocolRequest request;
		for (MemberNode memberNode : memberNodes) {
			request = new QueryChunksProtocolRequest(chunkIDs.toArray(new String[chunkIDs.size()]), me, memberNode);
			try {
				request.sign(me.getPrivateKey());
				MessageID messageId = _comms.sendMessage(memberNode, request, handler);
				memberMessages.put(messageId, memberNode);
			} catch (InvalidKeyException | CommsListenerNotStartedException | InvalidMessageException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<MemberNode> fetchMemberNodes(String containerID) {
		List<MemberNode> toReturn = _model.getMemberNodesForContainer(containerID);
		toReturn.remove(_myMemberNode);
		return toReturn;
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

	private void executeStrategy(ChunkRetrievalStrategy strategy, ChunksTransferHandler chunksHandler, Object state, CurrentUser me) {
		final ChunkRetrievalPlan plan = strategy.getPlan();
		final Map<MessageID, String> chunkRequests = new HashMap<>();
		ChunkRetrievalPlan.PlanTask task;
		FetchChunkProtocolRequest request;
		ProtocolCommsResponseHandler handler = new ProtocolCommsResponseHandler() {
			private void executeNextTask() {
				ChunkRetrievalPlan.PlanTask task;
				while ((task = plan.getNextTask()) != null) {
					FetchChunkProtocolRequest request = new FetchChunkProtocolRequest(task.chunkID, me, task.source);
					try {
						request.sign(me.getPrivateKey());
						MessageID messageID = _comms.sendMessage(task.source, request, this);
						chunkRequests.put(messageID, task.chunkID);
					} catch (InvalidKeyException | IOException | InvalidMessageException | CommsListenerNotStartedException e) {
						_log.warn("Failed to fetch chunk", e);
						plan.markChunkAsFailed(task.chunkID);
					}
				}
				if (plan.isComplete()) {
					List<String> failedChunks = plan.getFailedChunks();
					if (failedChunks.isEmpty()) {
						chunksHandler.fetchedAllChunksSuccessfully(state);
					} else {
						for (String chunk : failedChunks) {
							chunksHandler.failedToFetchChunk(chunk, "Nobody has this chunk", null, state);
						}
						chunksHandler.failedToFetchAllChunks(state);
					}
				}
			}

			@Override public void responseReceived(ProtocolMessage response, MessageID messageID) {
				String chunkID = chunkRequests.remove(messageID);
				if (response instanceof GiveChunkProtocolResponse) {
					GiveChunkProtocolResponse responseImpl = (GiveChunkProtocolResponse) response;
					try {
						User sender = _model.getUserByID(responseImpl.getUserID());
						if (responseImpl.verify(sender.getPublicKey())) {
							plan.markChunkAsFetched(chunkID);
							try {
								_chunkStorage.addChunk(chunkID, new ByteArrayInputStream(responseImpl.getChunkData()), null, true);
							} catch (DuplicateChunkException e) {
								//Ok, whatever
							}
							chunksHandler.didFetchChunk(chunkID, state);
						} else {
							_log.warn("Response from " + responseImpl.getUserID() + " had an invalid signature - ignoring message");
						}
					} catch (InsufficientSpaceException | IOException | InvalidReservationException e) {
						_log.error("Failed to store chunk", e);
						plan.markChunkAsFailed(chunkID);
					} catch (NoSuchUserException e) {
						_log.warn("Received a chunk from an unknown user - ignoring message", e);
						plan.markChunkAsFailed(chunkID);
					} catch (SignatureException e) {
						_log.warn("Response from " + responseImpl.getUserID() + " had an malformed signature - ignoring message");
						plan.markChunkAsFailed(chunkID);
					} catch (InvalidKeyException e) {
						_log.error(
								"Model info for " + responseImpl.getUserID() + " contains an invalid Public Key, cannot validate signature - ignoring message");
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
			request = new FetchChunkProtocolRequest(task.chunkID, me, task.source);
			try {
				request.sign(me.getPrivateKey());
				MessageID messageID = _comms.sendMessage(task.source, request, handler);
				chunkRequests.put(messageID, task.chunkID);
			} catch (InvalidKeyException | IOException | InvalidMessageException | CommsListenerNotStartedException e) {
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
					FetchChunkProtocolRequest requestImpl = (FetchChunkProtocolRequest) request;
					User sender = _model.getUserByID(requestImpl.getUserID());
					boolean sigIsValid = false;
					try {
						sigIsValid = request.verify(sender.getPublicKey());
					} catch (SignatureException e) {
						_log.warn("Request from " + requestImpl.getUserID() + " had an malformed signature - ignoring message");
					} catch (InvalidKeyException e) {
						_log.error(
								"Model info for " + requestImpl.getUserID() + " contains an invalid Public Key, cannot validate signature - ignoring message");
					}
					if(userIsAllowedToAskMeForChunks(sender)) {
						if (sigIsValid) {
							if (requestImpl.getAuthToken().equals(_myMemberNode.getAuthToken())) {
								if (_chunkStorage.hasChunk(requestImpl.getChunkID())) {
									try {
										byte[] data = _chunkStorage.getChunkData(requestImpl.getChunkID());
										response = new GiveChunkProtocolResponse(requestImpl.getChunkID(), data, me.getUserID());
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
						} else {
							response = new ErrorProtocolResponse("That's an invalid signature", me.getUserID());
						}
					} else {
						response = new ErrorProtocolResponse("I don't know you", me.getUserID());
					}
				} else if (request instanceof QueryChunksProtocolRequest) {
					QueryChunksProtocolRequest requestImpl = (QueryChunksProtocolRequest) request;
					User sender = _model.getUserByID(requestImpl.getUserID());
					boolean sigIsValid = false;
					try {
						sigIsValid = request.verify(sender.getPublicKey());
					} catch (SignatureException e) {
						_log.warn("Request from " + requestImpl.getUserID() + " had an malformed signature - ignoring message");
					} catch (InvalidKeyException e) {
						_log.error("Model info for " + requestImpl.getUserID() + " contains an invalid Public Key, cannot validate signature - ignoring message");
					}
					if(userIsAllowedToAskMeForChunks(sender)) {
						if (sigIsValid) {
							if (requestImpl.getAuthToken().equals(_myMemberNode.getAuthToken())) {
								List<String> chunksIHave = new ArrayList<>();
								for (String chunkID : requestImpl.getChunksRequired()) {
									if (_chunkStorage.hasChunk(chunkID)) {
										chunksIHave.add(chunkID);
									}
								}
								response = new HaveChunksProtocolResponse(chunksIHave.toArray(new String[chunksIHave.size()]), me.getUserID());
							} else {
								response = new ErrorProtocolResponse("That's not my auth token", me.getUserID());
							}
						} else {
							response = new ErrorProtocolResponse("That's an invalid signature", me.getUserID());
						}
					} else {
						response = new ErrorProtocolResponse("I don't know you", me.getUserID());
					}
				} else {
					_log.warn("Received unknown request type " + request.getType() + ", not processing");
					response = new ErrorProtocolResponse("I don't understand", me.getUserID());
				}
				response.sign(me.getPrivateKey());
				_comms.reply(response, messageID);
			} catch (NoSuchUserException e) {
				_log.warn("Cannot respond to messages if I'm not logged in");
			} catch (CommsListenerNotStartedException | InvalidMessageException e) {
				_log.error("Internal Error", e);
			} catch (InvalidMessageIDException e) {
				_log.warn("Could not reply to request because I don't have a MessageID for it");
			} catch (InvalidKeyException e) {
				_log.error("Could not sign a message with my private key. That's very bad");
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
					ErrorProtocolResponse resp = new ErrorProtocolResponse(message, me.getUserID());
					resp.sign(me.getPrivateKey());
					_comms.reply(resp, messageId);
				}
			} catch (NoSuchUserException e) {
				_log.warn("Can't reply if not logged in");
			} catch (CommsListenerNotStartedException | InvalidMessageIDException | InvalidMessageException e) {
				_log.warn("Failed to send error response", e);
			} catch (InvalidKeyException e) {
				_log.error("Could not sign a message with my private key. That's very bad");
			}
		}
	}

	private boolean userIsAllowedToAskMeForChunks(User user) {
		for(Workspace workspace : _model.getWorkspaces()) {
			for(Member m : workspace.getMembers()) {
				if(m.getUserID().equals(user.getUserID())) {
					return true;
				}
			}
		}
		return false;
	}
}
