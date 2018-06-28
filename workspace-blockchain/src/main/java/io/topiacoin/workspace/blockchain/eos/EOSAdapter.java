package io.topiacoin.workspace.blockchain.eos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.topiacoin.eosrpcadapter.EOSRPCAdapter;
import io.topiacoin.eosrpcadapter.exceptions.ChainException;
import io.topiacoin.eosrpcadapter.exceptions.WalletException;
import io.topiacoin.eosrpcadapter.messages.ChainInfo;
import io.topiacoin.eosrpcadapter.messages.ErrorResponse;
import io.topiacoin.eosrpcadapter.messages.RequiredKeys;
import io.topiacoin.eosrpcadapter.messages.SignedTransaction;
import io.topiacoin.eosrpcadapter.messages.TableRows;
import io.topiacoin.eosrpcadapter.messages.Transaction;
import io.topiacoin.model.File;
import io.topiacoin.model.FileTag;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.FileVersionReceipt;
import io.topiacoin.model.Member;
import io.topiacoin.model.Message;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchMessageException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import io.topiacoin.model.exceptions.WorkspaceAlreadyExistsException;
import io.topiacoin.workspace.blockchain.exceptions.BlockchainException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EOSAdapter {

    public static final String NULL_UINT128 = "0x00000000000000000000000000000000";
    private final Log _log = LogFactory.getLog(this.getClass());

    private String eosNodeURL;
    private String eosWalletURL;
    private URL nodeURL;
    private URL walletURL;
    private EOSRPCAdapter _eosRpcAdapter;
    private long _lastModified = 0;
    private final String contractAccount;

    public EOSAdapter(String eosNodeURL, String eosWalletURL) {
        this.eosNodeURL = eosNodeURL;
        this.eosWalletURL = eosWalletURL;
        this.contractAccount = "inita";
    }

    @PostConstruct
    public void initialize() throws RuntimeException {
        try {
            nodeURL = new URL(eosNodeURL + "/");
            walletURL = new URL(eosWalletURL + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to parse the Node URL", e);
        }

        _eosRpcAdapter = new EOSRPCAdapter(nodeURL, walletURL);
    }

    @PreDestroy
    public void shutdown() {

    }

    // ======== Public Methods ========

    public void initializeWorkspace(long guid, String owner, String name, String description, String ownerKey) throws WorkspaceAlreadyExistsException, BlockchainException {

        try {

            Map<String, Object> args = new HashMap<>();
            args.put("owner", owner);
            args.put("guid", guid);
            args.put("workspaceName", name);
            args.put("workspaceDescription", description);
            args.put("key", ownerKey);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(owner);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(owner, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "create", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof WorkspaceAlreadyExistsException) {
                    throw (WorkspaceAlreadyExistsException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException("An exception occurred communicating with the chain", e.getCause());
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public WorkspaceInfo getWorkspaceInfo(long guid) throws NoSuchWorkspaceException, BlockchainException {

        WorkspaceInfo workspaceInfo = null;

        try {
            TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount, Long.toString(guid), "workspaces", 100, true);
            System.out.println("rows: " + rows);
            if (rows.rows.size() == 0) {
                throw new NoSuchWorkspaceException("The requested workspace does not exist");
            }

            String name = (String) rows.rows.get(0).get("name");
            String description = (String) rows.rows.get(0).get("description");
            String owner = (String) rows.rows.get(0).get("owner");
            String newOwner = (String) rows.rows.get(0).get("newowner");
            workspaceInfo = new WorkspaceInfo(guid, name, description, owner, newOwner);
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return workspaceInfo;
    }

    public void setWorkspaceDescription(long guid, String owner, String description) throws NoSuchWorkspaceException, BlockchainException {

        try {

            Map<String, Object> args = new HashMap<>();
            args.put("user", owner);
            args.put("guid", guid);
            args.put("workspaceDescription", description);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(owner);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(owner, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "update", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void offerOwnership(long guid, String owner, String newOwner) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("newowner", newOwner);
            args.put("guid", guid);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(owner);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(owner, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "offerowner", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void acceptOwnership(long guid, String user) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "acceptowner", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void rescindOwnership(long guid, String user) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "rescindowner", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void destroy(long guid, String user) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "destroy", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void addMember(long guid, String owner, String invitee, String inviteeKey) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("inviter", owner);
            args.put("invitee", invitee);
            args.put("key", inviteeKey);
            args.put("permissions", new ArrayList<>());

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(owner);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(owner, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "invite", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void removeMember(long guid, String user, String member) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("remover", user);
            args.put("removee", member);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "remove", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public Members getMembers(long guid) throws NoSuchWorkspaceException, BlockchainException {
        return getMembers(guid, null);
    }

    public Members getMembers(long guid, Object continuationToken) throws NoSuchWorkspaceException, BlockchainException {

        List<Member> members = new ArrayList<>();
        boolean hasMore = false;
        Object newContinuationToken = null;

        try {
            String lowerBound = (continuationToken != null ? continuationToken.toString() : "0");
            String upperBound = "-1";
            TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount,
                    Long.toString(guid),
                    "membership",
                    lowerBound,
                    upperBound,
                    100,
                    true);
            System.out.println("rows: " + rows);
            if (rows.rows.size() == 0) {
                throw new NoSuchWorkspaceException("The requested workspace does not exist");
            }

            hasMore = rows.more;

            for (Map<String, Object> row : rows.rows) {
                String userID = (String) row.get("user");
                int status = (Integer) row.get("status");
                long inviteDate = 0;//Long.parseLong(row.get("inviteDate"));
                String inviterID = (String) row.get("inviter");
                String authToken = null;
                Member member = new Member(userID, status, inviteDate, inviterID, authToken, null);
                // Get the lock owner, if any, of this member
                member.setLockOwner(getLockHolder(guid, userID));

                members.add(member);
                newContinuationToken = getNextToken(row.get("id"));
            }
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return new Members(members, hasMore, (hasMore ? newContinuationToken : null));
    }

    public Member getMember(long guid, String member) throws NoSuchWorkspaceException, BlockchainException {

        // TODO - Rewrite this method once the EOS bug that prevents usage of secondary indexes is fixed.
        Member foundMember = null ;
        Members members = null;
        Object continuationToken = null ;

        do {
            members = getMembers(guid, continuationToken);
            for ( Member curMember : members.getMembers() ) {
                if ( curMember.getUserID().equals(member) ) {
                    foundMember = curMember ;
                    break ;
                }
            }
            continuationToken = members.getContinuationToken();
        } while ( foundMember == null && members.isHasMore());

//        throw new NotImplementedException("Awaiting bug fix in EOS");
        return foundMember ;
    }

    public void acceptInvitation(long guid, String invitee) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("invitee", invitee);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(invitee);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(invitee, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "accept", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void declineInvitation(long guid, String invitee) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("invitee", invitee);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(invitee);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(invitee, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "decline", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void lockMember(long guid, String user, String targetUser) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("locker", user);
            args.put("lockee", targetUser);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "lockmember", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void unlockMember(long guid, String user, String targetUser) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("locker", user);
            args.put("lockee", targetUser);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "unlockmember", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void addFile(long guid, String user, File file) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("uploader", user);
            args.put("parentID", (file.getParentID() != null ? file.getParentID() : NULL_UINT128));
            args.put("fileID", file.getEntryID());
            List<FileVersion> versions = file.getVersions();
            String versionID = NULL_UINT128;
            List<String> ancestorVersionIDs = null;
            if (versions != null && versions.size() > 0) {
                versionID = versions.get(0).getVersionID();
                ancestorVersionIDs = versions.get(0).getAncestorVersionIDs();
            }
            args.put("versionID", versionID);
            args.put("ancestorVersionID", ancestorVersionIDs);

            String metadata = convertFileToRow(file);

            args.put("fileMetadata", metadata);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "addfile", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void removeFile(long guid, String user, String fileID, String versionID) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("remover", user);
            args.put("fileID", fileID);
            args.put("versionID", (versionID != null ? versionID : NULL_UINT128));

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "removefile", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public Files getFiles(long guid, String user) throws NoSuchWorkspaceException, BlockchainException {
        return getFiles(guid, user, null);
    }

    public Files getFiles(long guid, String user, Object continuationToken) throws NoSuchWorkspaceException, BlockchainException {

        List<File> files = new ArrayList<>();
        boolean hasMore = false;
        Object newContinuationToken = null;

        try {
            String lower_bound = (continuationToken != null ? continuationToken.toString() : "0");
            String upper_bound = "-1";
            TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount,
                    Long.toString(guid),
                    "files",
                    lower_bound,
                    upper_bound,
                    100,
                    true);
//            System.out.println("rows: " + rows);

            hasMore = rows.more;

            for (Map<String, Object> row : rows.rows) {
                String metadata = (String) row.get("metadata");
                File file = convertRowToFile(guid, metadata);

                // Get the Lock Status of this file
                String fileLockOwner = getLockHolder(guid, file.getEntryID());
                file.setLockOwner(fileLockOwner);

                if ( file.getVersions() != null && file.getVersions().size() > 0 ) {
                    FileVersion fileVersion = file.getVersions().get(0);
                    String versionID = fileVersion.getVersionID();

                    // Get the Lock Status of this file version
                    String versionLockOwner = getLockHolder(guid, versionID);
                    fileVersion.setLockOwner(versionLockOwner) ;

                    // Get the User File Tags of this file version
                    List<FileTag> tags = getFileTags(guid, file.getEntryID(), versionID, "public", user);
                    fileVersion.setUserTags(tags);

                    // Get the File Receipts of this file version
                    List<FileVersionReceipt> fileReceipts = getFileReceipts(guid, file.getEntryID(), versionID);
                    fileVersion.setReceipts(fileReceipts);
                }
                files.add(file);
                newContinuationToken = getNextToken(row.get("id"));
            }
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return new Files(files, hasMore, (hasMore ? newContinuationToken : null));
    }

    public File getFile(long guid, String fileID, String versionID, String user) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        File file = null;

        Files files = null ;
        Object continuationToken = null;

        do {
            files = getFiles(guid, user, continuationToken);
            for ( File curFile : files.getFiles()) {
                if ( curFile.getEntryID().equals(fileID) && (versionID == null || curFile.getVersions().get(0).getVersionID().equals(versionID))) {
                    file = curFile;
                    break ;
                }
            }
            continuationToken = files.getContinuationToken();
        } while (file == null && files.isHasMore()) ;

        if ( file == null ) {
            throw new NoSuchFileException("Unable to find the specified file");
        }

        // TODO - Switch back to using this code once the EOS bug that prevents the use of secondary indexes is fixed
        /*
        try {
            TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount, Long.toString(guid), "files", "fileID", fileID, fileID, 100, true);
            System.out.println("rows: " + rows);

            for (Map<String, Object> row : rows.rows) {
                String name = null;
                String mimeType = null;
                String entryID = (String) row.get("fileID");
                String parentID = (String) row.get("parentID");
                boolean isFolder = false;
                int status = (Integer) row.get("status");
                String lockOwner = null;
                file = new File(name, mimeType, entryID, guid, parentID, isFolder, status, lockOwner, null);
                if (TextUtils.isEmpty(versionID) || row.get("versionID").equals(versionID)) {
                    break;
                }
            }
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }
*/
        return file;
    }

    public void acknowledgeFile(long guid, String user, String fileID, String versionID) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("user", user);
            args.put("fileID", fileID);
            args.put("versionID", versionID);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "ackfile", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void addMessage(long guid, String user, String message, String mimeType) throws NoSuchWorkspaceException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("author", user);
            args.put("message", message);
            args.put("mimeType", mimeType);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "addmessage", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public Messages getMessages(long guid) throws NoSuchWorkspaceException, BlockchainException {
        return getMessages(guid, null);
    }

    public Messages getMessages(long guid, Object continuationToken) throws NoSuchWorkspaceException, BlockchainException {

        List<Message> messages = new ArrayList<>();
        boolean hasMore = false;
        Object newContinuationToken = null;

        try {
            String lower_bound = (continuationToken != null ? continuationToken.toString() : "0");
            String upper_bound = "-1";
            TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount,
                    Long.toString(guid),
                    "messages",
                    lower_bound,
                    upper_bound,
                    100,
                    true);
            System.out.println("rows: " + rows);

            hasMore = rows.more;

            for (Map<String, Object> row : rows.rows) {
                String author = (String) row.get("author");
                String msgID = (String) row.get("msgID");
                long seq = (Integer) row.get("id");
                long timestamp = (Integer) row.get("timestamp");
                String text = (String) row.get("text");
                String mimeType = (String) row.get("mimeType");
                byte[] digSig = null;
                Message message = new Message(author, msgID, guid, seq, timestamp, text, mimeType, digSig);
                messages.add(message);
                newContinuationToken = getNextToken(row.get("id"));
            }
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return new Messages(messages, hasMore, (hasMore ? newContinuationToken : null));
    }

    public Message getMessage(long guid, String msgID) throws NoSuchWorkspaceException, NoSuchMessageException, BlockchainException {

        // TODO - Rewrite this method once the EOS bug that prevents usage of secondary indexes is fixed.
        Message message = null ;
        Messages messages = null ;
        Object continuationToken = null ;

        do {
            messages = getMessages(guid, continuationToken);
            for ( Message curMessage : messages.getMessages()) {
                if ( curMessage.getEntityID().equals(msgID)) {
                    message = curMessage;
                    break;
                }
            }
            continuationToken = messages.getContinuationToken();
        } while ( message == null && messages.isHasMore() );

        if ( message == null ) {
            throw new NoSuchMessageException("The specified message does not exist") ;
        }

        return message ;
    }

    public void acknowledgeMessage(long guid, String user, String msgID) throws NoSuchWorkspaceException, NoSuchMessageException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("user", user);
            args.put("msgID", msgID);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "ackmessage", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void addFileTag(long guid, String user, String fileID, String versionID, String tagValue, boolean isPublic) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("user", user);
            args.put("fileID", fileID);
            args.put("versionID", versionID);
            args.put("isPublic", (isPublic ? 1 : 0));
            args.put("value", tagValue);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "addtag", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void removeFileTag(long guid, String user, String fileID, String versionID, String tagValue, boolean isPublic) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("user", user);
            args.put("fileID", fileID);
            args.put("versionID", versionID);
            args.put("isPublic", (isPublic ? 1 : 0));
            args.put("value", tagValue);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "removetag", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void lockFile(long guid, String user, String fileID) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("user", user);
            args.put("fileID", fileID);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "lockfile", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void unlockFile(long guid, String user, String fileID) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("user", user);
            args.put("fileID", fileID);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "unlockfile", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void lockFileVersion(long guid, String user, String fileID, String versionID) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("user", user);
            args.put("fileID", fileID);
            args.put("versionID", versionID);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "lockver", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public void unlockFileVersion(long guid, String user, String fileID, String versionID) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        try {
            Map<String, Object> args = new HashMap<>();
            args.put("guid", guid);
            args.put("user", user);
            args.put("fileID", fileID);
            args.put("versionID", versionID);

            ChainInfo info = _eosRpcAdapter.chain().getInfo();

            List<String> scopes = new ArrayList<>();
            scopes.add(user);

            List<Transaction.Authorization> authorizations = new ArrayList<>();
            authorizations.add(new Transaction.Authorization(user, "active"));

            Date expirationDate = new Date(System.currentTimeMillis() + 30000);

            Transaction initTX = _eosRpcAdapter.chain().createRawTransaction(contractAccount, "unlockver", args, scopes, authorizations, expirationDate);

            List<String> availableKeys = _eosRpcAdapter.wallet().getPublicKeys();
            RequiredKeys requiredKeys = _eosRpcAdapter.chain().getRequiredKeys(initTX, availableKeys);
            SignedTransaction signedInitTx = _eosRpcAdapter.wallet().signTransaction(initTX, requiredKeys.required_keys, info.chain_id);
            _eosRpcAdapter.chain().pushTransaction(signedInitTx);
        } catch (ChainException e) {
            if (e.getCause() instanceof ChainException) {
                Exception extractedException = extractExceptionForRootCause((ChainException) e.getCause());
                if (extractedException instanceof NoSuchWorkspaceException) {
                    throw (NoSuchWorkspaceException) extractedException;
                } else {
                    throw (BlockchainException) extractedException;
                }
            } else {
                throw new BlockchainException(e);
            }
        } catch (WalletException e) {
            throw new BlockchainException("An exception occurred communicating with the wallet", e.getCause());
        }
    }

    public String getLockHolder(long guid, String objectID) throws BlockchainException {
        String lockHolder = null;

        // TODO - Replace this implementation once the EOS bug that prevents use of secondary indexes is fixed.

        boolean hasMore = false;
        Object continuationToken = null;

        try {
            do {
                String lower_bound = (continuationToken != null ? continuationToken.toString() : "0");
                String upper_bound = "-1";
                TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount,
                        Long.toString(guid),
                        "locks",
                        lower_bound,
                        upper_bound,
                        100,
                        true);
//                System.out.println("rows: " + rows);

                hasMore = rows.more;

                for (Map<String, Object> row : rows.rows) {
                    String entityGuid = (String) row.get("guid");
                    String lockOwner = (String) row.get("lockOwner");
                    if (objectID.equals(entityGuid)) {
                        lockHolder = lockOwner;
                    }
                    continuationToken = getNextToken(row.get("id"));
                }
            } while (lockHolder == null && hasMore);
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return lockHolder;
    }

    public List<FileTag> getFileTags(long guid, String fileID, String versionID, String... scopes) throws BlockchainException {
        List<FileTag> fileTags = new ArrayList<>();

        // TODO - Replace this implementation once the EOS bug that prevents use of secondary indexes is fixed.

        List<String> desiredScopes = Arrays.asList(scopes);

        boolean hasMore = false;
        Object continuationToken = null;

        try {
            do {
                String lower_bound = (continuationToken != null ? continuationToken.toString() : "0");
                String upper_bound = "-1";
                TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount,
                        Long.toString(guid),
                        "filetags",
                        lower_bound,
                        upper_bound,
                        100,
                        true);
//                System.out.println("rows: " + rows);

                hasMore = rows.more;

                for (Map<String, Object> row : rows.rows) {
                    String fID = (String) row.get("fileID");
                    String vID = (String) row.get("versionID");
                    if ( fileID.equals(fID) && versionID.equals(vID) ) {
                        String tagScope = (String) row.get("scope");
                        String tagValue = (String) row.get("value");
                        if (desiredScopes.contains(tagScope)) {
                            FileTag fileTag = new FileTag(tagScope, tagValue);
                            fileTags.add(fileTag);
                        }
                    }
                    continuationToken = getNextToken(row.get("id"));
                }
            } while (hasMore);
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return fileTags;
    }

    public List<FileVersionReceipt> getFileReceipts(long guid, String fileID, String versionID) throws BlockchainException {
        List<FileVersionReceipt> fileReceipts = new ArrayList<>();

        // TODO - Replace this implementation once the EOS bug that prevents use of secondary indexes is fixed.

        boolean hasMore = false;
        Object continuationToken = null;

        try {
            do {
                String lower_bound = (continuationToken != null ? continuationToken.toString() : "0");
                String upper_bound = "-1";
                TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount,
                        Long.toString(guid),
                        "filereceipts",
                        lower_bound,
                        upper_bound,
                        100,
                        true);
//                System.out.println("rows: " + rows);

                hasMore = rows.more;

                for (Map<String, Object> row : rows.rows) {
                    String fID = (String) row.get("fileID");
                    String vID = (String) row.get("versionID");
                    long uID = (Long) row.get("user") ;
                    long timestamp = (Long) row.get("timestamp");

                    if (fID.equals(fileID) && vID.equals(versionID)) {
                        FileVersionReceipt fvr = new FileVersionReceipt(fID, vID, Long.toString(uID), timestamp);
                        fileReceipts.add(fvr);
                    }
                    continuationToken = getNextToken(row.get("id"));
                }
            } while (hasMore);
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return fileReceipts;
    }

    // ======== Private Methods ========

    private Exception extractExceptionForRootCause(ChainException cause) {
        Exception res = null;

        if (cause.getDetails() != null) {
            ErrorResponse details = cause.getDetails();
            ErrorResponse.Error eosError = details.error;
            if (eosError.code == 3050003) {
                // This is an assertion failure.  Start checking the details
                List<Map<String, Object>> errorDetails = eosError.details;
                String errorMessage = (String) errorDetails.get(0).get("message");
                if (errorMessage.contains("already exists")) {
                    res = new WorkspaceAlreadyExistsException("A workspace with the specified GUID already exists");
                } else {
                    res = new BlockchainException("Exception while communicating with the chain: " + errorMessage, cause);
                }
            } else {
                res = new BlockchainException("Exception while communicating with the chain", cause);
            }
        } else {
            res = new BlockchainException("Failed to initialized the workspace", cause);
        }

        return res;
    }

    @NotNull
    private File convertRowToFile(long guid, String metadata) {
        try {
            // Unmarshall the metadata JSON String to a Map and start pulling info out.
            ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(metadata, File.class);
        } catch (IOException e) {
            _log.warn("Error Unmarshalling File data: ", e);
            return null;
        }
    }

    private String convertFileToRow(File file) {
        try {
            // Marshall the object into a JSON String
            ObjectMapper mapper = new ObjectMapper();
            String metadata = mapper.writeValueAsString(file);

            return metadata;
        } catch (JsonProcessingException e) {
            _log.warn("Error Marshalling File object ", e);
            return null;
        }
    }

    private Object getNextToken(Object token) {
        Object newToken = null;

        BigInteger bi = new BigInteger(token.toString());
        newToken = bi.add(BigInteger.valueOf(1)).toString();

        return newToken;
    }


    // ======== Testing Methods ========

    public long getLastBlockTime() {
        return _lastModified;
    }

    void updateLastBlockTime(long time) {
        _lastModified = time;
    }
}
