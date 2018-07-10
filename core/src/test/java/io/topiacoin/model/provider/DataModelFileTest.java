package io.topiacoin.model.provider;

import io.topiacoin.model.DataModel;
import io.topiacoin.model.File;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.FileAlreadyExistsException;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

public abstract class DataModelFileTest {

    public abstract DataModel initDataModel();

    public abstract void tearDownDataModel();

    @After
    public void destroy() {
        tearDownDataModel();
    }


    // -------- File Tests --------

    @Test
    public void testFileCRUD() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<File> files;

        files = dataModel.getFilesInWorkspace(workspaceID);
        assertNotNull(files);
        assertEquals(0, files.size());

        dataModel.addFileToWorkspace(workspaceID, file);

        files = dataModel.getFilesInWorkspace(workspaceID);
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals(file, files.get(0));
        assertNotSame(file, files.get(0));

        File fetchedFile = dataModel.getFile(fileID);
        assertNotNull(fetchedFile);
        assertEquals(file, fetchedFile);
        assertNotSame(file, fetchedFile);

        file.setName("bar");
        dataModel.updateFileInWorkspace(workspaceID, file);

        files = dataModel.getFilesInWorkspace(workspaceID);
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals(file, files.get(0));
        assertNotSame(file, files.get(0));

        dataModel.removeFileFromWorkspace(workspaceID, file);

        files = dataModel.getFilesInWorkspace(workspaceID);
        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void testGetFilesFromWorkspaceWithParentID() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID1 = UUID.randomUUID().toString();
        String fileID2 = UUID.randomUUID().toString();
        String parentID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file1 = new File();
        file1.setEntryID(fileID1);
        file1.setName("Foo");

        File file2 = new File();
        file2.setEntryID(fileID2);
        file2.setName("Foo");
        file2.setParentID(parentID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file1);
        dataModel.addFileToWorkspace(workspaceID, file2);

        List<File> files;

        files = dataModel.getFilesInWorkspace(workspaceID, null);
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals(file1, files.get(0));
        assertNotSame(file1, files.get(0));


        files = dataModel.getFilesInWorkspace(workspaceID, parentID);
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals(file2, files.get(0));
        assertNotSame(file2, files.get(0));
    }

    @Test
    public void testRemoveFilesFromWorkspaceByFileID() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID1 = UUID.randomUUID().toString();
        String fileID2 = UUID.randomUUID().toString();
        String parentID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file1 = new File();
        file1.setEntryID(fileID1);
        file1.setName("Foo");

        File file2 = new File();
        file2.setEntryID(fileID2);
        file2.setName("Foo");
        file2.setParentID(parentID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file1);
        dataModel.addFileToWorkspace(workspaceID, file2);

        List<File> files;

        files = dataModel.getFilesInWorkspace(workspaceID);
        assertNotNull(files);
        assertEquals(2, files.size());

        dataModel.removeFileFromWorkspace(workspaceID, fileID1);

        files = dataModel.getFilesInWorkspace(workspaceID);
        assertNotNull(files);
        assertEquals(1, files.size());
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testGetFilesFromNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        List<File> files;

        files = dataModel.getFilesInWorkspace(workspaceID);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testGetFilesWithParentFromNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        List<File> files;

        files = dataModel.getFilesInWorkspace(workspaceID, null);
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetNonExistentFile() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        File fetchedFile = dataModel.getFile(fileID);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testAddFileToNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        dataModel.addFileToWorkspace(workspaceID, file);
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testAddDuplicateFileToWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.addFileToWorkspace(workspaceID, file);
    }


    @Test(expected = NoSuchWorkspaceException.class)
    public void testUpdateFileInNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        dataModel.updateFileInWorkspace(workspaceID, file);
    }

    @Test(expected = NoSuchFileException.class)
    public void testUpdateNonExistentFileInWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.updateFileInWorkspace(workspaceID, file);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testRemoveFileFromNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        dataModel.removeFileFromWorkspace(workspaceID, file);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testRemoveFileIDFromNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();
        String ownerID = UUID.randomUUID().toString();

        DataModel dataModel = initDataModel();

        dataModel.removeFileFromWorkspace(workspaceID, fileID);
    }

    @Test(expected = NoSuchFileException.class)
    public void testRemoveNonExistentFileFromWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        File file = new File();
        file.setEntryID(fileID);
        file.setName("Foo");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.removeFileFromWorkspace(workspaceID, file);
    }

    @Test(expected = NoSuchFileException.class)
    public void testRemoveNonExistentFileIDFromWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        String fileID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.removeFileFromWorkspace(workspaceID, fileID);
    }
}
