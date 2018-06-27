package io.topiacoin.sdk;

import io.topiacoin.chunks.ChunkManager;
import io.topiacoin.chunks.exceptions.DuplicateChunkException;
import io.topiacoin.chunks.exceptions.InsufficientSpaceException;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunksFetchHandler;
import io.topiacoin.core.Configuration;
import io.topiacoin.core.EventsAPI;
import io.topiacoin.core.UsersAPI;
import io.topiacoin.core.WorkspacesAPI;
import io.topiacoin.core.callbacks.AcceptInvitationCallback;
import io.topiacoin.core.callbacks.AcknowledgeFileCallback;
import io.topiacoin.core.callbacks.AcknowledgeMessageCallback;
import io.topiacoin.core.callbacks.AddFileCallback;
import io.topiacoin.core.callbacks.AddFileTagCallback;
import io.topiacoin.core.callbacks.AddFileVersionCallback;
import io.topiacoin.core.callbacks.AddFolderCallback;
import io.topiacoin.core.callbacks.AddMessageCallback;
import io.topiacoin.core.callbacks.CreateWorkspaceCallback;
import io.topiacoin.core.callbacks.DeclineInvitationCallback;
import io.topiacoin.core.callbacks.DownloadFileVersionCallback;
import io.topiacoin.core.callbacks.InviteUserCallback;
import io.topiacoin.core.callbacks.LeaveWorkspaceCallback;
import io.topiacoin.core.callbacks.LockFileCallback;
import io.topiacoin.core.callbacks.RemoveFileCallback;
import io.topiacoin.core.callbacks.RemoveFileTagCallback;
import io.topiacoin.core.callbacks.RemoveFileVersionCallback;
import io.topiacoin.core.callbacks.RemoveFolderCallback;
import io.topiacoin.core.callbacks.RemoveMemberCallback;
import io.topiacoin.core.callbacks.SaveFileVersionCallback;
import io.topiacoin.core.callbacks.UnlockFileCallback;
import io.topiacoin.core.callbacks.UpdateWorkspaceDescriptionCallback;
import io.topiacoin.core.exceptions.NotLoggedInException;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.crypto.HashUtils;
import io.topiacoin.dht.SDFSDHTAccessor;
import io.topiacoin.model.CurrentUser;
import io.topiacoin.model.DHTWorkspaceEntry;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.File;
import io.topiacoin.model.FileChunk;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.Member;
import io.topiacoin.model.User;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.FileAlreadyExistsException;
import io.topiacoin.model.exceptions.FileChunkAlreadyExistsException;
import io.topiacoin.model.exceptions.FileVersionAlreadyExistsException;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchFileVersionException;
import io.topiacoin.model.exceptions.NoSuchMemberException;
import io.topiacoin.model.exceptions.NoSuchUserException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import io.topiacoin.workspace.blockchain.BlockchainUsersAPI;
import io.topiacoin.sdk.impl.DHTEventsAPI;
import io.topiacoin.workspace.blockchain.BlockchainWorkspacesAPI;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class SDFS {

    private final Log _log = LogFactory.getLog(this.getClass());

    private Configuration _configuration;
    private WorkspacesAPI _workspaceAPI;
    private UsersAPI _userAPI;
    private EventsAPI _eventAPI;

    private DataModel _dataModel;
    private ChunkManager _chunkManager;

    private ExecutorService _taskExecutor;

    SDFS() {
        _configuration = null;
        _taskExecutor = Executors.newSingleThreadExecutor();
    }

    public SDFS(KeyPair userKeyPair, Configuration configuration) {
        this() ;

        _configuration = configuration;
        _workspaceAPI = new BlockchainWorkspacesAPI(configuration);
        _userAPI = new BlockchainUsersAPI(configuration);
        _eventAPI = new DHTEventsAPI();

    }


    // -------- Event API --------

    public void addUpdateListener() throws NoSuchUserException {
        _eventAPI.startEventFetching(_configuration, _dataModel);
    }

    public void removeUpdateListener() {
        _eventAPI.stopEventFetching();
    }

    /**
     * Requests that the Blockchain Workspace API check all tracked workspaces for updates. This will cause the system
     * to check all of the tracked blockchains for changes. This functionality is periodically invoked by the API to
     * insure that the system is regularly updated with any new activity. This method is provided so that the client
     * application can trigger on demand updates, such as when requested by the user. This method does not guarantee
     * that an update will occur. In certain cases, the system may choose to ignore the request for updates, such as if
     * an update was recently performed (e.g. within the last 10 seconds).
     */
    public void checkForUpdates() {

    }


    // -------- User API --------

    /**
     * Returns the user with the specified identifier, if available. Depending on the configuration of the SDK, the
     * userIdentifier may be the userID, email address, or blockchain wallet address. If no user can be found with this
     * identifier, null is returned.
     *
     * @param userIdentifier
     *
     * @return
     */
    public User getUser(String userIdentifier) {
        return null;
    }

    public void updateUser(String userIdentifier, User user) {

    }

    // -------- Workspace API --------

    public List<Workspace> getWorkspaces() {
        return null;
    }

    public Workspace getWorkspace(String workspaceID) {
        return null;
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
     */
    public void createWorkspace(String workspaceName, String workspaceDescription, CreateWorkspaceCallback callback) throws NotLoggedInException {
        long workspaceID = 0L; //Generate this somehow
        SDFSDHTAccessor accessor = SDFSDHTAccessor.getInstance(_configuration, _dataModel);
        DHTWorkspaceEntry dhtEntry = accessor.addNewWorkspaceToDHT(workspaceID);
        //Not sure what to do with this Entry. Should we store it in the model?
        //Also, this is gonna fire an event. Is that ok?
        throw new NotImplementedException();
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
     * @param workspaceGUID
     * @param workspaceDescription
     */
    public void updateWorkspaceDescription(String workspaceGUID, String workspaceDescription, UpdateWorkspaceDescriptionCallback callback) {

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
     * @param workspaceGUID
     * @param userID
     * @param inviteMessage
     */
    public void inviteUser(String workspaceGUID, String userID, String inviteMessage, InviteUserCallback callback) throws IOException, NoSuchUserException {
        SDFSDHTAccessor accessor = SDFSDHTAccessor.getInstance(_configuration, _dataModel);
        DHTWorkspaceEntry dhtEntry = null; //Where does this come from?
        User user = _dataModel.getUserByID(userID);
        accessor.addInvitation(dhtEntry, user);
        //Also, this is gonna fire an event. Is that ok?
        throw new NotImplementedException();
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
     * @param workspaceGUID
     */
    public void acceptInvitation(String workspaceGUID, AcceptInvitationCallback callback) {

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
     * @param workspaceGUID
     */
    public void declineInvitation(String workspaceGUID, DeclineInvitationCallback callback) throws NotLoggedInException {
        SDFSDHTAccessor accessor = SDFSDHTAccessor.getInstance(_configuration, _dataModel);
        DHTWorkspaceEntry dhtEntry = null; //Where does this come from?
        accessor.leaveWorkspace(dhtEntry);
        //Also, this is gonna fire an event. Is that ok?
        throw new NotImplementedException();
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
    public void deliverWorkspaceKey(String workspaceGUID, String userID, String encryptedWorkspaceKey) {

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
     * @param workspaceGUID
     */
    public void leaveWorkspace(String workspaceGUID, LeaveWorkspaceCallback callback) throws NotLoggedInException {
        SDFSDHTAccessor accessor = SDFSDHTAccessor.getInstance(_configuration, _dataModel);
        DHTWorkspaceEntry dhtEntry = null; //Where does this come from?
        accessor.leaveWorkspace(dhtEntry);
        //Also, this is gonna fire an event. Is that ok?
        throw new NotImplementedException();
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
     * @param worksapceGUID
     * @param memberID
     */
    public void removeUserFromWorkspace(long worksapceGUID, String memberID, RemoveMemberCallback callback) throws NoSuchUserException, NoSuchWorkspaceException, NoSuchMemberException {
        SDFSDHTAccessor accessor = SDFSDHTAccessor.getInstance(_configuration, _dataModel);
        DHTWorkspaceEntry dhtEntry = null; //Where does this come from?
        Member member = _dataModel.getMemberInWorkspace(worksapceGUID, memberID);
        accessor.removeMemberFromWorkspace(dhtEntry, member);
        //Also, this is gonna fire an event. Is that ok?
        throw new NotImplementedException();
    }


    public List<File> getFilesInWorkspace(String workspaceGUID) {
        return null;
    }

    public File getFile(String fileID) {
        return null;
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
     * @param workspaceGUID
     * @param folderGUID
     * @param fileToBeAdded
     */
    public void addFile(final long workspaceGUID,
                        final String folderGUID,
                        final java.io.File fileToBeAdded,
                        final AddFileCallback callback) {
        try {
            CurrentUser currentUser = _dataModel.getCurrentUser();

            _taskExecutor.submit(() -> {
                _log.info ( "Processing " + fileToBeAdded.getName()) ;
                try {
                    String fileGUID = UUID.randomUUID().toString();
                    String fileVersionGUID = UUID.randomUUID().toString();
                    List<FileChunk> fileChunks = new ArrayList<>();

                    // Create File object with the basic metadata.
                    File newFile = new File();
                    newFile.setEntryID(fileGUID);
                    newFile.setName(fileToBeAdded.getName());
                    newFile.setParentID(folderGUID);
                    newFile.setContainerID(workspaceGUID);
                    newFile.setFolder(false);
                    newFile.setMimeType(mimeTypeForFile(fileToBeAdded));

                    // Create the version object for the new file
                    FileVersion fileVersion = new FileVersion();
                    fileVersion.setEntryID(fileGUID);
                    fileVersion.setVersionID(fileVersionGUID);
                    fileVersion.setUploadDate(System.currentTimeMillis());
                    fileVersion.setDate(fileToBeAdded.lastModified());
                    fileVersion.setSize(fileToBeAdded.length());

                    // Chunk and Encrypt the Data, collecting all the necessary metadata along the way.
                    int chunkSize = _configuration.getConfigurationOption("chunk.size", 524288);
                    long chunkIndex = 0;
                    long bytesRemaining = fileToBeAdded.length();
                    SecretKey chunkKey;
                    IvParameterSpec iv;
                    byte[] clearChunk = new byte[chunkSize];
                    byte[] cipherChunk = null;
                    byte[] fileHash = null;
                    MessageDigest sha256File = MessageDigest.getInstance("SHA-256");
                    FileInputStream fileInputStream = new FileInputStream(fileToBeAdded);
                    int bytesRead = 0;
                    try {
                        while (bytesRemaining > 0 && bytesRead >= 0) {
                            // Generate a new Chunk ID
                            String chunkID = HashUtils.sha256String(UUID.randomUUID().toString() + System.currentTimeMillis());

                            chunkKey = CryptoUtils.generateAESKey();
                            iv = CryptoUtils.generateIV(chunkKey.getAlgorithm());

                            // Read in on chunk's worth of data from the source file
                            bytesRead = fileInputStream.read(clearChunk);
                            if (bytesRead > 0) {
                                FileChunk fileChunk;

                                // Update the file Hash digest with the current chunk.
                                sha256File.update(clearChunk, 0, bytesRead);

                                int clearSize = bytesRead;
                                String clearHashStr = HashUtils.sha256String(clearChunk);

                                // Check to see if a chunk with this clear hash already exists.
                                FileChunk existingFileChunk = _dataModel.getFileChunkWithClearHash(clearHashStr);
                                if ( existingFileChunk != null ) {
                                    // There is an existing chunk we can reuse.  Copy it, then update the
                                    // chunk index of the copy to match where the chunk fits into this file.
                                    fileChunk = new FileChunk(existingFileChunk);
                                    fileChunk.setIndex(chunkIndex);
                                } else {
                                    // There is not an existing chunk.  Compress and encrypt the chunk data,
                                    // then create a new FileChunk object to contain the info about this chunk.

                                    // Compress the clearChunk
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    GZIPOutputStream gzos = new GZIPOutputStream(baos);
                                    gzos.write(clearChunk, 0, bytesRead);
                                    gzos.close();
                                    byte[] compressedChunk = baos.toByteArray();

                                    // Encrypt the chunk
                                    cipherChunk = CryptoUtils.encryptWithSecretKey(compressedChunk, chunkKey, iv);
                                    bytesRemaining -= bytesRead;

                                    // Calculate sizes and hashes of the clear and cipher chunks.
                                    int cipherSize = cipherChunk.length;
                                    String cipherHashStr = HashUtils.sha256String(cipherChunk);

                                    // Create the File Chunk Model object
                                    fileChunk = new FileChunk();
                                    fileChunk.setIndex(chunkIndex);
                                    fileChunk.setChunkID(chunkID);
                                    fileChunk.setClearTextSize(clearSize);
                                    fileChunk.setCipherTextSize(cipherSize);
                                    fileChunk.setClearTextHash(clearHashStr);
                                    fileChunk.setCipherTextHash(cipherHashStr);
                                    fileChunk.setCompressionAlgorithm("GZIP");
                                    fileChunk.setChunkKey(chunkKey);
                                    fileChunk.setInitializationVector(iv.getIV());
                                }

                                // Save the encrypted chunk in the chunk manager
                                _chunkManager.addChunk(chunkID, cipherChunk);

                                // Add the File Chunk to the Collection
                                fileChunks.add(fileChunk);

                                // Increment the Chunk Index Counter
                                chunkIndex++;
                            }

                        }

                        fileHash = sha256File.digest();
                    } finally {
                        fileInputStream.close();
                    }

                    // Set the calculated Information
                    String fileHashStr = sha256File.getAlgorithm() + ":" + Base64.encodeBase64String(fileHash); // TODO - Try to figure out how to do this using Hash Utils
                    fileVersion.setFileHash(fileHashStr);
                    fileVersion.setOwnerID(currentUser.getUserID());
                    fileVersion.setFileChunks(fileChunks);

                    newFile.setVersions(Collections.singletonList(fileVersion));

                    _log.info ( "Adding " + fileToBeAdded.getName() + " to Workspace") ;
                    _workspaceAPI.addFile(newFile, new AddFileCallback() {
                        @Override
                        public void didAddFile(java.io.File addFile) {
                            _log.info ( "Added " + fileToBeAdded.getName() + " to Workspace") ;
                            if ( callback != null )
                                callback.didAddFile(fileToBeAdded);
                        }

                        @Override
                        public void failedToAddFile(java.io.File file) {
                            _log.warn ( "Failed to Add " + fileToBeAdded.getName() + " to Workspace") ;
                            if ( callback != null )
                                callback.failedToAddFile(fileToBeAdded);
                        }
                    });

                    // Save all of the objects into the data model
                    _dataModel.addFileToWorkspace(workspaceGUID, newFile);
                    _dataModel.addFileVersion(fileGUID, fileVersion);
                    for (FileChunk fileChunk : fileChunks) {
                        _dataModel.addChunkForFile(fileGUID, fileVersionGUID, fileChunk);
                    }
                } catch (CryptographicException e) {
                    e.printStackTrace();
                    _log.warn ( "Encryption Failure Preparing File to be Added to Workspace",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (InsufficientSpaceException e) {
                    e.printStackTrace();
                    _log.warn ( "Insufficient Space Available to Add File to Workspace",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (IOException e) {
                    e.printStackTrace();
                    _log.warn ( "IOException Adding File to Workspace",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (DuplicateChunkException e) {
                    e.printStackTrace();
                    _log.warn ( "Attempt to add Duplicate chunk to data model", e) ;
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    _log.warn ( "Failed to find necessary Java Cryptographic Algorithm",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (NoSuchWorkspaceException e) {
                    e.printStackTrace();
                    _log.warn ( "Target workspace not found",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (FileAlreadyExistsException e) {
                    e.printStackTrace();
                    _log.warn ( "The specified file already exists in the workspace",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (FileVersionAlreadyExistsException e) {
                    e.printStackTrace();
                    _log.warn ( "Failure Adding File to Workspace",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (NoSuchFileVersionException e) {
                    e.printStackTrace();
                    _log.warn ( "Failure Adding File to Workspace",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (NoSuchFileException e) {
                    e.printStackTrace();
                    _log.warn ( "Failure Adding File to Workspace",e);
                    if ( callback != null )
                        callback.failedToAddFile(fileToBeAdded);
                } catch (FileChunkAlreadyExistsException e) {
                    e.printStackTrace();
                    _log.warn("Failure Adding File to Workspace", e);
                    if (callback != null)
                        callback.failedToAddFile(fileToBeAdded);
                } catch ( Exception e) {
                    e.printStackTrace();
                    // TODO - Remove this catch all
                } finally {

                }
            });

        } catch (NoSuchUserException e) {
            e.printStackTrace();
            _log.info ( "User Not Logged In",e);
            throw new RuntimeException("No current user logged in", e);
        } catch ( Exception e) {
            e.printStackTrace();
            // TODO - Remove this catch all
        } finally {

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
     * @param workspaceGUID
     * @param fileGUID
     */
    public void removeFile(String workspaceGUID, String fileGUID, RemoveFileCallback callback) {

    }

    public List<String> getAvailableFileVersions(String fileID) {
        return null;
    }

    public FileVersion getFileVersion(String fileGUID, String fileVersionGUID) {
        return null;
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
     * @param workspaceGUID
     * @param fileGUID
     * @param fileToBeAdded
     */
    public void addFileVersion(String workspaceGUID, String fileGUID, java.io.File fileToBeAdded, AddFileVersionCallback callback) {

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
     */
    public void removeFileVersion(String workspaceGUID, String fileGUID, String fileVersionGUID, RemoveFileVersionCallback callback) {

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
     */
    public void acknowledgeFileVersion(String workspaceGUID, String fileGUID, String fileVersionGUID, AcknowledgeFileCallback callback) {

    }

    /**
     * @param workspaceGUID
     * @param fileGUID
     * @param tagName
     * @param isPrivate
     */
    public void addFileTag(String workspaceGUID, String fileGUID, String tagName, boolean isPrivate, AddFileTagCallback callback) {

    }

    /**
     * @param workspaceGUID
     * @param fileGUID
     * @param tagName
     */
    public void removeFileTag(String workspaceGUID, String fileGUID, String tagName, RemoveFileTagCallback callback) {

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
    public void downloadFileVersion(final long workspaceGUID,
                                    final String fileGUID,
                                    final String fileVersionGUID,
                                    final DownloadFileVersionCallback callback)
            throws NoSuchWorkspaceException, NoSuchFileException, NoSuchFileVersionException {

        try {
            // Get userIDs of all workspace members
            // - throws NoSuchWorkspaceException if the requested workspace does not exist.
            List<Member> members = _dataModel.getMembersInWorkspace(workspaceGUID);

            // Verify that the file version requested actually exists and is in the specified workspace.
            File file = _dataModel.getFile(fileGUID);
            if (file.getContainerID() != workspaceGUID) {
                throw new NoSuchFileException("The requested file does not exist in the specified workspace");
            }
            FileVersion fileVersion = _dataModel.getFileVersion(fileGUID, fileVersionGUID);

            // Get chunkIDs for the specified file.
            List<FileChunk> fileChunks = _dataModel.getChunksForFileVersion(fileGUID, fileVersionGUID);
            List<String> chunkIDs = fileChunks.stream().map(FileChunk::getChunkID).collect(Collectors.toList());

            // --Fetch the Member Nodes for each of the userIDs-- Handled by Chunk Manager currently.
            // Ask the chunk manager to fetch all the chunks
            _chunkManager.fetchChunks(chunkIDs, workspaceGUID, new ChunksFetchHandler() {
                @Override
                public void finishedFetchingChunks(List<String> successfulChunks, List<String> unsuccessfulChunks, Object state) {
                    if ( callback != null )
                        callback.didDownloadFileVersion(fileGUID, fileVersionGUID);
                }

                @Override
                public void errorFetchingChunks(String message, Exception cause, Object state) {
                    if ( callback != null )
                        callback.failedToDownloadFileVersion(fileGUID, fileVersionGUID, message);
                }
            }, null);
        } catch (NoSuchWorkspaceException e) {
            _log.info("Attempt to download a file from a non-existent workspace.", e);
            throw e;
        } catch (NoSuchFileException e) {
            _log.info("Attempt to download a file version from a non-existent file.", e);
            throw e;
        } catch (NoSuchFileVersionException e) {
            _log.info("Attempt to download a non-existent file version.", e);
            throw e;
        }
    }

    /**
     * Decrypts, reconstructs, and saves the specified file into the target directory. This method assumes that the file
     * has been previously downloaded and its chunks are available on the device. It is an error to specify a
     * non-existent workspace GUID, a non-existent file GUID, or a non-existent file version GUID. It is also an error
     * to attempt to save a file whose chunks have not been downloaded.
     * <p>
     * This method will invoke the callback when the file save finishes, either successfully or with an error.
     * <p>
     * On completion of file save, a notification of type 'fileVersionSaveComplete' will be posted to the notification
     * center.  The classifier of this notification will be the fileID.  The notification information will contain the
     * workspace ID of the saved file under the 'workspaceID' key, the version number under the 'versionNumber' key, and
     * the full path to the location of the decrypted file under the 'filePath' key.
     * <p>
     * On failed save of the file, a notification of type 'fileVersionSaveFailed' will be posted to the the notification
     * center.  The classifier of this notification will be the fileID.  The notification information will contain the
     * workspace ID of the saved file under the 'workspaceID' key, the version number under the 'versionNumber' key, and
     * the reason for the failure under the 'reason' key.
     *
     * @param workspaceGUID   The GUID of the workspace containing the file being saved.
     * @param fileGUID        The GUID of the file whose version is being saved.
     * @param fileVersionGUID The GUID of the version of the file being saved.
     * @param targetDirectory The directory into which the file is to be saved.
     *
     * @throws NoSuchFileException        If the specified fileGUID does not exist in the specified workspace.
     * @throws NoSuchFileVersionException If the specified file does not have a version with the specified
     *                                    fileVersionGUID.
     * @throws IOException                If there is an error accessing the target location, or decrypting the file
     *                                    version's chunk data.
     */
    public void saveFileVersion(final long workspaceGUID,
                                final String fileGUID,
                                final String fileVersionGUID,
                                final String targetDirectory,
                                final SaveFileVersionCallback callback)
            throws NoSuchFileException, NoSuchFileVersionException, IOException {

        try {
            // Verify that the file version requested actually exists and is in the specified workspace.
            File file = _dataModel.getFile(fileGUID);
            if (file.getContainerID() != workspaceGUID) {
                throw new NoSuchFileException("The requested file does not exist in the specified workspace");
            }

            // Get chunkIDs for the specified file.
            List<FileChunk> fileChunks = _dataModel.getChunksForFileVersion(fileGUID, fileVersionGUID);

            // Verify that the chunk Manager has all of the necessary chunks
            for (FileChunk fileChunk : fileChunks) {
                if (!_chunkManager.hasChunk(fileChunk.getChunkID())) {
                    throw new NoSuchChunkException("Missing chunk required to save file");
                }
            }

            // Create the File object for the decrypted file,
            // de-duplicating the name if a file with the name already exists.
            java.io.File candidateFile = new java.io.File(targetDirectory, file.getName());
            int dedupIndex = 0;
            while (candidateFile.exists()) {
                candidateFile = new java.io.File(targetDirectory, file.getName() + "-" + ++dedupIndex);
            }

            // Capture the candidate file object in a final variable so we can reference it inside the Runnable below.
            final java.io.File targetFile = candidateFile;

            // Submit a Runnable (via Java 8 lambda) to the executor to save the file off in the background.
            _taskExecutor.submit(() -> {
                try {
                    decryptAndSaveFileVersion(fileChunks, targetFile);
                    if (callback != null) {
                        callback.didSaveFile(fileGUID, fileVersionGUID, targetFile);
                    }
                } catch (CryptographicException e) {
                    _log.info("Failed to decrypt required file chunk.", e);
                    if ( callback != null )
                        callback.failedToSaveFile(fileGUID, fileVersionGUID, "Error decrypting the file chunks");
                } catch (IOException e) {
                    _log.info("Exception while saving the file.", e);
                    if ( callback != null )
                        callback.failedToSaveFile(fileGUID, fileVersionGUID, "Error saving the file");
                } catch (NoSuchChunkException e) {
                    _log.info("Exception while saving the file.", e);
                    if ( callback != null )
                        callback.failedToSaveFile(fileGUID, fileVersionGUID, "Unable to find all of the required file chunks");
                }
            });
        } catch (NoSuchFileException e) {
            _log.info("Attempt to download a file version from a non-existent file.", e);
            throw e;
        } catch (NoSuchFileVersionException e) {
            _log.info("Attempt to download a non-existent file version.", e);
            throw e;
        } catch (NoSuchChunkException e) {
            _log.info("Failed to retrieve required file chunk.", e);
            throw new IOException("Unable to retrieve required file chunk", e);
        }
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
     * @param workspaceGUID
     * @param fileGUID
     */
    public void lockFile(String workspaceGUID, String fileGUID, LockFileCallback callback) {

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
     * On failure to lock the file, a notification of type 'unlockFileFailed' will be posted to the notification center.
     * The classifier of this notification will be the workspace ID.  The notification info will contain the file ID
     * under the 'fileID' key, and the reason for failure under the 'reason' key.
     *
     * @param workspaceGUID
     * @param fileGUID
     */
    public void unlockFile(String workspaceGUID, String fileGUID, UnlockFileCallback callback) {

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
     * @param workspaceGUID
     * @param parentGUID
     * @param folderName
     */
    public void addFolder(String workspaceGUID, String parentGUID, String folderName, AddFolderCallback callback) {

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
     * @param workspaceGUID
     * @param folderGUID
     */
    public void removeFolder(String workspaceGUID, String folderGUID, RemoveFolderCallback callback) {

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
     */
    public void addMessage(String workspaceGUID, String message, AddMessageCallback callback) {

    }

    /**
     * Acknowledges receipt of a workspace message. It is an error to specify a non-existent workspace GUID, or a
     * non-existent message GUID.
     * <p>
     * On successful acknowledgement of the message, a notification of type 'messageAcknowledged' will be posted to the
     * notification center. The classifier of this notification will be the workspace ID. The notification info will
     * contain the message ID under the key 'messageID'.
     *
     * @param workspaceGUID
     * @param messageGUID
     */
    public void acknowledgeMessage(String workspaceGUID, String messageGUID, AcknowledgeMessageCallback callback) {

    }

    // -------- Private Methods --------

    /**
     * <i>Note: This method is invoked asynchronously.</i>
     */
    private void decryptAndSaveFileVersion(List<FileChunk> fileChunks, java.io.File targetFile) throws NoSuchChunkException, CryptographicException, IOException {
        FileOutputStream targetStream = new FileOutputStream(targetFile);

        try {
            // Iterate over the chunks copying the decrypted chunk data to the output file.
            // -- This code assumes that the file chunks are returned in the proper order from the data model.
            for (FileChunk fileChunk : fileChunks) {
                SecretKey chunkKey = fileChunk.getChunkKey();
                IvParameterSpec iv = new IvParameterSpec(fileChunk.getInitializationVector());

                InputStream chunkStream = _chunkManager.getChunkDataAsStream(fileChunk.getChunkID());
                try {
                    CryptoUtils.decryptWithSecretKey(chunkStream, targetStream, chunkKey, iv);
                } finally {
                    chunkStream.close();
                }
            }
        } finally {
            targetStream.close();
        }
    }

    private String mimeTypeForFile(java.io.File fileToBeAdded) {
        // TODO - Make this method actually do something useful
        return "application/octet-stream";
    }


    // -------- Accessor Methods --------


    public void setWorkspaceAPI(WorkspacesAPI workspaceAPI) {
        _workspaceAPI = workspaceAPI;
    }

    public void setUserAPI(UsersAPI userAPI) {
        _userAPI = userAPI;
    }

    public void setEventAPI(EventsAPI eventAPI) {
        _eventAPI = eventAPI;
    }

    public void setDataModel(DataModel dataModel) {
        _dataModel = dataModel;
    }

    public void setChunkManager(ChunkManager chunkManager) {
        _chunkManager = chunkManager;
    }

    public void setConfiguration(Configuration configuration) {
        _configuration = configuration;
    }
}
