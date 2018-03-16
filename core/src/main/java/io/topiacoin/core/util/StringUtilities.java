package io.topiacoin.core.util;

public class StringUtilities {

    public static byte[] getStringBytesOrEmptyArray(String string) {
        return ( string != null ? string.getBytes() : new byte[0] ) ;
    }
}
