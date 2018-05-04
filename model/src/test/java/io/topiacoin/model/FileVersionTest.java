package io.topiacoin.model;

import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FileVersionTest {

    @Test
    public void testDefaultConstructor() throws Exception {

        FileVersion fileVersion = new FileVersion() ;

        assertNull ( fileVersion.getEntryID()) ;
        assertNull(fileVersion.getVersionID());
        assertNull(fileVersion.getOwnerID());
        assertNull(fileVersion.getFileHash());
        assertNull(fileVersion.getStatus());
        assertEquals (0, fileVersion.getSize());
        assertEquals (0, fileVersion.getDate());
        assertEquals (0, fileVersion.getUploadDate());

        assertNotNull ( fileVersion.getFileChunks());
        assertNotNull ( fileVersion.getReceipts());
        assertNotNull ( fileVersion.getUserTags());
        assertNotNull ( fileVersion.getSystemTags());

    }

    @Test
    public void testConstructor() throws Exception {

        String entryID = "fizz-buzz";
        String versionID = "foo-bar";
        String ownerID = "Janner";
        long size = 98765;
        long date = 1234567890321L;
        long uploadDate = 1234567890123L;
        String fileHash = "HasheyHashHash";
        String status = "Irritated";
        List<FileTag> userTags = new ArrayList<FileTag>();
        List<FileTag> systemTags = new ArrayList<FileTag>();
        List<FileChunk> fileChunks = new ArrayList<FileChunk>();
        List<FileVersionReceipt> receipts = new ArrayList<FileVersionReceipt>();

        FileVersion fileVersion = new FileVersion(entryID, versionID, ownerID, size, date, uploadDate, fileHash, status, userTags, systemTags, fileChunks, receipts) ;

        assertEquals(entryID, fileVersion.getEntryID());
        assertEquals(versionID, fileVersion.getVersionID());
        assertEquals(ownerID, fileVersion.getOwnerID());
        assertEquals(size, fileVersion.getSize());
        assertEquals(date, fileVersion.getDate());
        assertEquals(uploadDate, fileVersion.getUploadDate());
        assertEquals(fileHash, fileVersion.getFileHash());
        assertEquals(status, fileVersion.getStatus());
        assertEquals(userTags, fileVersion.getUserTags());
        assertEquals(systemTags, fileVersion.getSystemTags());
        assertEquals(fileChunks, fileVersion.getFileChunks());
        assertEquals(receipts, fileVersion.getReceipts());

        assertNotSame(userTags, fileVersion.getUserTags());
        assertNotSame(systemTags, fileVersion.getSystemTags());
        assertNotSame(fileChunks, fileVersion.getFileChunks());
        assertNotSame(receipts, fileVersion.getReceipts());

    }


    @Test
    public void testConstructorWithNullCollections() throws Exception {

        String entryID = "fizz-buzz";
        String versionID = "foo-bar";
        String ownerID = "Janner";
        long size = 98765;
        long date = 1234567890321L;
        long uploadDate = 1234567890123L;
        String fileHash = "HasheyHashHash";
        String status = "Irritated";
        List<FileTag> userTags = null;
        List<FileTag> systemTags = null;
        List<FileChunk> fileChunks = null;
        List<FileVersionReceipt> receipts = null;

        FileVersion fileVersion = new FileVersion(entryID, versionID, ownerID, size, date, uploadDate, fileHash, status, userTags, systemTags, fileChunks, receipts) ;

        assertEquals(entryID, fileVersion.getEntryID());
        assertEquals(versionID, fileVersion.getVersionID());
        assertEquals(ownerID, fileVersion.getOwnerID());
        assertEquals(size, fileVersion.getSize());
        assertEquals(date, fileVersion.getDate());
        assertEquals(uploadDate, fileVersion.getUploadDate());
        assertEquals(fileHash, fileVersion.getFileHash());
        assertEquals(status, fileVersion.getStatus());

        assertNotNull(fileVersion.getUserTags());
        assertNotNull( fileVersion.getSystemTags());
        assertNotNull( fileVersion.getFileChunks());
        assertNotNull( fileVersion.getReceipts());
    }


    @Test
    public void testBasicAccessors() throws Exception {
        String entryID = "fizz-buzz";
        String versionID = "foo-bar";
        String ownerID = "Janner";
        long size = 98765;
        long date = 1234567890321L;
        long uploadDate = 1234567890123L;
        String fileHash = "HasheyHashHash";
        String status = "Irritated";

        FileVersion fileVersion = new FileVersion();

        // Check the Entry ID Accessors
        assertNull ( fileVersion.getEntryID()) ;
        fileVersion.setEntryID(entryID);
        assertEquals(entryID, fileVersion.getEntryID());
        fileVersion.setEntryID(null);
        assertNull ( fileVersion.getEntryID()) ;

        // Check the Version ID Accessors
        assertNull(fileVersion.getVersionID());
        fileVersion.setVersionID(versionID);
        assertEquals(versionID, fileVersion.getVersionID());
        fileVersion.setVersionID(null);
        assertNull(fileVersion.getVersionID());

        // Check the Owner ID Accessors
        assertNull(fileVersion.getOwnerID());
        fileVersion.setOwnerID(ownerID);
        assertEquals(ownerID, fileVersion.getOwnerID());
        fileVersion.setOwnerID(null);
        assertNull(fileVersion.getOwnerID());

        // Check the Size Accessors
        assertEquals (0, fileVersion.getSize());
        fileVersion.setSize(size);
        assertEquals(size, fileVersion.getSize());
        fileVersion.setSize(0);
        assertEquals (0, fileVersion.getSize());

        // Check the Date Accessors
        assertEquals (0, fileVersion.getDate());
        fileVersion.setDate(date);
        assertEquals(date, fileVersion.getDate());
        fileVersion.setDate(0);
        assertEquals (0, fileVersion.getDate());

        // Check the Upload Date Accessors
        assertEquals (0, fileVersion.getUploadDate());
        fileVersion.setUploadDate(uploadDate);
        assertEquals(uploadDate, fileVersion.getUploadDate());
        fileVersion.setUploadDate(0);
        assertEquals (0, fileVersion.getUploadDate());

        // Check the File Hash Accessors
        assertNull(fileVersion.getFileHash());
        fileVersion.setFileHash(fileHash);
        assertEquals(fileHash, fileVersion.getFileHash());
        fileVersion.setFileHash(null);
        assertNull(fileVersion.getFileHash());

        // Check the Status Accessors
        assertNull(fileVersion.getStatus());
        fileVersion.setStatus(status);
        assertEquals(status, fileVersion.getStatus());
        fileVersion.setStatus(null);
        assertNull(fileVersion.getStatus());
    }


    @Test
    public void testCollectionAccessors() throws Exception {
        String entryID = "fizz-buzz";
        String versionID = "foo-bar";
        String ownerID = "Janner";
        long date = 1234567890321L;
        List<FileTag> userTags = new ArrayList<FileTag>();
        List<FileTag> systemTags = new ArrayList<FileTag>();
        List<FileChunk> fileChunks = new ArrayList<FileChunk>();
        List<FileVersionReceipt> receipts = new ArrayList<FileVersionReceipt>();

        userTags.add(new FileTag ( "scope", "value")) ;

        systemTags.add (new FileTag("system", "value2")) ;

        SecretKey key = new SecretKeySpec(new byte[16], "AES");
        fileChunks.add(new FileChunk("1", 0, 12, 10, key, new byte[16], "SHA-256:AAAAAAAAAAAAAAAA", "SHA-256:AAAAAAAAAAAAAAAA", "ZIP"));

        receipts.add(new FileVersionReceipt(entryID, versionID, ownerID, date)) ;

        FileVersion fileVersion = new FileVersion();

        // Check the User Tags Accessors
        assertNotNull ( fileVersion.getUserTags());
        assertEquals ( 0, fileVersion.getUserTags().size()) ;
        fileVersion.setUserTags(userTags);
        assertEquals(userTags, fileVersion.getUserTags());
        assertNotSame(userTags, fileVersion.getUserTags());
        fileVersion.setUserTags(null);
        assertNotNull ( fileVersion.getUserTags());
        assertEquals ( 0, fileVersion.getUserTags().size()) ;

        // Check the System Tags Accessors
        assertNotNull ( fileVersion.getSystemTags());
        assertEquals(0, fileVersion.getSystemTags().size());
        fileVersion.setSystemTags(systemTags);
        assertEquals(systemTags, fileVersion.getSystemTags());
        assertNotSame(systemTags, fileVersion.getSystemTags());
        fileVersion.setSystemTags(null);
        assertNotNull ( fileVersion.getSystemTags());
        assertEquals(0, fileVersion.getSystemTags().size());

        // Check the File Chunks Accessors
        assertNotNull ( fileVersion.getFileChunks());
        assertEquals(0, fileVersion.getFileChunks().size());
        fileVersion.setFileChunks(fileChunks);
        assertEquals(fileChunks, fileVersion.getFileChunks());
        assertNotSame(fileChunks, fileVersion.getFileChunks());
        fileVersion.setFileChunks(null);
        assertNotNull ( fileVersion.getFileChunks());
        assertEquals(0, fileVersion.getFileChunks().size());

        // Check the Receipts Accessors
        assertNotNull ( fileVersion.getReceipts());
        assertEquals(0, fileVersion.getReceipts().size());
        fileVersion.setReceipts(receipts);
        assertEquals(receipts, fileVersion.getReceipts());
        assertNotSame(receipts, fileVersion.getReceipts());
        fileVersion.setReceipts(null);
        assertNotNull ( fileVersion.getReceipts());
        assertEquals(0, fileVersion.getReceipts().size());
    }


    @Test
    public void testEqualsAndHashCode() throws Exception {

        String entryID = "fizz-buzz";
        String versionID = "foo-bar";
        String ownerID = "Janner";
        long size = 98765;
        long date = 1234567890321L;
        long uploadDate = 1234567890123L;
        String fileHash = "HasheyHashHash";
        String status = "Irritated";
        List<FileTag> userTags = new ArrayList<FileTag>();
        List<FileTag> systemTags = new ArrayList<FileTag>();
        List<FileChunk> fileChunks = new ArrayList<FileChunk>();
        List<FileVersionReceipt> receipts = new ArrayList<FileVersionReceipt>();

        userTags.add(new FileTag ( "scope", "value")) ;

        systemTags.add (new FileTag("system", "value2")) ;

        SecretKey key = new SecretKeySpec(new byte[16], "AES");
        fileChunks.add(new FileChunk("1", 0, 12, 10, key, new byte[16], "SHA-256:AAAAAAAAAAAAAAAA", "SHA-256:AAAAAAAAAAAAAAAA", "ZIP"));

        receipts.add(new FileVersionReceipt(entryID, versionID, ownerID, date)) ;

        FileVersion fileVersion1 = new FileVersion(entryID, versionID, ownerID, size, date, uploadDate, fileHash, status, userTags, systemTags, fileChunks, receipts) ;
        FileVersion fileVersion2 = new FileVersion(entryID, versionID, ownerID, size, date, uploadDate, fileHash, status, userTags, systemTags, fileChunks, receipts) ;

        assertEquals(fileVersion1, fileVersion1);
        assertEquals(fileVersion2, fileVersion2);
        assertEquals(fileVersion1, fileVersion2);
        assertEquals(fileVersion2, fileVersion1);

        assertEquals(fileVersion1.hashCode(), fileVersion2.hashCode());
    }


    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {

        FileVersion fileVersion1 = new FileVersion() ;
        FileVersion fileVersion2 = new FileVersion() ;

        assertEquals(fileVersion1, fileVersion1);
        assertEquals(fileVersion2, fileVersion2);
        assertEquals(fileVersion1, fileVersion2);
        assertEquals(fileVersion2, fileVersion1);

        assertEquals(fileVersion1.hashCode(), fileVersion2.hashCode());
    }


}
