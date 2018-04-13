package io.topiacoin.crypto;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.junit.Assert.*;

public class HashUtilsTest {

    @Test
    public void testSha1() throws Exception {
        String inputString = "I am the very model of a modern major general";
        byte[] inputBytes = inputString.getBytes() ;
        byte[] expectedHash = new byte[] {
                (byte)0xe8, (byte)0xc3, (byte)0xd1, (byte)0x96, (byte)0x70, (byte)0x5e, (byte)0x3c, (byte)0xec,
                (byte)0x74, (byte)0x27, (byte)0x09, (byte)0x6f, (byte)0xbe, (byte)0xfe, (byte)0x5e, (byte)0x8f,
                (byte)0x07, (byte)0x38, (byte)0xf7, (byte)0x65 } ;
        String expectedHashString = Hex.toHexString(expectedHash) ;

        byte[] bytesHashBytes = HashUtils.sha1(inputBytes) ;
        byte[] stringHashBytes = HashUtils.sha1(inputString) ;
        String bytesHashString = HashUtils.sha1String(inputBytes) ;
        String stringHashString = HashUtils.sha1String(inputString) ;

        assertArrayEquals ( expectedHash, bytesHashBytes) ;
        assertArrayEquals(expectedHash, stringHashBytes);
        assertEquals ( expectedHashString, bytesHashString) ;
        assertEquals(expectedHashString, stringHashString);
    }

    @Test
    public void testSha256() throws Exception {
        String inputString = "I am the very model of a modern major general";
        byte[] inputBytes = inputString.getBytes() ;
        byte[] expectedHash = new byte[] {
                (byte)0xff, (byte)0x51, (byte)0xe3, (byte)0xd4, (byte)0xcc, (byte)0xd0, (byte)0x42, (byte)0xd2,
                (byte)0xd4, (byte)0x97, (byte)0x2a, (byte)0x07, (byte)0xe0, (byte)0x76, (byte)0xf5, (byte)0xbf,
                (byte)0x78, (byte)0x0a, (byte)0xd3, (byte)0x49, (byte)0x2c, (byte)0x7d, (byte)0xf0, (byte)0xf8,
                (byte)0xaa, (byte)0x22, (byte)0xf2, (byte)0x10, (byte)0xee, (byte)0x7b, (byte)0x5b, (byte)0x91 } ;
        String expectedHashString = Hex.toHexString(expectedHash) ;

        byte[] bytesHashBytes = HashUtils.sha256(inputBytes) ;
        byte[] stringHashBytes = HashUtils.sha256(inputString) ;
        String bytesHashString = HashUtils.sha256String(inputBytes) ;
        String stringHashString = HashUtils.sha256String(inputString) ;

        assertArrayEquals ( expectedHash, bytesHashBytes) ;
        assertArrayEquals(expectedHash, stringHashBytes);
        assertEquals ( expectedHashString, bytesHashString) ;
        assertEquals(expectedHashString, stringHashString);
    }


    //28740eb10cbe00c2650143b582529b457df428ed34c86203c025192ef2045b2e

    @Test
    public void testSha3() throws Exception {
        String inputString = "I am the very model of a modern major general";
        byte[] inputBytes = inputString.getBytes() ;
        byte[] expectedHash = new byte[] {
                (byte)0x28, (byte)0x74, (byte)0x0e, (byte)0xb1, (byte)0x0c, (byte)0xbe, (byte)0x00, (byte)0xc2,
                (byte)0x65, (byte)0x01, (byte)0x43, (byte)0xb5, (byte)0x82, (byte)0x52, (byte)0x9b, (byte)0x45,
                (byte)0x7d, (byte)0xf4, (byte)0x28, (byte)0xed, (byte)0x34, (byte)0xc8, (byte)0x62, (byte)0x03,
                (byte)0xc0, (byte)0x25, (byte)0x19, (byte)0x2e, (byte)0xf2, (byte)0x04, (byte)0x5b, (byte)0x2e } ;
        String expectedHashString = Hex.toHexString(expectedHash) ;

        byte[] bytesHashBytes = HashUtils.sha3(inputBytes) ;
        byte[] stringHashBytes = HashUtils.sha3(inputString) ;
        String bytesHashString = HashUtils.sha3String(inputBytes) ;
        String stringHashString = HashUtils.sha3String(inputString) ;

        assertArrayEquals ( expectedHash, bytesHashBytes) ;
        assertArrayEquals(expectedHash, stringHashBytes);
        assertEquals ( expectedHashString, bytesHashString) ;
        assertEquals(expectedHashString, stringHashString);
    }
}
