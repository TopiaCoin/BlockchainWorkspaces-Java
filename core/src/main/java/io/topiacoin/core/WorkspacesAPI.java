package io.topiacoin.core;

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
import io.topiacoin.model.File;
import io.topiacoin.model.Message;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;

/**
 * The Workspace API defines all of the operations that can be performed on a workspace. This documentation defines the
 * behavior of the API calls and identifies the required parameters for the calls. Due to variations in the languages in
 * which this API will be implemented (e.g. Java vs C# vs Obj-C vs Javascript), specific platform implementations may
 * add additional parameters, or change the return types of the calls to accommodate the particular conventions of the
 * language. This might include passing in callback blocks or closures, the use of promises or futures, or the
 * definition and use of callback interfaces.
 */
public interface WorkspacesAPI {

    String MESSAGE_ACKNOWLEDGED_NOTIFICATION_TYPE = "messageAcknowledged";

    String MESSAGE_ADD_COMPLETE_NOTIFICATION_TYPE = "messageAddComplete";
    String MESSAGE_ADD_FAILED_NOTIFICATION_TYPE = "messageAddFailed";

    String FOLDER_ADD_COMPLETE_NOTIFICATION_TYPE = "folderAddComplete";
    String FOLDER_ADD_FAILED_NOTIFICATION_TYPE = "folderAddFailed";

    String FILE_REMOVE_COMPLETE_NOTIFICATION_TYPE = "fileRemoveComplete";
    String FILE_REMOVE_FAILED_NOTIFICATION_TYPE = "fileRemoveFailed";

    String FILE_ACKNOWLEDGED_NOTIFICATION_TYPE = "fileAcknowledged";

    String USER_REMOVE_COMPLETE_NOTIFICATION_TYPE = "userRemoveComplete";
    String USER_REMOVE_FAILED_NOTIFICATION_TYPE = "userRemoveFailed";

    String USER_INVITE_COMPLETE_NOTIFICATION_TYPE = "userInviteComplete";
    String USER_INVITE_FAILED_NOTIFICATION_TYPE = "userInviteFailed";

    String WORKSPACE_REMOVE_COMPLETE_NOTIFICATION_TYPE = "workspaceRemoveComplete";
    String WORKSPACE_REMOVE_FAILED_NOTIFICATION_TYPE = "workspaceRemoveFailed";

    String WORKSPACE_DECLINE_COMPLETE_NOTIFICATION_TYPE = "workspaceDeclineComplete";
    String WORKSPACE_DECLINE_FAILED_NOTIFICATION_TYPE = "workspaceDeclineFailed";

    String WORKSPACE_ACCEPT_COMPLETE_NOTIFICATION_TYPE = "workspaceAcceptComplete";
    String WORKSPACE_ACCEPT_FAILED_NOTIFICATION_TYPE = "workspaceAcceptFailed";

    String WORKSPACE_UPDATE_COMPLETE_NOTIFICATION_TYPE = "workspaceUpdateComplete";
    String WORKSPACE_UPDATE_FAILED_NOTIFICATION_TYPE = "workspaceUpdateFailed";

    String WORKSPACE_CREATION_COMPLETE_NOTIFICATION_TYPE = "workspaceCreationComplete";
    String WORKSPACE_CREATION_FAILED_NOTIFICATION_TYPE = "workspaceCreationFailed";

    String WORKSPACE_CONNECTION_COMPLETE_NOTIFICATION_TYPE = "workspaceConnectionComplete";
    String WORKSPACE_CONNECTION_FAILED_NOTIFICATION_TYPE = "workspaceConnectionFailed";

    String FILE_DOWNLOAD_COMPLETE_NOTIFICATION_TYPE = "fileDownloadComplete";
    String FILE_DOWNLOAD_FAILED_NOTIFICATION_TYPE = "fileDownloadFailed";
    String FILE_DOWNLOAD_PROGRESS_NOTIFICATION_TYPE = "fileDownloadProgress";

    String FILE_UPLOAD_COMPLETE_NOTIFICATION_TYPE = "fileUploadComplete";
    String FILE_UPLOAD_FAILED_NOTIFICATION_TYPE = "fileUploadFailed";
    String FILE_UPLOAD_PROGRESS_NOTIFICATION_TYPE = "fileUploadProgress";

