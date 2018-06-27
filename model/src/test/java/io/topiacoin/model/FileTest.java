package io.topiacoin.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FileTest {

    @Test
    public void testDefaultConstructor() throws Exception {

        File file = new File();

        assertNull(file.getName());
        assertNull(file.getMimeType());
        assertNull(file.getEntryID());
        assertNull(file.getParentID());
        assertFalse(file.isFolder()) ;
        assertEquals ( 0, file.getStatus());
        assertNull(file.getLockOwner());
        assertNotNull(file.getVersions());
    }

    @Test
    public void testConstructor() throws Exception {

        String name = "MrMagoo";
        String mimeType = "person/dottering";
        String entryID = "1234567";
        long containerID = 45678L;
        String parentID = "feed";
        boolean isFolder = true;
        int status = 765;
        String lockOwner = "IAmTheOwner";
        List<FileVersion> versions = new ArrayList<FileVersion>();

        File file = new File(name, mimeType, entryID, containerID, parentID, isFolder, status, lockOwner, versions);

        assertEquals(name, file.getName());
        assertEquals(mimeType, file.getMimeType());
        assertEquals(entryID, file.getEntryID());
        assertEquals(containerID, file.getContainerID());
        assertEquals(parentID, file.getParentID());
        assertEquals(isFolder, file.isFolder());
        assertEquals(status, file.getStatus());
        assertEquals(lockOwner, file.getLockOwner());
        assertEquals(versions, file.getVersions());
    }

    @Test
    public void testConstructorWithNullVersions() throws Exception {

        String name = "MrMagoo";
        String mimeType = "person/dottering";
        String entryID = "1234567";
        long containerID = 45678L;
        String parentID = "feed";
        boolean isFolder = true;
        int status = 765;
        String lockOwner = "IAmTheOwner";
        List<FileVersion> versions = null;

        File file = new File(name, mimeType, entryID, containerID, parentID, isFolder, status, lockOwner, versions);

        assertEquals(name, file.getName());
        assertEquals(mimeType, file.getMimeType());
        assertEquals(entryID, file.getEntryID());
        assertEquals(containerID, file.getContainerID());
        assertEquals(parentID, file.getParentID());
        assertEquals(isFolder, file.isFolder());
        assertEquals(status, file.getStatus());
        assertEquals(lockOwner, file.getLockOwner());

        assertNotNull(file.getVersions()) ;
        assertEquals (0, file.getVersions().size()) ;
    }

    @Test
    public void testBasicAccessors() throws Exception {

        String name = "MrMagoo";
        String mimeType = "person/dottering";
        String entryID = "1234567";
        long containerID = 45678L;
        String parentID = "feed";
        int status = 765;
        String lockOwner = "IAmTheOwner";

        File file = new File() ;

        // Check the Name accessor
        assertNull ( file.getName()) ;
        file.setName(name);
        assertEquals ( name, file.getName()) ;
        file.setName(null);
        assertNull (file.getName());

        // Check the MimeType Accessor
        assertNull(file.getMimeType());
        file.setMimeType(mimeType);
        assertEquals(mimeType, file.getMimeType());
        file.setMimeType(null);
        assertNull(file.getMimeType());

        // Check the Entry ID Accessor
        assertNull(file.getEntryID());
        file.setEntryID(entryID);
        assertEquals(entryID, file.getEntryID());
        file.setEntryID(null);
        assertNull(file.getEntryID());

        // Check the Container ID Accessor
        file.setContainerID(containerID);
        assertEquals(containerID, file.getContainerID());

        // Check the Parent ID Accessor
        assertNull(file.getParentID());
        file.setParentID(parentID);
        assertEquals(parentID, file.getParentID());
        file.setParentID(null);
        assertNull(file.getParentID());

        // Check the isFolder Accessor
        assertFalse(file.isFolder()) ;
        file.setFolder(true);
        assertTrue(file.isFolder()) ;
        file.setFolder(false);
        assertFalse(file.isFolder()) ;

        // Check the Status Accessor
        assertEquals ( 0, file.getStatus());
        file.setStatus(status);
        assertEquals(status, file.getStatus());
        file.setStatus(0);
        assertEquals ( 0, file.getStatus());

        // Check the Lock Owner Accessor
        assertNull(file.getLockOwner());
        file.setLockOwner(lockOwner);
        assertEquals(lockOwner, file.getLockOwner());
        file.setLockOwner(null);
        assertNull(file.getLockOwner());
    }

    @Test
    public void testCollectionAccessors() throws Exception {

        List<FileVersion> versions = new ArrayList<FileVersion>() ;
        FileVersion version1 = new FileVersion() ;
        FileVersion version2 = new FileVersion() ;
        versions.add(version1) ;
        versions.add(version2) ;

        File file = new File() ;

        // Check the Version Collection Accessor
        assertNotNull(file.getVersions());
        assertEquals(0, file.getVersions().size());
        file.setVersions(versions);
        assertNotNull ( file.getVersions()) ;
        assertNotSame(versions, file.getVersions());
        assertEquals(versions.size(), file.getVersions().size());
        assertEquals ( versions, file.getVersions()) ;
        file.setVersions(null);
        assertNotNull(file.getVersions());
        assertNotSame(versions, file.getVersions());
        assertEquals(0, file.getVersions().size());

    }

    @Test
    public void testEqualsAndHashCode() throws Exception {

        String name = "MrMagoo";
        String mimeType = "person/dottering";
        String entryID = "1234567";
        long containerID = 45678L;
        String parentID = "feed";
        boolean isFolder = true;
        int status = 765;
        String lockOwner = "IAmTheOwner";
        List<FileVersion> versions = new ArrayList<FileVersion>();

        File file1 = new File(name, mimeType, entryID, containerID, parentID, isFolder, status, lockOwner, versions);
        File file2 = new File(name, mimeType, entryID, containerID, parentID, isFolder, status, lockOwner, versions);

        assertEquals(file1, file1);
        assertEquals(file2, file2);
        assertEquals(file1, file2);
        assertEquals(file2, file1);

        assertEquals(file1.hashCode(), file2.hashCode());

    }

    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {

        File file1 = new File();
        File file2 = new File();

        assertEquals(file1, file1);
        assertEquals(file2, file2);
        assertEquals(file1, file2);

        assertEquals(file1.hashCode(), file2.hashCode());

    }
}
