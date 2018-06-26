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
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.Member;
import io.topiacoin.model.Message;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchMessageException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import io.topiacoin.model.exceptions.WorkspaceAlreadyExistsException;
import io.topiacoin.workspace.blockchain.exceptions.BlockchainException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EOSAdapter {

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

    public Members getMembers(long guid, Long continuationToken) throws NoSuchWorkspaceException, BlockchainException {

        List<Member> members = new ArrayList<>();
        boolean hasMore = false;
        long newContinuationToken = -1;

        try {
            long lowerBound = (continuationToken != null ? continuationToken : 0);
            long upperBound = -1;
            TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount,
                    Long.toString(guid),
                    "membership",
                    Long.toString(lowerBound),
                    Long.toString(upperBound),
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
                Member member = new Member(userID, status, inviteDate, inviterID, authToken);
                members.add(member);
                newContinuationToken = (Integer) row.get("id");
            }
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return new Members(members, hasMore, (hasMore ? newContinuationToken : -1));
    }

    public Member getMember(long guid, String member) {
        throw new NotImplementedException("Awaiting bug fix in EOS");
//        return null ;
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
            args.put("parentID", file.getParentID());
            args.put("fileID", file.getEntryID());
            List<FileVersion> versions = file.getVersions();
            String versionID = "0x00000000000000000000000000000000";
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
            args.put("versionID", versionID);

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

    public Files getFiles(long guid) throws NoSuchWorkspaceException, BlockchainException {
        return getFiles(guid, null);
    }

    public Files getFiles(long guid, Long continuationToken) throws NoSuchWorkspaceException, BlockchainException {

        List<File> files = new ArrayList<>();
        boolean hasMore = false;
        long newContinuationToken = -1;

        try {
            long lower_bound = (continuationToken != null ? continuationToken : 0);
            long upper_bound = -1;
            TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount,
                    Long.toString(guid),
                    "files",
                    Long.toString(lower_bound),
                    Long.toString(upper_bound),
                    100,
                    true);
            System.out.println("rows: " + rows);

            hasMore = rows.more;

            for (Map<String, Object> row : rows.rows) {
                String metadata = (String) row.get("metadata");
                File file = convertRowToFile(guid, metadata);
                files.add(file);
                newContinuationToken = (Integer) row.get("id");
            }
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return new Files(files, hasMore, (hasMore ? newContinuationToken : -1));
    }

    public File getFile(long guid, String user, String fileID, String versionID) throws NoSuchWorkspaceException, NoSuchFileException, BlockchainException {

        if (true) {
            throw new NotImplementedException("Awaiting bug fix in EOS RPC Interface");
        }

        File file = null;

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
                file = new File(name, mimeType, entryID, Long.toString(guid), parentID, isFolder, status, lockOwner, null);
                if (TextUtils.isEmpty(versionID) || row.get("versionID").equals(versionID)) {
                    break;
                }
            }
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

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

    public Messages getMessages(long guid, Long continuationToken) throws NoSuchWorkspaceException, BlockchainException {

        List<Message> messages = new ArrayList<>();
        boolean hasMore = false;
        long newContinuationToken = -1;

        try {
            long lower_bound = (continuationToken != null ? continuationToken : 0);
            long upper_bound = -1;
            TableRows rows = _eosRpcAdapter.chain().getTableRows(contractAccount, Long.toString(guid), "messages", 100, true);
            System.out.println("rows: " + rows);

            for (Map<String, Object> row : rows.rows) {
                String author = (String) row.get("author");
                String msgID = (String) row.get("msgID");
                long seq = (Integer) row.get("id");
                long timestamp = (Integer) row.get("timestamp");
                String text = (String) row.get("text");
                String mimeType = (String) row.get("mimeType");
                byte[] digSig = null;
                Message message = new Message(author, Long.toString(guid), msgID, seq, timestamp, text, mimeType, digSig);
                messages.add(message);
                newContinuationToken = (Integer) row.get("id");
            }
        } catch (ChainException e) {
            throw new BlockchainException("An exception occurred communicating with the blockchain", e.getCause());
        }

        return new Messages(messages, hasMore, (hasMore ? newContinuationToken : -1));
    }

    public void getMessage() {
        throw new NotImplementedException("This method does not work due to bugs in the EOS software");
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


    // ======== Testing Methods ========

    public long getLastBlockTime() {
        return _lastModified;
    }

    void updateLastBlockTime(long time) {
        _lastModified = time;
    }
}
