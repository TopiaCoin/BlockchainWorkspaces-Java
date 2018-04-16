package io.topiacoin.model;

import io.topiacoin.model.exceptions.FileChunkAlreadyExistsException;
import io.topiacoin.model.exceptions.NoSuchFileChunkException;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchFileVersionException;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class DataModelFileChunkTest {


    // -------- File Chunk Tests --------

    @Test
    public void testFileChunkCRUD() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        List<FileChunk> fileChunks;

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
        assertNotNull(fileChunks);
        assertEquals(0, fileChunks.size());

        dataModel.addChunkForFile(fileID, versionID, fileChunk);

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
        assertNotNull(fileChunks);
        assertEquals(1, fileChunks.size());
        assertEquals(fileChunk, fileChunks.get(0));
        assertNotSame(fileChunk, fileChunks.get(0));

        fileChunk.setCompressionAlgorithm("GZIP");
        dataModel.updateChunkForFile(fileID, versionID, fileChunk);

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
        assertNotNull(fileChunks);
        assertEquals(1, fileChunks.size());
        assertEquals(fileChunk, fileChunks.get(0));
        assertNotSame(fileChunk, fileChunks.get(0));

        dataModel.removeChunkForFile(fileID, versionID, fileChunk);

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
        assertNotNull(fileChunks);
        assertEquals(0, fileChunks.size());
    }

    @Test
    public void testChangingAddedFileChunkDoesNotChangeModel() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        List<FileChunk> fileChunks;

        dataModel.addChunkForFile(fileID, versionID, fileChunk);

        fileChunk.setCompressionAlgorithm("GZIP");

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
        assertNotNull(fileChunks);
        assertEquals(1, fileChunks.size());
        assertNotEquals(fileChunk, fileChunks.get(0));
    }


    @Test
    public void testChangingFetchedFileChunkDoesNotChangeModel() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        List<FileChunk> fileChunks;

        dataModel.addChunkForFile(fileID, versionID, fileChunk);

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
        FileChunk fetchedChunk = fileChunks.get(0);

        fetchedChunk.setCompressionAlgorithm("GZIP");

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
        assertNotNull(fileChunks);
        assertEquals(1, fileChunks.size());
        assertEquals(fileChunk, fileChunks.get(0));
        assertNotEquals(fetchedChunk, fileChunks.get(0));
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetFileChunksForNonExistentFile() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);

        List<FileChunk> fileChunks;

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testGetFileChunksForNonExistentFileVersion() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        List<FileChunk> fileChunks;

        fileChunks = dataModel.getChunksForFileVersion(fileID, versionID);
    }

    @Test(expected = NoSuchFileException.class)
    public void testAddFileChunkForNonExistentFile() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);

        dataModel.addChunkForFile(fileID, versionID, fileChunk);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testAddFileChunkForNonExistentFileVersion() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.addChunkForFile(fileID, versionID, fileChunk);
    }

    @Test(expected = FileChunkAlreadyExistsException.class)
    public void testAddDuplicateFileChunk() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        dataModel.addChunkForFile(fileID, versionID, fileChunk);

        dataModel.addChunkForFile(fileID, versionID, fileChunk);
    }

    @Test(expected = NoSuchFileException.class)
    public void testUpdateFileChunkForNonExistentFile() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);

        dataModel.updateChunkForFile(fileID, versionID, fileChunk);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testUpdateFileChunkForNonExistentFileVersion() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.updateChunkForFile(fileID, versionID, fileChunk);
    }

    @Test(expected = NoSuchFileChunkException.class)
    public void testUpdateNonExistentFileChunk() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        dataModel.updateChunkForFile(fileID, versionID, fileChunk);

    }

    @Test(expected = NoSuchFileException.class)
    public void testRemoveFileChunkForNonExistentFile() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);

        dataModel.removeChunkForFile(fileID, versionID, fileChunk);
    }

    @Test(expected = NoSuchFileVersionException.class)
    public void testRemoveFileChunkForNonExistentFileVersion() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);

        dataModel.removeChunkForFile(fileID, versionID, fileChunk);
    }

    @Test(expected = NoSuchFileChunkException.class)
    public void testRemoveNonExistentFileChunk() throws Exception {
        String workspaceID = UUID.randomUUID().toString();
        String fileID = UUID.randomUUID().toString();
        String chunkID = UUID.randomUUID().toString();
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

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setIndex(0);

        DataModel dataModel = new DataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addFileToWorkspace(workspaceID, file);
        dataModel.addFileVersion(fileID, fileVersion);

        dataModel.removeChunkForFile(fileID, versionID, fileChunk);
    }

}
