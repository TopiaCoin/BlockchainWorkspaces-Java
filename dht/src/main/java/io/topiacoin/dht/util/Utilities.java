package io.topiacoin.dht.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Base64;

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

    /**
     * Converts a Serializable Object to a String by serializing it and then encoding it.
     * Diametrically opposed to {@link #objectToString(Serializable)}
     * @param obj the Object to be serialized to a string
     * @return the String representation of the Object
     */
    public static String objectToString(Serializable obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Internal failure", e);
        } finally {
            try {
                baos.close();
                if(oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                //nop
            }
        }
    }

    /**
     * Converts a Serialized Object String to the Object. Enjoy blindly casting it all on your own.
     * Diametrically opposed to {@link #objectToString(Serializable)}
     * Play stupid games, win stupid prizes - pass something other than a serialized object in here and watch the world burn
     * @param s a Serialized Object String
     * @return the Object the String represents
     */
    public static Object objectFromString(String s) {
        byte[] data = Base64.getDecoder().decode(s);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bais);
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                bais.close();
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                //NOP
            }
        }
    }
}
