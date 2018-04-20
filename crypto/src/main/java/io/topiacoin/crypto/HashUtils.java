package io.topiacoin.crypto;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    /**
     * Returns the SHA-1 hash of the input bytes.  The hash is returned as a byte array.
     *
     * @param input The byte array whose content is to be hashed.
     *
     * @return A byte array containing the SHA-1 hash of the input byte array.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-1 hashing.
     */
    public static byte[] sha1(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        return sha1.digest(input);
    }

    /**
     * Returns the SHA-1 hash of the input string.  The hash is calculated over the bytes that make up the string and is
     * returned as a byte array.
     *
     * @param input The string whose hash is to be calculated.
     *
     * @return A byte array containing the SHA-1 hash of the input string.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-1 hashing.
     */
    public static  byte[] sha1(String input) throws NoSuchAlgorithmException {
        return sha1(input.getBytes());
    }

    /**
     * Returns the Hexadecimal String containing the SHA-1 Hash of the input bytes.
     *
     * @param input The byte array whose content is to be hashed.
     *
     * @return A String containing the hash of the input bytes in hexadecimal.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-1 hashing.
     */
    public static  String sha1String(byte[] input) throws NoSuchAlgorithmException {
        return Hex.toHexString(sha1(input));
    }

    /**
     * Returns the Hexadecimal String containing the SHA-1 Hash of the input bytes. The hash is calculated over the
     * bytes that make up the string.
     *
     * @param input The String whose whose hash is to be calculated
     *
     * @return A String containing the hash of the input bytes in hexadecimal.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-1 hashing.
     */
    public static  String sha1String(String input) throws NoSuchAlgorithmException {
        return sha1String(input.getBytes());
    }

    /**
     * Returns the SHA-256 hash of the input bytes.  The hash is returned as a byte array.
     *
     * @param inputs The byte arrays whose content is to be hashed.
     *
     * @return A byte array containing the SHA-256 hash of the input byte arrays.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-256 hashing.
     */
    public static byte[] sha256(byte[]... inputs) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        for(byte[] input : inputs) {
            sha256.update(input);
        }
        return sha256.digest();
    }

    /**
     * Returns the SHA-256 hash of the input string.  The hash is calculated over the bytes that make up the string and
     * is returned as a byte array.
     *
     * @param input The string whose hash is to be calculated.
     *
     * @return A byte array containing the SHA-256 hash of the input string.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-256 hashing.
     */
    public static byte[] sha256(String input) throws NoSuchAlgorithmException {
        return sha256(input.getBytes());
    }

    /**
     * Returns the Hexadecimal String containing the SHA-256 Hash of the input bytes.
     *
     * @param input The byte array whose content is to be hashed.
     *
     * @return A String containing the hash of the input bytes in hexadecimal.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-256 hashing.
     */
    public static  String sha256String(byte[] input) throws NoSuchAlgorithmException {
        return Hex.toHexString(sha256(input));
    }

    /**
     * Returns the Hexadecimal String containing the SHA-256 Hash of the input bytes. The hash is calculated over the
     * bytes that make up the string.
     *
     * @param input The String whose whose hash is to be calculated
     *
     * @return A String containing the hash of the input bytes in hexadecimal.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-256 hashing.
     */
    public static  String sha256String(String input) throws NoSuchAlgorithmException {
        return sha256String(input.getBytes());
    }

    /**
     * Returns the SHA-3 hash of the input bytes.  The hash is returned as a byte array.
     *
     * @param input The byte array whose content is to be hashed.
     *
     * @return A byte array containing the SHA-3 hash of the input byte array.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-3 hashing.
     */
    public static  byte[] sha3(byte[] input) throws NoSuchAlgorithmException {
        SHA3Digest digest = new SHA3Digest(256);
        byte[] hash = new byte[digest.getDigestSize()];

        if (input.length != 0) {
            digest.update(input, 0, input.length);
        }
        digest.doFinal(hash, 0);
        return hash;
    }

    /**
     * Returns the SHA-3 hash of the input string.  The hash is calculated over the bytes that make up the string and is
     * returned as a byte array.
     *
     * @param input The string whose hash is to be calculated.
     *
     * @return A byte array containing the SHA-3 hash of the input string.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-3 hashing.
     */
    public static  byte[] sha3(String input) throws NoSuchAlgorithmException {
        return sha3(input.getBytes());
    }

    /**
     * Returns the Hexadecimal String containing the SHA-3 Hash of the input bytes.
     *
     * @param input The byte array whose content is to be hashed.
     *
     * @return A String containing the hash of the input bytes in hexadecimal.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-3 hashing.
     */
    public static  String sha3String(byte[] input) throws NoSuchAlgorithmException {
        return Hex.toHexString(sha3(input));
    }

    /**
     * Returns the Hexadecimal String containing the SHA-3 Hash of the input bytes. The hash is calculated over the
     * bytes that make up the string.
     *
     * @param input The String whose whose hash is to be calculated
     *
     * @return A String containing the hash of the input bytes in hexadecimal.
     *
     * @throws NoSuchAlgorithmException If the platform does not support SHA-3 hashing.
     */
    public static  String sha3String(String input) throws NoSuchAlgorithmException {
        return sha3String(input.getBytes());
    }
}
