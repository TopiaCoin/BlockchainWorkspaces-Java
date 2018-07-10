package io.topiacoin.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class FileVersionReceiptTest {

    @Test
    public void testDefaultConstructor() throws Exception {
        FileVersionReceipt fileVersionReceipt = new FileVersionReceipt();

        assertNull ( fileVersionReceipt.getEntryID()) ;
        assertNull( fileVersionReceipt.getVersionID());
        assertNull(fileVersionReceipt.getRecipientID());
        assertEquals(0, fileVersionReceipt.getDate());
    }


    @Test
    public void testConstructor() throws Exception {

        String entryID = "id-1";
        String versionID = "version-2";
        String recipientID = "KalmarWingfeather";
        long date = 987654321 ;

        FileVersionReceipt fileVersionReceipt = new FileVersionReceipt(entryID, versionID, recipientID, date);

        assertEquals (entryID, fileVersionReceipt.getEntryID()) ;
        assertEquals(versionID, fileVersionReceipt.getVersionID());
        assertEquals(recipientID, fileVersionReceipt.getRecipientID());
        assertEquals(date, fileVersionReceipt.getDate());
    }


    @Test
    public void testBasicAccessors() throws Exception {

        String entryID = "id-1";
        String versionID = "version-2";
        String recipientID = "KalmarWingfeather";
        long date = 987654321 ;

        FileVersionReceipt fileVersionReceipt = new FileVersionReceipt();


        assertNull ( fileVersionReceipt.getEntryID()) ;
        fileVersionReceipt.setEntryID(entryID);
        assertEquals (entryID, fileVersionReceipt.getEntryID()) ;
        fileVersionReceipt.setEntryID(null);
        assertNull ( fileVersionReceipt.getEntryID()) ;

        assertNull( fileVersionReceipt.getVersionID());
        fileVersionReceipt.setVersionID(versionID);
        assertEquals(versionID, fileVersionReceipt.getVersionID());
        fileVersionReceipt.setVersionID(null);
        assertNull( fileVersionReceipt.getVersionID());

        assertNull(fileVersionReceipt.getRecipientID());
        fileVersionReceipt.setRecipientID(recipientID);
        assertEquals(recipientID, fileVersionReceipt.getRecipientID());
        fileVersionReceipt.setRecipientID(null);
        assertNull(fileVersionReceipt.getRecipientID());

        assertEquals(0, fileVersionReceipt.getDate());
        fileVersionReceipt.setDate(date);
        assertEquals(date, fileVersionReceipt.getDate());
        fileVersionReceipt.setDate(0);
        assertEquals(0, fileVersionReceipt.getDate());

    }


    @Test
    public void testEqualsAndHashCode() throws Exception {

        String entryID = "id-1";
        String versionID = "version-2";
        String recipientID = "KalmarWingfeather";
        long date = 987654321 ;

        FileVersionReceipt fileVersionReceipt1 = new FileVersionReceipt(entryID, versionID, recipientID, date);
        FileVersionReceipt fileVersionReceipt2 = new FileVersionReceipt(entryID, versionID, recipientID, date);

        assertEquals(fileVersionReceipt1, fileVersionReceipt1);
        assertEquals(fileVersionReceipt2, fileVersionReceipt2);
        assertEquals(fileVersionReceipt1, fileVersionReceipt2);
        assertEquals(fileVersionReceipt2, fileVersionReceipt1);

        assertEquals(fileVersionReceipt1.hashCode(), fileVersionReceipt2.hashCode());
    }


    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {

        FileVersionReceipt fileVersionReceipt1 = new FileVersionReceipt();
        FileVersionReceipt fileVersionReceipt2 = new FileVersionReceipt();

        assertEquals(fileVersionReceipt1, fileVersionReceipt1);
        assertEquals(fileVersionReceipt2, fileVersionReceipt2);
        assertEquals(fileVersionReceipt1, fileVersionReceipt2);
        assertEquals(fileVersionReceipt2, fileVersionReceipt1);

        assertEquals(fileVersionReceipt1.hashCode(), fileVersionReceipt2.hashCode());
    }



}
