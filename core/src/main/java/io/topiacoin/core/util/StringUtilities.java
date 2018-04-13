package io.topiacoin.core.util;

public class StringUtilities {

    /**
     * Returns a byte array containing the bytes making up the string, or a zero-length byte array if the string is
     * null.
     *
     * @param string The string whose bytes are to be returned
     *
     * @return A byte array containing the bytes that make up the string, or a zero-length byte array if the string is
     * null.
     */
    public static byte[] getStringBytesOrEmptyArray(String string) {
        return (string != null ? string.getBytes() : new byte[0]);
    }
}
