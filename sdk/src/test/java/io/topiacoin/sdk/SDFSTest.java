package io.topiacoin.sdk;

import io.topiacoin.chunks.ChunkManager;
import io.topiacoin.chunks.exceptions.NoSuchChunkException;
import io.topiacoin.chunks.intf.ChunksFetchHandler;
import io.topiacoin.core.Configuration;
import io.topiacoin.core.callbacks.DownloadFileVersionCallback;
import io.topiacoin.core.callbacks.SaveFileVersionCallback;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.crypto.CryptoUtils;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.File;
import io.topiacoin.model.FileChunk;
import io.topiacoin.model.FileVersion;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.NoSuchFileException;
import io.topiacoin.model.exceptions.NoSuchFileVersionException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SDFSTest {

    boolean success = false;

    @Test
    public void testDownloadFile() throws Exception {

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        List<String> chunkIDs = Collections.singletonList(chunkID);
        Capture<ChunksFetchHandler> callbackCapture = EasyMock.newCapture();
        EasyMock.reset(chunkManager);
        chunkManager.fetchChunks(EasyMock.eq(chunkIDs), EasyMock.eq(workspaceID), capture(callbackCapture), EasyMock.isNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                // Report that the chunk fetch was successful.
                callbackCapture.getValue().finishedFetchingChunks(chunkIDs, Collections.emptyList(), null);
                return null;
            }
        });
        EasyMock.replay(chunkManager);

        sdfs.downloadFileVersion(workspaceID, fileID, versionID, new DownloadFileVersionCallback() {
            @Override
            public void didDownloadFileVersion(String fileGUID, String fileVersionGUID) {
                assertEquals(fileID, fileGUID);
                assertEquals(versionID, fileVersionGUID);
                success = true;
            }

            @Override
            public void failedToDownloadFileVersion(String fileGUID, String fileVersionGUID, String failureMessage) {

            }
        });

        Thread.sleep(1000);

        assertTrue(success);
        EasyMock.verify(chunkManager);
    }

    @Test
    public void testDownloadFileWithMultipleChunks() throws Exception {

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID1 = "chunk-id-1";
        final String chunkID2 = "chunk-id-2";

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk1 = new FileChunk();
        fileChunk1.setChunkID(chunkID1);
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk1);
        FileChunk fileChunk2 = new FileChunk();
        fileChunk2.setChunkID(chunkID2);
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk2);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        List<String> chunkIDs = Arrays.asList(chunkID1, chunkID2);
        Capture<ChunksFetchHandler> callbackCapture = EasyMock.newCapture();
        EasyMock.reset(chunkManager);
        chunkManager.fetchChunks(EasyMock.eq(chunkIDs), EasyMock.eq(workspaceID), capture(callbackCapture), EasyMock.isNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                // Report that the chunk fetch was successful.
                callbackCapture.getValue().finishedFetchingChunks(chunkIDs, Collections.emptyList(), null);
                return null;
            }
        });
        EasyMock.replay(chunkManager);

        sdfs.downloadFileVersion(workspaceID, fileID, versionID, new DownloadFileVersionCallback() {
            @Override
            public void didDownloadFileVersion(String fileGUID, String fileVersionGUID) {
                assertEquals(fileID, fileGUID);
                assertEquals(versionID, fileVersionGUID);
                success = true;
            }

            @Override
            public void failedToDownloadFileVersion(String fileGUID, String fileVersionGUID, String failureMessage) {

            }
        });

        Thread.sleep(1000);

        assertTrue(success);
        EasyMock.verify(chunkManager);
    }

    @Test
    public void testDownloadFileWithWrongWorkspaceID() throws Exception {

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        final String workspaceID = "workspace-id";
        final String otherWorkspaceID = "other-workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setGuid(otherWorkspaceID);
        dataModel.addWorkspace(otherWorkspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        List<String> chunkIDs = Collections.singletonList(chunkID);
        Capture<ChunksFetchHandler> callbackCapture = EasyMock.newCapture();
        EasyMock.reset(chunkManager);
        EasyMock.replay(chunkManager);

        try {
            sdfs.downloadFileVersion(otherWorkspaceID, fileID, versionID, null);
            fail ( "Expected NoSuchFileException Not Thrown");
        } catch ( NoSuchFileException e ){
            // Expected Exception
        }
    }

    @Test
    public void testDownloadFileFromNonExistent() throws Exception {

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        final String workspaceID = "workspace-id";
        final String nonExistentWorkspaceID = "non-existent-workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        List<String> chunkIDs = Collections.singletonList(chunkID);
        Capture<ChunksFetchHandler> callbackCapture = EasyMock.newCapture();
        EasyMock.reset(chunkManager);
        EasyMock.replay(chunkManager);

        try {
            sdfs.downloadFileVersion(nonExistentWorkspaceID, fileID, versionID, null);
            fail ( "Expected NoSuchWorkspaceException Not Thrown");
        } catch ( NoSuchWorkspaceException e ){
            // Expected Exception
        }
    }

    @Test
    public void testDownloadNonExistentFileVersion() throws Exception {

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String nonExistentVersionID = "non-existent-version-id";
        final String chunkID = "chunk-id";

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        List<String> chunkIDs = Collections.singletonList(chunkID);
        Capture<ChunksFetchHandler> callbackCapture = EasyMock.newCapture();
        EasyMock.reset(chunkManager);
        EasyMock.replay(chunkManager);

        try {
            sdfs.downloadFileVersion(workspaceID, fileID, nonExistentVersionID, null);
            fail ( "Expected NoSuchFileVersionException Not Thrown");
        } catch ( NoSuchFileVersionException e ){
            // Expected Exception
        }
    }

    @Test
    public void testDownloadFileWithError() throws Exception {

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        List<String> chunkIDs = Collections.singletonList(chunkID);
        Capture<ChunksFetchHandler> callbackCapture = EasyMock.newCapture();
        EasyMock.reset(chunkManager);
        chunkManager.fetchChunks(EasyMock.eq(chunkIDs), EasyMock.eq(workspaceID), capture(callbackCapture), EasyMock.isNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                // Report that the chunk fetch was successful.
                callbackCapture.getValue().errorFetchingChunks("Failure",null, null);
                return null;
            }
        });
        EasyMock.replay(chunkManager);

        sdfs.downloadFileVersion(workspaceID, fileID, versionID, new DownloadFileVersionCallback() {
            @Override
            public void didDownloadFileVersion(String fileGUID, String fileVersionGUID) {
            }

            @Override
            public void failedToDownloadFileVersion(String fileGUID, String fileVersionGUID, String failureMessage) {
                assertEquals(fileID, fileGUID);
                assertEquals(versionID, fileVersionGUID);
                success = true;
            }
        });

        Thread.sleep(1000);

        assertTrue(success);
        EasyMock.verify(chunkManager);
    }

    // -------- SaveFile Tests --------

    @Test
    public void testSaveFile() throws Exception {

        Random random = new Random();

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        String fileName = "TestFile.txt";
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        final SecretKey chunkKey = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        final IvParameterSpec iv = new IvParameterSpec(ivBytes);

        // Clean up from any previous test
        java.io.File oldFile = new java.io.File("./target/" + fileName);
        oldFile.delete();

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        testFile.setName(fileName);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setChunkKey(chunkKey);
        fileChunk.setInitializationVector(iv.getIV());
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Create the Encrypted Chunk Data
        byte[] chunkData1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi sit amet magna et neque semper auctor. Maecenas ullamcorper elit vitae pellentesque feugiat. Nam vitae felis elit. Praesent vehicula nunc orci, a fermentum tortor sodales condimentum. Cras non nulla blandit diam finibus auctor. Aliquam vel maximus turpis. Curabitur non luctus leo.".getBytes("UTF-8");
        byte[] cipherData1 = CryptoUtils.encryptWithSecretKey(chunkData1, chunkKey, iv);
        InputStream chunkStream1 = new ByteArrayInputStream(cipherData1);

        // Establish the Mock Object Expectations
        EasyMock.reset(chunkManager);
        EasyMock.expect(chunkManager.hasChunk(chunkID)).andReturn(true);
        EasyMock.expect(chunkManager.getChunkDataAsStream(chunkID)).andReturn(chunkStream1);
        EasyMock.replay(chunkManager);

        String targetDirectory = "./target";

        sdfs.saveFileVersion(workspaceID, fileID, versionID, targetDirectory, new SaveFileVersionCallback() {
            @Override
            public void didSaveFile(String fileGUID, String fileVersionGUID, java.io.File targetFile) {
                assertEquals(fileID, fileGUID);
                assertEquals(versionID, fileVersionGUID);
                success = true;
            }

            @Override
            public void failedToSaveFile(String fileGUID, String fileVersionGUID, String failureMessage) {

            }
        });

        // Wait a moment for the background processing of the operation to execute.
        Thread.sleep(250);

        // Verify the results!
        assertTrue(success);
        EasyMock.verify(chunkManager);

        FileInputStream fis = new FileInputStream("./target/" + testFile.getName());
        byte[] fileData = new byte[fis.available()];
        int bytesRead = fis.read(fileData);

        assertEquals(chunkData1.length, bytesRead);
        assertArrayEquals(chunkData1, fileData);
    }


    @Test
    public void testSaveFileWithMultipleChunks() throws Exception {

        Random random = new Random();

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        String fileName = "TestFile.txt";
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID1 = "chunk-id-1";
        final String chunkID2 = "chunk-id-2";

        byte[] key1Bytes = new byte[16];
        random.nextBytes(key1Bytes);
        final SecretKey chunkKey1 = new SecretKeySpec(key1Bytes, "AES");
        byte[] iv1Bytes = new byte[16];
        random.nextBytes(iv1Bytes);
        final IvParameterSpec iv1 = new IvParameterSpec(iv1Bytes);

        byte[] key2Bytes = new byte[16];
        random.nextBytes(key2Bytes);
        final SecretKey chunkKey2 = new SecretKeySpec(key2Bytes, "AES");
        byte[] iv2Bytes = new byte[16];
        random.nextBytes(iv2Bytes);
        final IvParameterSpec iv2 = new IvParameterSpec(iv2Bytes);

        // Clean up from any previous test
        java.io.File oldFile = new java.io.File("./target/" + fileName);
        oldFile.delete();

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        testFile.setName(fileName);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk1 = new FileChunk();
        fileChunk1.setChunkID(chunkID1);
        fileChunk1.setChunkKey(chunkKey1);
        fileChunk1.setInitializationVector(iv1.getIV());
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk1);

        FileChunk fileChunk2 = new FileChunk();
        fileChunk2.setChunkID(chunkID2);
        fileChunk2.setChunkKey(chunkKey2);
        fileChunk2.setInitializationVector(iv2.getIV());
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk2);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Create the Encrypted Chunk Data
        byte[] chunkData1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi sit amet magna et neque semper auctor. Maecenas ullamcorper elit vitae pellentesque feugiat. Nam vitae felis elit. ".getBytes("UTF-8");
        byte[] cipherData1 = CryptoUtils.encryptWithSecretKey(chunkData1, chunkKey1, iv1);
        InputStream chunkStream1 = new ByteArrayInputStream(cipherData1);

        byte[] chunkData2 = "Praesent vehicula nunc orci, a fermentum tortor sodales condimentum. Cras non nulla blandit diam finibus auctor. Aliquam vel maximus turpis. Curabitur non luctus leo.".getBytes("UTF-8");
        byte[] cipherData2 = CryptoUtils.encryptWithSecretKey(chunkData2, chunkKey2, iv2);
        InputStream chunkStream2 = new ByteArrayInputStream(cipherData2);

        // Establish the Mock Object Expectations
        EasyMock.reset(chunkManager);
        EasyMock.expect(chunkManager.hasChunk(chunkID1)).andReturn(true);
        EasyMock.expect(chunkManager.getChunkDataAsStream(chunkID1)).andReturn(chunkStream1);
        EasyMock.expect(chunkManager.hasChunk(chunkID2)).andReturn(true);
        EasyMock.expect(chunkManager.getChunkDataAsStream(chunkID2)).andReturn(chunkStream2);
        EasyMock.replay(chunkManager);

        String targetDirectory = "./target";

        sdfs.saveFileVersion(workspaceID, fileID, versionID, targetDirectory, new SaveFileVersionCallback() {
            @Override
            public void didSaveFile(String fileGUID, String fileVersionGUID, java.io.File targetFile) {
                assertEquals(fileID, fileGUID);
                assertEquals(versionID, fileVersionGUID);
                success = true;
            }

            @Override
            public void failedToSaveFile(String fileGUID, String fileVersionGUID, String failureMessage) {

            }
        });

        // Wait a moment for the background processing of the operation to execute.
        Thread.sleep(250);

        // Verify the results!
        assertTrue(success);
        EasyMock.verify(chunkManager);

        byte[] expectedData = new byte[chunkData1.length + chunkData2.length] ;
        System.arraycopy(chunkData1, 0, expectedData, 0, chunkData1.length);
        System.arraycopy(chunkData2, 0, expectedData, chunkData1.length, chunkData2.length);
        FileInputStream fis = new FileInputStream("./target/" + testFile.getName());
        byte[] fileData = new byte[fis.available()];
        int bytesRead = fis.read(fileData);

        assertEquals(expectedData.length, bytesRead);
        assertArrayEquals(expectedData, fileData);
    }

    @Test
    public void testSaveFileWithMissingChunk() throws Exception {

        Random random = new Random();

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        String fileName = "TestFile.txt";
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        final SecretKey chunkKey = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        final IvParameterSpec iv = new IvParameterSpec(ivBytes);

        // Clean up from any previous test
        java.io.File oldFile = new java.io.File("./target/" + fileName);
        oldFile.delete();

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        testFile.setName(fileName);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        FileVersion testVersion = new FileVersion();
        testVersion.setVersionID(versionID);
        testVersion.setEntryID(fileID);
        dataModel.addFileVersion(testFile.getEntryID(), testVersion);

        FileChunk fileChunk = new FileChunk();
        fileChunk.setChunkID(chunkID);
        fileChunk.setChunkKey(chunkKey);
        fileChunk.setInitializationVector(iv.getIV());
        dataModel.addChunkForFile(testFile.getEntryID(), testVersion.getVersionID(), fileChunk);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        EasyMock.reset(chunkManager);
        EasyMock.expect(chunkManager.hasChunk(chunkID)).andReturn(false);
        EasyMock.replay(chunkManager);

        String targetDirectory = "./target";

        try {
            sdfs.saveFileVersion(workspaceID, fileID, versionID, targetDirectory, null);
            fail ( "Expected IOException was not thrown");
        } catch ( IOException e ) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testSaveFileWithNonExistentVersion() throws Exception {

        Random random = new Random();

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        String fileName = "TestFile.txt";
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        final SecretKey chunkKey = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        final IvParameterSpec iv = new IvParameterSpec(ivBytes);

        // Clean up from any previous test
        java.io.File oldFile = new java.io.File("./target/" + fileName);
        oldFile.delete();

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        testFile.setName(fileName);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        EasyMock.reset(chunkManager);
        EasyMock.expect(chunkManager.hasChunk(chunkID)).andReturn(false);
        EasyMock.replay(chunkManager);

        String targetDirectory = "./target";

        try {
            sdfs.saveFileVersion(workspaceID, fileID, versionID, targetDirectory, null);
            fail ( "Expected NoSuchFileVersionException was not thrown");
        } catch ( NoSuchFileVersionException e ) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testSaveFileWithWrongWorkspaceID() throws Exception {

        Random random = new Random();

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        String fileName = "TestFile.txt";
        final String workspaceID = "workspace-id";
        final String otherWorkspaceID = "other-workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        final SecretKey chunkKey = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        final IvParameterSpec iv = new IvParameterSpec(ivBytes);

        // Clean up from any previous test
        java.io.File oldFile = new java.io.File("./target/" + fileName);
        oldFile.delete();

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setGuid(otherWorkspaceID);
        dataModel.addWorkspace(otherWorkspace);

        File testFile = new File();
        testFile.setEntryID(fileID);
        testFile.setContainerID(workspaceID);
        testFile.setName(fileName);
        dataModel.addFileToWorkspace(workspaceID, testFile);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        EasyMock.reset(chunkManager);
        EasyMock.expect(chunkManager.hasChunk(chunkID)).andReturn(false);
        EasyMock.replay(chunkManager);

        String targetDirectory = "./target";

        try {
            sdfs.saveFileVersion(otherWorkspaceID, fileID, versionID, targetDirectory, null);
            fail ( "Expected NoSuchFileException was not thrown");
        } catch ( NoSuchFileException e ) {
            // NOOP - Expected Exception
        }
    }

    @Test
    public void testSaveFileWithNonExistentFile() throws Exception {

        Random random = new Random();

        final KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        final Configuration configuration = new DefaultConfiguration();
        String fileName = "TestFile.txt";
        final String workspaceID = "workspace-id";
        final String fileID = "file-id";
        final String versionID = "version-id";
        final String chunkID = "chunk-id";
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        final SecretKey chunkKey = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        final IvParameterSpec iv = new IvParameterSpec(ivBytes);

        // Clean up from any previous test
        java.io.File oldFile = new java.io.File("./target/" + fileName);
        oldFile.delete();

        // Create the object being tested
        SDFS sdfs = new SDFS();

        // Create the Concrete Test objects
        DataModel dataModel = new DataModel() {
            // This anonymous subclass exists so that we can bypass the singleton instance for testing.
        };

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        dataModel.addWorkspace(workspace);

        // Setup the Mock Objects for the test
        ChunkManager chunkManager = EasyMock.createMock(ChunkManager.class);

        // Setup the dependencies
        sdfs.setDataModel(dataModel);
        sdfs.setChunkManager(chunkManager);

        // Establish the Mock Object Expectations
        EasyMock.reset(chunkManager);
        EasyMock.expect(chunkManager.hasChunk(chunkID)).andReturn(false);
        EasyMock.replay(chunkManager);

        String targetDirectory = "./target";

        try {
            sdfs.saveFileVersion(workspaceID, fileID, versionID, targetDirectory, null);
            fail ( "Expected NoSuchFileException was not thrown");
        } catch ( NoSuchFileException e ) {
            // NOOP - Expected Exception
        }
    }

}