    String FILE_VERSION_SAVE_COMPLETE_NOTIFICATION_TYPE = "fileVersionSaveComplete";
    String FILE_VERSION_SAVE_FAILED_NOTIFICATION_TYPE = "fileVersionSaveFailed";



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
     */
    void connectWorkspace(String workspaceID, ConnectWorkspaceCallback callback) throws NotLoggedInException, NoSuchWorkspaceException;

    /**
     * Requestes that the Blockchain Workspace API check all tracked workspaces for updates. This will cause the system
     * to check all of the tracked blockchains for changes. This functionality is periodically invoked by the API to
     * insure that the system is regularly updated with any new activity. This method is provided so that the client
     * application can trigger on demand updates, such as when requested by the user. This method does not guarentee
     * that an update will occur. In certain cases, the system may choose to ignore the request for updates, such as if
     * an update was recently performed (e.g. within the last 10 seconds).
     */
    void checkForUpdates();

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
     */
    void createWorkspace(String workspaceName, String workspaceDescription, CreateWorkspaceCallback callback);

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
     */
    void updateWorkspaceDescription(Workspace workspaceToUpdate, UpdateWorkspaceDescriptionCallback callback);

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
     */
    void inviteUser(Workspace workspace, String userID, String inviteMessage, InviteUserCallback callback);

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
     */
    void acceptInvitation(Workspace workspace, AcceptInvitationCallback callback);

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
     */
    void declineInvitation(Workspace workspace, DeclineInvitationCallback callback);

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
    void deliverWorkspaceKey(String workspaceGUID, String userID, String encryptedWorkspaceKey);

    /**
     * Note: Not sure this method is required. I don't know what information we expect it to return.
     */
    //BlockchainInfo getWorkspaceBlockchainInfo(String workspaceGUID);

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
     */
    void leaveWorkspace(Workspace workspace, LeaveWorkspaceCallback callback);

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
     */
    void removeUserFromWorkspace(Workspace workspace, String memberID, RemoveUserCallback callback);

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
     */
    void addFile(File fileToAdd, AddFileCallback callback);

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
     */
    void removeFile(File fileToRemove, RemoveFileCallback callback);

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
     */
    void addFileVersion(File fileToBeAdded, AddFileVersionCallback callback);

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
     */
    void removeFileVersion(String workspaceGUID, String fileGUID, String fileVersionGUID, RemoveFileVersionCallback callback);

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
     */
    void acknowledgeFileVersion(String workspaceGUID, String fileGUID, String fileVersionGUID, AcknowledgeFileCallback callback);

    /**
     *
     */
    void addFileTag(File fileToTag, String tagName, boolean isPrivate, AddFileTagCallback callback);

    /**
     *
     */
    void removeFileTag(File fileToUntag, String tagName, boolean isPrivate, RemoveFileTagCallback callback);

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
     */
    void fetchFileVersion(String workspaceGUID, String fileGUID, String fileVersionGUID, FetchFileVersionCallback callback);

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
     */
    void lockFile(File fileToLock, LockFileCallback callback);

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
     * On failure to lock the file, a notification of type 'unlockFileFailed' will be posted to the notification center.
     * The classifier of this notification will be the workspace ID.  The notification info will contain the file ID
     * under the 'fileID' key, and the reason for failure under the 'reason' key.
     */
    void unlockFile(File fileToUnlock, UnlockFileCallback callback);

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
     */
    void addFolder(File folderToAdd, AddFolderCallback callback);

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
     */
    void removeFolder(File folderToRemove, RemoveFolderCallback callback);


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
     */
    void addMessage(String workspaceGUID, String message, AddMessageCallback callback);


    /**
     * Acknowledges receipt of a workspace message. It is an error to specify a non-existent workspace GUID, or a
     * non-existent message GUID.
     * <p>
     * On successful acknowledgement of the message, a notification of type 'messageAcknowledged' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will
     * contain the message ID under the key 'messageID'.
     */
    void acknowledgeMessage(Message messageToAcknowledge, AcknowledgeMessageCallback callback);

}
