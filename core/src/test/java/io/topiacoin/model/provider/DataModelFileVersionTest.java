package io.topiacoin.model.provider;

import io.topiacoin.model.DataModel;
import io.topiacoin.model.File;
import io.topiacoin.model.FileTag;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.FileVersionReceipt;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.FileTagAlreadyExistsException;
import io.topiacoin.model.exceptions.FileVersionAlreadyExistsException;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchFileTagException;
import io.topiacoin.model.exceptions.NoSuchFileVersionException;
import io.topiacoin.model.exceptions.NoSuchFileVersionReceiptException;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

public abstract class DataModelFileVersionTest {

    public abstract DataModel initDataModel();

    // -------- File Version Tests --------

    @Test
    public void testFileVersionCRUD() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        fileVersionIDs = dataModel.getAvailableVersionsOfFile(fileID) ;
        assertNotNull(fileVersionIDs);
        assertEquals(0, fileVersionIDs.size());

        fileVersions = dataModel.getFileVersionsForFile(fileID);
        assertNotNull(fileVersions);
        assertEquals(0, fileVersions.size());

        dataModel.addFileVersion(fileID, fileVersion);

        fileVersions = dataModel.getFileVersionsForFile(fileID);
        assertNotNull(fileVersions);
        assertEquals(1, fileVersions.size());
        assertEquals(fileVersion, fileVersions.get(0));
        assertNotSame(fileVersion, fileVersions.get(0));

        fileVersionIDs = dataModel.getAvailableVersionsOfFile(fileID) ;
        assertNotNull(fileVersionIDs);
        assertEquals(1, fileVersionIDs.size());
        assertEquals(versionID, fileVersionIDs.get(0));

        FileVersion fetchedVersion = dataModel.getFileVersion(fileID, versionID) ;
        assertNotNull (fetchedVersion) ;
        assertEquals(fileVersion, fetchedVersion);

        fileVersion.setDate(1000);
        dataModel.updateFileVersion(fileID, fileVersion);

        fileVersions = dataModel.getFileVersionsForFile(fileID);
        assertNotNull(fileVersions);
        assertEquals(1, fileVersions.size());
        assertEquals(fileVersion, fileVersions.get(0));
        assertNotSame(fileVersion, fileVersions.get(0));

        fileVersionIDs = dataModel.getAvailableVersionsOfFile(fileID) ;
        assertNotNull(fileVersionIDs);
        assertEquals(1, fileVersionIDs.size());
        assertEquals(versionID, fileVersionIDs.get(0));

        dataModel.removeFileVersion(fileID, fileVersion);

        fileVersions = dataModel.getFileVersionsForFile(fileID);
        assertNotNull(fileVersions);
        assertEquals(0, fileVersions.size());

