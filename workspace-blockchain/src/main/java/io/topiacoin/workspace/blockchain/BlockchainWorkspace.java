package io.topiacoin.workspace.blockchain;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.WorkspacesAPI;
import io.topiacoin.core.callbacks.AcceptInvitationCallback;
import io.topiacoin.core.callbacks.AcknowledgeFileCallback;
import io.topiacoin.core.callbacks.AcknowledgeMessageCallback;
import io.topiacoin.core.callbacks.AddFileCallback;
import io.topiacoin.core.callbacks.AddFileTagCallback;
import io.topiacoin.core.callbacks.AddFileVersionCallback;
import io.topiacoin.core.callbacks.AddFolderCallback;
import io.topiacoin.core.callbacks.AddMessageCallback;
import io.topiacoin.core.callbacks.ConnectWorkspaceCallback;
import io.topiacoin.core.callbacks.CreateWorkspaceCallback;
import io.topiacoin.core.callbacks.DeclineInvitationCallback;
import io.topiacoin.core.callbacks.FetchFileVersionCallback;
import io.topiacoin.core.callbacks.InviteUserCallback;
import io.topiacoin.core.callbacks.LeaveWorkspaceCallback;
import io.topiacoin.core.callbacks.LockFileCallback;
import io.topiacoin.core.callbacks.RemoveFileCallback;
import io.topiacoin.core.callbacks.RemoveFileTagCallback;
import io.topiacoin.core.callbacks.RemoveFileVersionCallback;
import io.topiacoin.core.callbacks.RemoveFolderCallback;
import io.topiacoin.core.callbacks.RemoveUserCallback;
import io.topiacoin.core.callbacks.UnlockFileCallback;
import io.topiacoin.core.callbacks.UpdateWorkspaceDescriptionCallback;
import io.topiacoin.core.exceptions.NotLoggedInException;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.dht.SDFSDHTAccessor;
import io.topiacoin.model.DHTWorkspaceEntry;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.File;
import io.topiacoin.model.Message;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchMessageException;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import io.topiacoin.model.exceptions.WorkspaceAlreadyExistsException;
import io.topiacoin.util.Notification;
import io.topiacoin.util.NotificationCenter;
import io.topiacoin.util.NotificationHandler;
import io.topiacoin.workspace.blockchain.eos.EOSAdapter;
import io.topiacoin.workspace.blockchain.eos.EOSChainmail;
import io.topiacoin.workspace.blockchain.exceptions.BlockchainException;
import io.topiacoin.workspace.blockchain.exceptions.ChainAlreadyExistsException;
import io.topiacoin.workspace.blockchain.exceptions.NoSuchChainException;
import multichain.object.Block;
import org.apache.commons.lang.NotImplementedException;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class BlockchainWorkspace implements WorkspacesAPI {

    private RPCAdapterManager _adapterManager ;
    private DataModel _dataModel;
    private Chainmail _chainMail;
    private SDFSDHTAccessor _dhtAccessor;
    private Configuration _config;
    private NotificationCenter _notificationCenter;
    private String currentUserID = null;

    public BlockchainWorkspace(Configuration config) {
        _chainMail = new EOSChainmail(config);
        _adapterManager = new RPCAdapterManager(_chainMail);
        _dataModel = DataModel.getInstance();
        _config = config;
        _dhtAccessor = SDFSDHTAccessor.getInstance(_config, _dataModel);
        _notificationCenter = NotificationCenter.defaultCenter();
        _notificationCenter.addHandler(new NotificationHandler() {
            @Override public void handleNotification(Notification notification) {
                try {
                    currentUserID = _dataModel.getCurrentUser().getUserID();
                } catch (NoSuchUserException e) {
                    e.printStackTrace();
                }
            }
        }, "login", null);
    }

    /**
     * Requestes that the Blockchain Workspace API check all tracked workspaces for updates. This will cause the system
     * to check all of the tracked blockchains for changes. This functionality is periodically invoked by the API to
     * insure that the system is regularly updated with any new activity. This method is provided so that the client
     * application can trigger on demand updates, such as when requested by the user. This method does not guarentee
     * that an update will occur. In certain cases, the system may choose to ignore the request for updates, such as if
     * an update was recently performed (e.g. within the last 10 seconds).
     */
    @Override
    public void checkForUpdates() {

    }

    /**
     * Instructs the Blockchain API to connect to the blockchain for the specified workspace ID. This will start the
     * process of tracking the blockchain for new events. Calling this method for a workspace that is already being
     * tracked is effectively a NOP.
     * <p>
     * On successful connection of the workspace, a notification of type 'workspaceConnectionComplete' will be posted to
     * the notification center. The classifier of this notification will be the workspace ID. The notification info will
     * be empty.
     * <p>
     * On a failure to connect to the workspace, a notification of type 'workspaceConnectionFailed' will be posted to
     * the notification center.  The classifier of this notification will be the workspace ID.  The notification info
     * will include the reason for the failure under the 'reason' key.
     *
     * @param workspaceID
     * @param callback
     */
    @Override
    public void connectWorkspace(long workspaceID, ConnectWorkspaceCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Instruct ChainMail to connect to the specified workspace's blockchain
        DHTWorkspaceEntry dhtWorkspace = _dhtAccessor.fetchDHTWorkspace(workspaceID);
        if(dhtWorkspace == null) {
            throw new NoSuchWorkspaceException();
        }
        String currentUserID;
        try {
            currentUserID = _dataModel.getCurrentUser().getUserID();
        } catch (NoSuchUserException e) {
            throw new NotLoggedInException("", e);
        }
        try {
            _chainMail.startBlockchain(currentUserID, workspaceID, dhtWorkspace.getMemberNodes());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchChainException e) {
            try {
                _chainMail.createBlockchain(currentUserID, workspaceID);
                _chainMail.startBlockchain(currentUserID, workspaceID, null);
            } catch (ChainAlreadyExistsException | NoSuchChainException | IOException e1) {
                e1.printStackTrace();
            }
        }
        // On connection, ask the RPC Adapter Manager if it has an adapter for the specified workspace chain.
        EOSAdapter adapter = _adapterManager.getRPCAdapter(workspaceID);
    }

    /**
     * Creates a new blockchain-based workspace. The new workspace is given the specified name and description. This
     * call will result in the creation of the workspace blockchain, and the insertion of the initial set of
     * transactions that establish the blockchain. Upon completion, a response will be sent that includes the GUID
     * assigned to the workspace.
     * <p>
     * This method returns the workspace GUID assigned to the new workspace.
     * <p>
     * On successful creation of the workspace, a notification of type 'workspaceCreationComplete' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will be
     * empty.
     * <p>
     * On failure to create the workspace, a notification of type 'workspaceCreationFailed' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID.  The notification info will
     * include the reason for the failure under the 'reason' key.
     *
     * @param workspaceName
     * @param workspaceDescription
     * @param callback
     */
    @Override
    public void createWorkspace(String workspaceName, String workspaceDescription, CreateWorkspaceCallback callback) throws NotLoggedInException {
        // Generate a GUID for the new workspace
        long workspaceID = generateWorkspaceGUID();
        // Instruct Chainmail to create a new workspace blockchain using the new workspace GUID.
        try {
            SecretKey workspaceKey = CryptoUtils.generateAESKey();
            String ownerKey = CryptoUtils.encryptWithPublicKeyToString(workspaceKey.getEncoded(), _dataModel.getCurrentUser().getPublicKey());
            _chainMail.createBlockchain(currentUserID, workspaceID);
            _chainMail.startBlockchain(currentUserID, workspaceID, null);
            // Once the chain has been created and started, get the RPC Adapter from the Manager.
            EOSAdapter adapter = _adapterManager.getRPCAdapter(workspaceID);
            // Instruct the RPC Adapter to initialize the chain with the workspace name, description, and user record for the currently logged in user.
            adapter.initializeWorkspace(workspaceID, currentUserID, workspaceName, workspaceDescription, ownerKey);
        } catch (ChainAlreadyExistsException | NoSuchChainException | IOException | CryptographicException | NoSuchUserException | WorkspaceAlreadyExistsException | BlockchainException e1) {
            e1.printStackTrace();
        }
    }

    private long generateWorkspaceGUID() {
        //TODO is this how we want to do this?
        return new Random().nextLong();
    }

    /**
     * Updates the description of the specified workspace. It is an error to specify a non-existent workspace. To remove
     * a description, specify a null or empty workspace description.
     * <p>
     * On successful connection of the workspace, a notification of type 'workspaceUpdateComplete' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will be
     * empty.
     * <p>
     * On failure to update the workspace, a notification of type 'workspaceUpdateFailed' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID. The notification info will
     * include the reason for the failure under the 'reason' key.
     *
     * @param workspaceToUpdate
     * @param callback
     */
    @Override
    public void updateWorkspaceDescription(Workspace workspaceToUpdate, UpdateWorkspaceDescriptionCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(workspaceToUpdate.getGuid());
        // Instruct the RPC Adapter to set the workspace description
        try {
            adapter.setWorkspaceDescription(workspaceToUpdate.getGuid(), currentUserID, workspaceToUpdate.getDescription());
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    private EOSAdapter getEOSAdapter(long guid) throws NotLoggedInException, NoSuchWorkspaceException {
        EOSAdapter adapter = _adapterManager.getRPCAdapter(guid);
        // If not found, tell Chainmail to start the blockchain, fetching the RPC Adapter upon completion.
        if(adapter == null) {
            connectWorkspace(guid, null);
            adapter = _adapterManager.getRPCAdapter(guid);
        }
        return adapter;
    }

    /**
     * Invites a user to a workspace, sending them the specified message along with the invitation. It is an error to
     * specify a non-existent workspace, or a non-existent userID. If no invite message is desired, specify null or an
     * empty string for the message.
     * <p>
     * On successful invitation to the workspace, a notification of type 'userInviteComplete' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will
     * contain the user ID of the invited user under the 'userID' key.
     * <p>
     * On failure to invite the user to the workspace, a notification of type 'userInviteFailed' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID.  The notification info will
     * contain the user ID of the invited user under the 'userID' key, and the reason for the failure under the 'reason'
     * key.
     *
     * @param workspace
     * @param userID
     * @param inviteMessage
     * @param callback
     */
    @Override
    public void inviteUser(Workspace workspace, String userID, String inviteMessage, InviteUserCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(workspace.getGuid());
        try {
            String ownerKey = CryptoUtils.encryptWithPublicKeyToString(workspace.getWorkspaceKey().getEncoded(), _dataModel.getUserByID(userID).getPublicKey());
            // Instruct the RPC Adapter to add an invitation record to the blockchain for the specified user.
            adapter.addMember(workspace.getGuid(), currentUserID, userID, ownerKey);
        } catch (CryptographicException | BlockchainException | NoSuchUserException e) {
            e.printStackTrace();
        }
    }

    /**
     * Accepts an invitation to a workspace. It is an error to specify a non-existent workspace. It is an error to
     * specify a workspace of which you are already a member.
     * <p>
     * Upon accepting the invitation to the workspace, a notification of type 'workspaceAcceptComplete' will be posted
     * to the notification center. The classifier of this notification will be the workspace ID. The notification info
     * will be empty.
     * <p>
     * On failure to accept a workspace invitation, a notification of type 'workspaceAcceptFailed' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID.  The notification info will
     * include the reason for the failure under the 'reason' key.
     *
     * @param workspace
     * @param callback
     */
    @Override
    public void acceptInvitation(Workspace workspace, AcceptInvitationCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(workspace.getGuid());
        // Instruct the RPC Adapter to accept the invitation to the specified workspace for the currently logged in user.
        try {
            adapter.acceptInvitation(workspace.getGuid(), currentUserID);
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * Declines an invitation to a workspace. It is an error to specify a non-existent workspace. Is is an error to
     * specify a workspace of which you are already a member.
     * <p>
     * Upon declining the invitation to the workspace, a notification of type 'workspaceDeclineComplete' will be posted
     * to the notification center. The classifier of this notification will be the workspace ID. The notification info
     * will be empty.
     * <p>
     * On failure to decline a workspace invitation, a notification of type 'workspaceDeclineFailed' will be posted to
     * the notification center.  The classifier of this notification will be the workspace ID. The notification info
     * will include the reason for the failure under the 'reason' key.
     *
     * @param workspace
     * @param callback
     */
    @Override
    public void declineInvitation(Workspace workspace, DeclineInvitationCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(workspace.getGuid());
        try {
            // Instruct the RPC Adapter to decline the invitation to the specified workspace for the currently logged in user.
            adapter.declineInvitation(workspace.getGuid(), currentUserID);
            // Instruct Chainmail to stop the blockchain for the declined workspace
            // Instruct Chainmail to delete the blockchain for the declined workspace.
            _chainMail.destroyBlockchain(workspace.getGuid());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an encrypted workspace key to a new workspace member. Normally, the inviter will send the encrypted
     * workspace key to the new member when it sees that they have accepted the invitation. It is an error to specify a
     * non-existent workspace GUID.
     * <p>
     * Note: We may want to simply incorporate this behavior into the workspace library itself. The library can wait
     * until it sees a workspace acceptance message, then automatically encrypt the key and send it to the new member.
     * <p>
     * Workspace getWorkspace(String workspaceGUID)
     * <p>
     * Note: Not sure that this method is required. I don't know what information we expect it to return.
     *
     * @param workspaceGUID
     * @param userID
     * @param encryptedWorkspaceKey
     */
    @Override
    public void deliverWorkspaceKey(String workspaceGUID, String userID, String encryptedWorkspaceKey) {
        //TODO implement me
        throw new NotImplementedException("");
    }

    /**
     * Removes the user from the specified workspace. The user will lose access to all content stored in the workspace.
     * In order to regain access to the content, another user must re-invite them to the workspace. If there are no
     * other users in the workspace, then the workspace is permanently lost. It is an error to specify a non-existent
     * workspace GUID.
     * <p>
     * Upon successfully leaving the workspace, a notification of type 'workspaceRemoveComplete' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will be
     * empty.
     * <p>
     * On failure to leave a workspace, a notification of type 'workspaceRemoveFailed' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID.  The notification info will
     * include the reason for the failure under the 'reason' key.
     *
     * @param workspace
     * @param callback
     */
    @Override
    public void leaveWorkspace(Workspace workspace, LeaveWorkspaceCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(workspace.getGuid());
        try {
            // Instruct the RPC Adapter to remove the currently logged in user from the workspace
            adapter.removeMember(workspace.getGuid(), currentUserID, currentUserID);
            // Instruct Chainmail to stop the blockchain for the workspace
            // Instruct Chainmail to delete the blockchain for the workspace
            _chainMail.destroyBlockchain(workspace.getGuid());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the specified member from the workspace. It is an error to specify a non-existent workspace GUID, or a
     * non-existent memberID. Once the user is removed from the workspace, they will lose access to all content stored
     * in the workspace. In order to regain access to the content, the user will have to be re-invited to the
     * workspace.
     * <p>
     * On successfully removing a user from the workspace, a notification of type 'userRemoveComplete' will be posted to
     * the notification center. The classifier of this notification will be the workspace ID. The notification info will
     * contain the userID of the removed user under the 'userID' key.
     * <p>
     * On failure to remove a user from a workspace, a notification of type 'userRemoveFailed' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID. The notification info will
     * contain the user ID of the user under the 'userID' key, and the reason for the failure under the 'reason' key.
     *
     * @param workspace
     * @param memberID
     * @param callback
     */
    @Override
    public void removeUserFromWorkspace(Workspace workspace, String memberID, RemoveUserCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(memberID.equals(currentUserID)) {
            leaveWorkspace(workspace, new LeaveWorkspaceCallback() {
                //call callback.whatever();
            });
        } else {
            // Get the RPC Adapter for the specified workspace
            EOSAdapter adapter = getEOSAdapter(workspace.getGuid());
            // Instruct the RPC Adapter to remove the specified user from the workspace
            try {
                adapter.removeMember(workspace.getGuid(), currentUserID, memberID);
            } catch (BlockchainException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds a file to the specified workspace with the specified parent. If parentGUID is null or an empty string, then
     * the file is placed in the root of the workspace. It is an error to specify a non-existent workspace GUID, or a
     * non-existent folder GUID. The library will handle the process of chunking, encrypting, and sharing the file
     * content with other workspace members. This method returns the file GUID assigned to the new file.
     * <p>
     * On successful upload of the file version, a notification of type 'fileUploadComplete' will be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the uploaded file under the 'workspaceID' key, and the version number that was
     * uploaded under the 'versionNumber' key.
     * <p>
     * On failed upload of the file version, a notification of type 'fileUploadFailed' will be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the uploaded file under the 'workspaceID' key, the version number whose uploaded was
     * requested under the 'versionNumber' key, and the reason for the failure under the 'reason' key.
     * <p>
     * Periodically during the file version upload, a notification of type 'fileUploadProgress' may be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the uploaded file under the 'workspaceID' key, the version number of the file being
     * uploaded under the 'versionNumber' key, and the progress of the upload under the 'progress' key.  Upload progress
     * is reported in percent complete from 0 to 100 in whole numbers only.  The rate at which this notification is
     * emitted is implementation dependent, but shouldn't occur more than 100 times for any upload (only when the
     * percentage complete changes), and not more frequently than once every 5 seconds.  In any event, this notification
     * will be posted upon start of the file upload to allow listeners to learn the file ID and version ID assigned to
     * this file.
     *
     * @param fileToAdd
     * @param callback
     */
    @Override
    public void addFile(File fileToAdd, AddFileCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(fileToAdd.isFolder() || fileToAdd.getVersions() == null || fileToAdd.getVersions().isEmpty()) {
            throw new IllegalArgumentException("Cannot add File - malformed");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToAdd.getContainerID());
        // Instruct the Adapter to add the specified file metadata to the blockchain
        try {
            adapter.addFile(fileToAdd.getContainerID(), currentUserID, fileToAdd);
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new folder in the specified workspace with the specified parent. It is an error to specify a
     * non-existent workspace GUID, a non-existent parent GUID, or a null or blank folderName. If the parentID is null
     * or blank, the folder will be created at the root of the workspace.
     * <p>
     * This method will return the folder GUID of the newly created folder.
     * <p>
     * On successful creation of the folder, a notification of type 'folderAddComplete' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will
     * contain the folder ID under the key 'folderID'.
     * <p>
     * On failure to add a folder, a notification of type 'folderAddFailed' will be posted to the notification center.
     * The classifier of this notification will be the workspace ID.  The notification info will contain the the reason
     * for the failure under the 'reason' key.
     *
     * @param folderToAdd
     * @param callback
     */
    @Override
    public void addFolder(File folderToAdd, AddFolderCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(!folderToAdd.isFolder() || (folderToAdd.getVersions() != null && !folderToAdd.getVersions().isEmpty())) {
            throw new IllegalArgumentException("Cannot add File - malformed");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(folderToAdd.getContainerID());
        // Instruct the Adapter to add the specified file metadata to the blockchain
        try {
            adapter.addFile(folderToAdd.getContainerID(), currentUserID, folderToAdd);
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes a file from the specified workspace. It is an error to specify a non-existent workspace GUID, or a
     * non-existent fileGUID. Once a file is removed from a workspace, it is no longer displayed in the client
     * applications, and the library will no longer make its encrypted chunks available to other clients.
     * <p>
     * On successfully removing the file from the workspace, a notification of type 'fileRemoveComplete' will be posted
     * to the notification center. The classifier of this notification will be the workspace ID. The notification info
     * will contain the file ID of the workspace under the 'fileID' key.
     * <p>
     * On the failure to remove a file from the workspace, a notification of type 'fileRemoveFailed' will be posted to
     * the notification center.  The classifier of this notification will be the workspace ID.  The notification info
     * will contain the file ID under the 'fileID' key, and the reason for the failure under the 'reason' key.
     *
     * @param fileToRemove
     * @param callback
     */
    @Override
    public void removeFile(File fileToRemove, RemoveFileCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(fileToRemove.isFolder() || fileToRemove.getVersions() == null || fileToRemove.getVersions().isEmpty()) {
            throw new IllegalArgumentException("Cannot add File - malformed");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToRemove.getContainerID());
        // Instruct the RPC Adapter to remove the specified file from the blockchain
        try {
            adapter.removeFile(fileToRemove.getContainerID(), currentUserID, fileToRemove.getEntryID(), null);
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a new version to an existing file. It is an error to specify a non-existent workspace GUID, a non-existent
     * file GUID, or a non-exsitent file. The library will handle the process of chunking, encrypting, and sharing the
     * file content with other workspace members.
     * <p>
     * This method returns the version ID assigned to the new version.
     * <p>
     * On successful upload of the file version, a notification of type 'fileUploadComplete' will be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the uploaded file under the 'workspaceID' key, and the version number that was
     * uploaded under the 'versionNumber' key.
     * <p>
     * On failed upload of the file version, a notification of type 'fileUploadFailed' will be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the uploaded file under the 'workspaceID' key, the version number whose uploaded was
     * requested under the 'versionNumber' key, and the reason for the failure under the 'reason' key.
     * <p>
     * Periodically during the file version upload, a notification of type 'fileUploadProgress' may be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the uploaded file under the 'workspaceID' key, the version number of the file being
     * uploaded under the 'versionNumber' key, and the progress of the upload under the 'progress' key.  Upload progress
     * is reported in percent complete from 0 to 100 in whole numbers only.  The rate at which this notification is
     * emitted is implementation dependent, but shouldn't occur more than 100 times for any upload (only when the
     * percentage complete changes), and not more frequently than once every 5 seconds.  In any event, this notification
     * will be posted upon start of the file upload to allow listeners to learn the version ID assigned to this
     * version.
     *
     * @param fileToBeAdded
     * @param callback
     */
    @Override
    public void addFileVersion(File fileToBeAdded, AddFileVersionCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(fileToBeAdded.isFolder() || fileToBeAdded.getVersions() == null || fileToBeAdded.getVersions().isEmpty() || fileToBeAdded.getVersions().size() > 1 || fileToBeAdded.getVersions().get(0).getAncestorVersionIDs() == null || fileToBeAdded.getVersions().get(0).getAncestorVersionIDs().isEmpty()) {
            throw new IllegalArgumentException("Cannot add version - malformed");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToBeAdded.getContainerID());
        // Instruct the RPC Adapter to add a new version to the specified file with the given metadata.
        try {
            adapter.addFile(fileToBeAdded.getContainerID(), currentUserID, fileToBeAdded);
        } catch(BlockchainException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Removes a file version from a file in a workspace. It is an error to specify a non-existent workspace GUID, a
     * non-existent file GUID, or a non-existent file version GUID. Once removed from a workspace, the file version is
     * no longer displayed in the client application and the library will no longer make its encrypted chunks available
     * to other clients.
     * <p>
     * On successful removal of the file from the workspace, a notification of type 'fileRemoveComplete' will be posted
     * to the notification center. The classifier of this notification will be the workspace ID. The notification info
     * will contain the file ID of the removed version under the 'fileID' key, and the version number that was deleted
     * under the 'versionNumber' key.
     * <p>
     * On the failure to remove a file version from the workspace, a notification of type 'fileRemoveFailed' will be
     * posted to the notification center.  The classifier of this notification will be the workspace ID.  The
     * notification info will contain the file ID under the 'fileID' key, and the reason for the failure under the
     * 'reason' key.
     *
     * @param workspaceGUID
     * @param fileGUID
     * @param fileVersionGUID
     * @param callback
     */
    @Override
    public void removeFileVersion(long workspaceGUID, String fileGUID, String fileVersionGUID, RemoveFileVersionCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(workspaceGUID);
        // Instract the RPC Adapter to remove the specified file version from the blockchain.
        try {
            adapter.removeFile(workspaceGUID, currentUserID, fileGUID, fileVersionGUID);
        } catch(BlockchainException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Acknowledges receipt of a version of a file. It is an error to specify a non-existent workspace GUID, a
     * non-existent file GUID, or a non-existent file version GUID. File acknowledgements are recorded in the blockchain
     * and are visible to the clients.
     * <p>
     * Note: Downloading a file version will automatically acknowledge it.
     * <p>
     * On successful acknowledgement of the file, a notification of type 'fileAcknowledged' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will
     * contain the file ID of the acknowledged file under the 'fileID' key, and the version number that was acknowledged
     * under the 'versionNumber' key.
     *
     * @param workspaceGUID
     * @param fileGUID
     * @param fileVersionGUID
     * @param callback
     */
    @Override
    public void acknowledgeFileVersion(long workspaceGUID, String fileGUID, String fileVersionGUID, AcknowledgeFileCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(workspaceGUID);
        // Instruct the RPC Adapter to mark the specified file version as acknowledged.
        try {
            adapter.acknowledgeFile(workspaceGUID, currentUserID, fileGUID, fileVersionGUID);
        } catch (NoSuchFileException e) {
            e.printStackTrace();
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fileToTag
     * @param tagName
     * @param isPublic
     * @param callback
     */
    @Override
    public void addFileTag(File fileToTag, String tagName, boolean isPublic, AddFileTagCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(fileToTag.isFolder() || fileToTag.getVersions() == null || fileToTag.getVersions().isEmpty() || fileToTag.getVersions().size() > 1) {
            throw new IllegalArgumentException("Cannot add tag - malformed");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToTag.getContainerID());
        // Instruct the RPC Adapter to add the given tag to the specified file.
        try {
            adapter.addFileTag(fileToTag.getContainerID(), currentUserID, fileToTag.getEntryID(), fileToTag.getVersions().get(0).getVersionID(), tagName, isPublic);
        } catch (NoSuchFileException e) {
            e.printStackTrace();
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fileToUntag
     * @param tagName
     * @param isPublic
     * @param callback
     */
    @Override
    public void removeFileTag(File fileToUntag, String tagName, boolean isPublic, RemoveFileTagCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(fileToUntag.isFolder() || fileToUntag.getVersions() == null || fileToUntag.getVersions().isEmpty() || fileToUntag.getVersions().size() > 1) {
            throw new IllegalArgumentException("Cannot add tag - malformed");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToUntag.getContainerID());
        // Instruct the RPC Adapter to remove the given tag from the specified file.
        try {
            adapter.removeFileTag(fileToUntag.getContainerID(), currentUserID, fileToUntag.getEntryID(), fileToUntag.getVersions().get(0).getVersionID(), tagName, isPublic);
        } catch (NoSuchFileException e) {
            e.printStackTrace();
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiates a download of the specified file version. It is an error to specify a non-existent workspaceGUID, a
     * non-existent fileGUID, or a non-existent file version GUID. The library will acquire the file chunks necessary to
     * download this file end notify the caller when the are available.
     * <p>
     * Note: Downloading a file version will automatically acknowledge it.
     * <p>
     * This method will invoke the callback when the download finishes, either successfully or with an error.
     * <p>
     * On successful download of the file version, a notification of type 'fileDownloadComplete' will be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the downloaded file under the 'workspaceID' key, and the version number that was
     * downloaded under the 'versionNumber' key.
     * <p>
     * On failed download of the file version, a notification of type 'fileDownloadFailed' will be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the downloaded file under the 'workspaceID' key, the version number whose download
     * was requested under the 'versionNumber' key, and the reason for the failure under the 'reason' key.
     * <p>
     * Periodically during the file version download, a notification of type 'fileDownloadProgress' may be posted to the
     * notification center.  The classifier of this notification will be the fileID.  The notification information will
     * contain the workspace ID of the downloaded file under the 'workspaceID' key, the version number of the file being
     * downloaded under the 'versionNumber' key, and the progress of the download under the 'progress' key.  Download
     * progress is reported in percent complete from 0 to 100 in whole numbers only.  The rate at which this
     * notification is emitted is implementation dependent, but shouldn't occur more than 100 times for any download
     * (only when the percentage complete changes), and not more frequently than once every 5 seconds.
     *
     * @param workspaceGUID
     * @param fileGUID
     * @param fileVersionGUID
     * @param callback
     */
    @Override
    public void fetchFileVersion(String workspaceGUID, String fileGUID, String fileVersionGUID, FetchFileVersionCallback callback) {
        throw new NotImplementedException("The Blockchain code is not responsible for this action");
    }

    /**
     * Locks the specified file.  Locking a file prevents other users from deleting or uploading new version of the
     * file.  It is an error to specify a non-existent workspace GUID, or a non-existent file GUID.
     * <p>
     * Note: Not sure how we enforce this in a decentralized, blockchain based workspace.
     * <p>
     * On successful locking of the file, a notification of type 'lockFileComplete' will be posted to the notification
     * center.  The classifier of this notification will be the workspace ID.  The notification info will contain the
     * file ID under the 'fileID' key.
     * <p>
     * On failure to lock the file, a notification of type 'lockFileFailed' will be posted to the notification center.
     * The classifier of this notification will be the workspace ID.  The notification info will contain the file ID
     * under the 'fileID' key, and the reason for failure under the 'reason' key.
     *
     * @param fileToLock
     * @param callback
     */
    @Override
    public void lockFile(File fileToLock, LockFileCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(fileToLock.isFolder()) {
            throw new IllegalArgumentException("Cannot lock folders");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToLock.getContainerID());
        // Instruct the RPC Adapter to lock the specified file in the blockchain
        try {
            adapter.lockFile(fileToLock.getContainerID(), currentUserID, fileToLock.getEntryID());
        } catch(BlockchainException ex) {
            ex.printStackTrace();
        } catch (NoSuchFileException e) {
            e.printStackTrace();
        }
    }

    /**
     * Locks the specified file version.  Locking a file version prevents other users from deleting the version or the
     * file.  It is an error to specify a non-existent workspace GUID, or a non-existent file GUID, or a non-existant version GUID.
     * <p>
     * Note: Not sure how we enforce this in a decentralized, blockchain based workspace.
     * <p>
     * On successful locking of the file version, a notification of type 'lockFileVersionComplete' will be posted to the notification
     * center.  The classifier of this notification will be the workspace ID.  The notification info will contain the
     * file ID under the 'fileID' key and version ID under the 'versionID' key
     * <p>
     * On failure to lock the file version, a notification of type 'lockFileVersionFailed' will be posted to the notification center.
     * The classifier of this notification will be the workspace ID.  The notification info will contain the file ID
     * under the 'fileID' key, the version ID under the 'versionID' key, and the reason for failure under the 'reason' key.
     *
     * @param fileToLock
     * @param callback
     */
    @Override
    public void lockFileVersion(File fileToLock, LockFileCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(fileToLock.isFolder() || fileToLock.getVersions() == null || fileToLock.getVersions().isEmpty() || fileToLock.getVersions().size() > 1) {
            throw new IllegalArgumentException("Cannot add tag - malformed");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToLock.getContainerID());
        // Instruct the RPC Adapter to lock the specified file in the blockchain
        try {
            adapter.lockFileVersion(fileToLock.getContainerID(), currentUserID, fileToLock.getEntryID(), fileToLock.getVersions().get(0).getVersionID());
        } catch(BlockchainException ex) {
            ex.printStackTrace();
        } catch (NoSuchFileException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlocks the specified file.  Unlocking a file allows other users to once again delete or upload new version of
     * the file. It is an error to specify a non-existent workspace GUID, or a non-existent file GUID.
     * <p>
     * Note: Not sure how we enforce this in a decentralized, blockchain based workspace.
     * <p>
     * On successful unlocking of the file, a notification of type 'unlockFileComplete' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID.  The notification info will
     * contain the file ID under the 'fileID' key.
     * <p>
     * On failure to unlock the file, a notification of type 'unlockFileFailed' will be posted to the notification center.
     * The classifier of this notification will be the workspace ID.  The notification info will contain the file ID
     * under the 'fileID' key, and the reason for failure under the 'reason' key.
     *
     * @param fileToUnlock
     * @param callback
     */
    @Override
    public void unlockFile(File fileToUnlock, UnlockFileCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToUnlock.getContainerID());
        // Instruct the RPC Adapter to unlock the specified file in the blockchain
        try {
            adapter.unlockFile(fileToUnlock.getContainerID(), currentUserID, fileToUnlock.getEntryID());
        } catch(BlockchainException ex) {
            ex.printStackTrace();
        } catch (NoSuchFileException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlocks the specified file version.  Unlocking a file version allows other users to once again delete the version of
     * the file or the file itself. It is an error to specify a non-existent workspace GUID, or a non-existent file GUID, or a non-existent version GUID
     * <p>
     * Note: Not sure how we enforce this in a decentralized, blockchain based workspace.
     * <p>
     * On successful unlocking of the file version, a notification of type 'unlockFileVersionComplete' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID.  The notification info will
     * contain the file ID under the 'fileID' key and the version ID under the 'versionID' key.
     * <p>
     * On failure to unlock the file version, a notification of type 'unlockFileVersionFailed' will be posted to the notification center.
     * The classifier of this notification will be the workspace ID.  The notification info will contain the file ID
     * under the 'fileID' key, the version ID under the 'versionID' key, and the reason for failure under the 'reason' key.
     *
     * @param fileToUnlock
     * @param callback
     */
    @Override
    public void unlockFileVersion(File fileToUnlock, UnlockFileCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(fileToUnlock.isFolder() || fileToUnlock.getVersions() == null || fileToUnlock.getVersions().isEmpty() || fileToUnlock.getVersions().size() > 1) {
            throw new IllegalArgumentException("Cannot add tag - malformed");
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(fileToUnlock.getContainerID());
        // Instruct the RPC Adapter to unlock the specified file in the blockchain
        try {
            adapter.unlockFileVersion(fileToUnlock.getContainerID(), currentUserID, fileToUnlock.getEntryID(), fileToUnlock.getVersions().get(0).getVersionID());
        } catch(BlockchainException ex) {
            ex.printStackTrace();
        } catch (NoSuchFileException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the specified folder from workspace. All content contained within the folder is also removed. It is an
     * error to specify a non-existent workspace GUID, or a non-existent folder GUID.
     * <p>
     * On successful removal of the folder, a notification of type 'folderRemoveComplete' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will
     * contain the folder ID under the key 'folderID'.
     * <p>
     * On failure to remove the folder, a notification of type 'folderRemoveFailed' will be posted to the notification
     * center.  The classifier of this notification will be the workspace ID.  The notification info will contain the
     * folder ID of the folder under the 'folderID' key, and the reason for the failure under the 'reason' key.
     *
     * @param folderToRemove
     * @param callback
     */
    @Override
    public void removeFolder(File folderToRemove, RemoveFolderCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        if(!folderToRemove.isFolder()) {
            throw new IllegalArgumentException();
        }
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(folderToRemove.getContainerID());
        // Instruct the RPC Adapter to remove the specified folder in the blockchain.
        try {
            adapter.removeFile(folderToRemove.getContainerID(), currentUserID, folderToRemove.getEntryID(), null);
        } catch(BlockchainException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Adds a message to the workspace. It is an error to specify a non-existent workspaceGUID, or a null or empty
     * message.
     * <p>
     * The method will return the message GUID of the new message.
     * <p>
     * On successful addition of th message to the workspace, a notification of type 'messageAddComplete' will be posted
     * to the notification center. The classifier of this notification will be the workspace ID. The notification info
     * will contain the message ID under the key 'messageID'.
     * <p>
     * On failure to add the message to the workspace, a notification of type 'messageAddFailed' will be posted to the
     * notification center.  The classifier of this notification will be the workspace ID.  The notification info will
     * contain the reason for the failure under the 'reason' key.
     *
     * @param workspaceGUID
     * @param message
     * @param callback
     */
    @Override
    public void addMessage(long workspaceGUID, String message, String mimeType, AddMessageCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(workspaceGUID);
        // Instruct the RPC Adapter to add the specified message to the blockchain.
        try {
            adapter.addMessage(workspaceGUID, currentUserID, message, mimeType);
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }

    /**
     * Acknowledges receipt of a workspace message. It is an error to specify a non-existent workspace GUID, or a
     * non-existent message GUID.
     * <p>
     * On successful acknowledgement of the message, a notification of type 'messageAcknowledged' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will
     * contain the message ID under the key 'messageID'.
     *
     * @param messageToAcknowledge
     * @param callback
     */
    @Override
    public void acknowledgeMessage(Message messageToAcknowledge, AcknowledgeMessageCallback callback) throws NotLoggedInException, NoSuchWorkspaceException {
        // Get the RPC Adapter for the specified workspace
        EOSAdapter adapter = getEOSAdapter(messageToAcknowledge.getGuid());
        // Instruct the RPC Adapter to acknowledge the specified message in the blockchain.
        try {
            adapter.acknowledgeMessage(messageToAcknowledge.getGuid(), currentUserID, messageToAcknowledge.getEntityID());
        } catch (NoSuchMessageException e) {
            e.printStackTrace();
        } catch (BlockchainException e) {
            e.printStackTrace();
        }
    }
}
