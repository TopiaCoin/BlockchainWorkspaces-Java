package io.topiacoin.dht.util;

import java.math.BigInteger;

public class Utilities {

    /**
     * Performs a bitwise XOR on two byte arrays, returning the result.  This method requires that both arrays be of the
     * same length.
     *
     * @param firstArray  The first array to be used in the XOR operation.
     * @param secondArray The second array to be used in the XOR operation.
     *
     * @return A new byte array containing the result of bitwise XORing the two arrays.
     *
     * @throws IllegalArgumentException If the two arrays are not the same length.
     */
    public static byte[] xorByteArrays(byte[] firstArray, byte[] secondArray) throws IllegalArgumentException {
        if (firstArray.length != secondArray.length)
            throw new IllegalArgumentException("Arrays must be of the same length");

        byte[] result = new byte[firstArray.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (firstArray[i] ^ secondArray[i]);
        }

        return result;
    }

    /**
     * Tests whether the specified data has the required number of leading zeros.
     *
     * @param data          The data that is being checked for leading zeros
     * @param requiredZeros The number of leading zeros required in the data.
     *
     * @return True if data has the required number of leading zeros.  False if the data does not have the required
     * number of leading zeros.
     */
    public static boolean hasSufficientZeros(byte[] data, int requiredZeros) {
        BigInteger intVersion = new BigInteger(1, data);
        BigInteger limit = BigInteger.valueOf(1L);
        limit = limit.shiftLeft((data.length * 8) - requiredZeros);

        return (intVersion.compareTo(limit) < 0);
    }

}