        fileVersionIDs = dataModel.getAvailableVersionsOfFile(fileID) ;
        assertNotNull(fileVersionIDs);
        assertEquals(0, fileVersionIDs.size());
    }

    @Test
    public void testRemoveFileVersionByID() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        List<FileVersion> fileVersions;

        dataModel.removeFileVersion(fileID, versionID);

        fileVersions = dataModel.getFileVersionsForFile(fileID);
        assertNotNull(fileVersions);
        assertEquals(0, fileVersions.size());
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetAvailableVersionsForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        fileVersionIDs = dataModel.getAvailableVersionsOfFile(fileID) ;
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetFileVersionsForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        fileVersions = dataModel.getFileVersionsForFile(fileID);
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetFileVersionForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        FileVersion fetchedVersion = dataModel.getFileVersion(fileID, versionID) ;
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testGetNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        FileVersion fetchedVersion = dataModel.getFileVersion(fileID, versionID) ;
    }

    @Test(expected = NoSuchFileException.class)
    public void testAddFileVersionForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        dataModel.addFileVersion(fileID, fileVersion);
    }

    @Test(expected = FileVersionAlreadyExistsException.class)
    public void testAddDuplicateFileVersionForFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        dataModel.addFileVersion(fileID, fileVersion);

        dataModel.addFileVersion(fileID, fileVersion);
    }

    @Test(expected = NoSuchFileException.class)
    public void testUpdateFileFileVersionForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        dataModel.updateFileVersion(fileID, fileVersion);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testUpdateNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        dataModel.updateFileVersion(fileID, fileVersion);
    }

    @Test(expected = NoSuchFileException.class)
    public void testRemoveFileVersionForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        dataModel.removeFileVersion(fileID, fileVersion);
    }

    @Test(expected = NoSuchFileException.class)
    public void testRemoveFileVersionByIDForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        dataModel.removeFileVersion(fileID, versionID);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testRemoveNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        dataModel.removeFileVersion(fileID, fileVersion);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testRemoveNonExistentFileVersionByID() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileVersion> fileVersions;
        List<String> fileVersionIDs;

        dataModel.removeFileVersion(fileID, versionID);
    }


    // -------- File Version Receipts Tests --------

    @Test
    public void testFileVersionReceiptCRUD() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        List<FileVersionReceipt> receipts ;

        receipts = dataModel.getFileVersionReceipts(fileID, versionID) ;
        assertNotNull ( receipts ) ;
        assertEquals(0, receipts.size()) ;

        dataModel.addFileVersionReceipt(fileID, versionID, fileReceipt) ;

        receipts = dataModel.getFileVersionReceipts(fileID, versionID) ;
        assertNotNull ( receipts ) ;
        assertEquals(1, receipts.size()) ;
        assertEquals(fileReceipt, receipts.get(0)) ;
        assertNotSame(fileReceipt, receipts.get(0));

        fileReceipt.setDate(10000);
        dataModel.updateFileVersionReceipt(fileID, versionID, fileReceipt);

        receipts = dataModel.getFileVersionReceipts(fileID, versionID) ;
        assertNotNull ( receipts ) ;
        assertEquals(1, receipts.size()) ;
        assertEquals(fileReceipt, receipts.get(0)) ;
        assertNotSame(fileReceipt, receipts.get(0));

        dataModel.removeFileVersionReceipt(fileID, versionID, fileReceipt);

        receipts = dataModel.getFileVersionReceipts(fileID, versionID) ;
        assertNotNull ( receipts ) ;
        assertEquals(0, receipts.size()) ;
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetFileVersionReceiptsForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileVersionReceipt> receipts ;

        receipts = dataModel.getFileVersionReceipts(fileID, versionID) ;
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testGetFileVersionReceiptForNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileVersionReceipt> receipts ;

        receipts = dataModel.getFileVersionReceipts(fileID, versionID) ;
    }

    @Test(expected = NoSuchFileException.class)
    public void testAddFileVersionReceiptForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.addFileVersionReceipt(fileID, versionID, fileReceipt) ;
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testAddFileVersionReceiptForNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.addFileVersionReceipt(fileID, versionID, fileReceipt) ;
    }

    @Test(expected = NoSuchFileException.class)
    public void testUpdateFileVersionReceiptForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.updateFileVersionReceipt(fileID, versionID, fileReceipt);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testUpdateFileVersionReceiptForNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.updateFileVersionReceipt(fileID, versionID, fileReceipt);
    }

    @Test(expected = NoSuchFileVersionReceiptException.class)
    public void testUpdateNonExistentFileVersionReceipt() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        dataModel.updateFileVersionReceipt(fileID, versionID, fileReceipt);
    }

    @Test(expected = NoSuchFileException.class)
    public void testRemoveFileVersionReceiptForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.removeFileVersionReceipt(fileID, versionID, fileReceipt);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testRemoveFileVersionReceiptForNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.removeFileVersionReceipt(fileID, versionID, fileReceipt);
    }

    @Test(expected = NoSuchFileVersionReceiptException.class)
    public void testRemoveNonExistentFileVersionReceipt() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileVersionReceipt fileReceipt = new FileVersionReceipt() ;
        fileReceipt.setEntryID(fileID);
        fileReceipt.setVersionID(versionID);
        fileReceipt.setRecipientID("George Bush");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        dataModel.removeFileVersionReceipt(fileID, versionID, fileReceipt);
    }

    // -------- File Tag Tests --------

    @Test
    public void testFileTagCRUD() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        List<FileTag> tags ;

        tags = dataModel.getTagsForFileVersion(fileID, versionID) ;
        assertNotNull (tags) ;
        assertEquals(0, tags.size()) ;

        dataModel.addTagForFile(fileID, versionID, fileTag);

        tags = dataModel.getTagsForFileVersion(fileID, versionID) ;
        assertNotNull (tags) ;
        assertEquals(1, tags.size()) ;
        assertEquals(fileTag, tags.get(0));
        assertNotSame(fileTag, tags.get(0));

        dataModel.removeTagForFile(fileID, versionID, fileTag);

        tags = dataModel.getTagsForFileVersion(fileID, versionID) ;
        assertNotNull (tags) ;
        assertEquals(0, tags.size()) ;
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetFileTagsForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<FileTag> tags ;

        tags = dataModel.getTagsForFileVersion(fileID, versionID) ;
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testGetFileTagsForNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileTag> tags ;

        tags = dataModel.getTagsForFileVersion(fileID, versionID) ;
    }

    @Test(expected = NoSuchFileException.class)
    public void testAddFileTagForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.addTagForFile(fileID, versionID, fileTag);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testAddFileTagForNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.addTagForFile(fileID, versionID, fileTag);
    }

    @Test(expected = FileTagAlreadyExistsException.class)
    public void testAddDuplicateFileTag() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        dataModel.addTagForFile(fileID, versionID, fileTag);

        dataModel.addTagForFile(fileID, versionID, fileTag);
    }

    @Test(expected = NoSuchFileException.class)
    public void testRemoveFileTagForNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.removeTagForFile(fileID, versionID, fileTag);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testRemoveFileTagForNonExistentFileVersion() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.removeTagForFile(fileID, versionID, fileTag);
    }

    @Test(expected = NoSuchFileTagException.class)
    public void testRemoveNonExistentFileTag() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String versionID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        FileVersion fileVersion = new FileVersion();
        fileVersion.setVersionID(versionID);
        fileVersion.setEntryID(fileID);

        FileTag fileTag = new FileTag() ;
        fileTag.setScope("public");
        fileTag.setValue("booyah");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        dataModel.removeTagForFile(fileID, versionID, fileTag);
    }

}
