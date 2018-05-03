package io.topiacoin.crypto;

import io.topiacoin.crypto.impl.ECAsymmetricCryptoProvider;
import io.topiacoin.crypto.impl.RSAAsymmetricCryptoProvider;
import io.topiacoin.crypto.impl.SecrataAsymmetricCryptoProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class CryptoUtils {

    private static final Log _log = LogFactory.getLog(CryptoUtils.class);

    private static final Map<String, Integer> blockSizes = new HashMap<String, Integer>();
    private static final Map<String, KeyPairGenerator> keyPairGenerators = new HashMap<String, KeyPairGenerator>();

    private static final SecureRandom secureRandom = new SecureRandom();


    // -------- Secret Key Generation Methods --------

    /**
     * Generates and returns a new AES key of the default size (128 bits).
     *
     * @return a newly generated AES key of the default size.
     */
    public static SecretKey generateAESKey() throws CryptographicException {
        return generateAESKey(128);
    }

    /**
     * Generates and returns a new AES of the specified size.  Valid sizes for AES keys are 128, 192, and 256.
     *
     * @param keyLength The desired length of the newly generated AES key.
     *
     * @return a newly generated AES key of the specified size.
     */
    public static SecretKey generateAESKey(int keyLength) throws CryptographicException {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(keyLength);
            return kgen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            _log.fatal("", e);
            throw new CryptographicException("Failed to create a new AES key", e);
        } catch (InvalidParameterException e) {
            _log.fatal("", e);
            throw new CryptographicException("Failed to create a new AES key", e);
        }
    }

    public static IvParameterSpec generateIV(String keyAlgorithm) throws CryptographicException {
        Integer size = blockSizes.get(keyAlgorithm);
        if (size == null) {
            synchronized (blockSizes) {
                try {
                    blockSizes.put(keyAlgorithm, Cipher.getInstance(keyAlgorithm).getBlockSize());
                    size = blockSizes.get(keyAlgorithm);
                } catch (NoSuchAlgorithmException e) {
                    _log.fatal("Failed to Generate Initialization Vector", e);
                    throw new CryptographicException(e);
                } catch (NoSuchPaddingException e) {
                    _log.fatal("", e);
                    throw new CryptographicException("Failed to Generate Initialization Vector", e);
                }
            }
        }
        byte[] ivBytes = new byte[size];
        secureRandom.nextBytes(ivBytes);
        return new IvParameterSpec(ivBytes);
    }

    public static KeyPair generateKeyPair(String algorithm) throws CryptographicException {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator keyPairGenerator = getKeyPairGenerator(algorithm);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Unrecognized Algorithm: " + algorithm);
        }

        return keyPair;
    }

    public static KeyPair generateECKeyPair() throws CryptographicException {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = getKeyPairGenerator("EC");
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Unrecognized Algorithm: EC");
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptographicException("Unrecognized Parameter Spec: secp256r1", e);
        }

        return keyPair;
    }

    public static KeyPair generateRSAKeyPair() throws CryptographicException {
        return generateKeyPair("RSA");
    }

    public static KeyPair generateDSAKeyPair() throws CryptographicException {
        return generateKeyPair("DSA");
    }

    public static KeyPair generateDHKeyPair() throws CryptographicException {
        return generateKeyPair("DH");
    }

    public static byte[] generateECDHSharedSecret(PrivateKey myPrivateKey, PublicKey theirPublicKey) throws CryptographicException {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(myPrivateKey);
            ka.doPhase(theirPublicKey, true);
            return ka.generateSecret();
        } catch (InvalidKeyException e) {
            throw new CryptographicException("Could not use keys to generate secret");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Unrecognized Algorithm: ECDH");
        }
    }

    public static PublicKey getPublicKeyFromEncodedBytes(String algorithm, byte[] publicKeyBytes) throws CryptographicException {
        if (algorithm == null) {
            throw new CryptographicException("Cannot get Public Key - algorithm null");
        }
        if (publicKeyBytes == null) {
            throw new CryptographicException("Cannot get Public Key - keydata null");
        }
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(publicKeyBytes);
            return kf.generatePublic(pkSpec);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Unrecognized Algorithm: " + algorithm);
        } catch (InvalidKeySpecException e) {
            throw new CryptographicException("Could not parse bytes into Public Key");
        }
    }

    public static PublicKey getECPublicKeyFromEncodedBytes(byte[] ecPublicKeyBytes) throws CryptographicException {
        return getPublicKeyFromEncodedBytes("EC", ecPublicKeyBytes);
    }


    // -------- Public Key Encryption Methods --------

    /**
     * Encrypts the inputData using the given Public Key.
     *
     * @param inputData The data to be encrypted
     * @param publicKey The public key with which the data is to be encrypted
     *
     * @return A byte array containing the encrypted data.
     */
    public static byte[] encryptWithPublicKey(byte[] inputData, PublicKey publicKey) throws CryptographicException {
        SecrataAsymmetricCryptoProvider asymmetricCryptoProvider = null;
        if (publicKey.getAlgorithm().equals("RSA")) {
            asymmetricCryptoProvider = RSAAsymmetricCryptoProvider.getInstance();
        } else if (publicKey.getAlgorithm().equals("EC")) {
            asymmetricCryptoProvider = ECAsymmetricCryptoProvider.getInstance();
        }
        return asymmetricCryptoProvider.encryptWithPublicKey(inputData, publicKey);
    }

    /**
     * Encrypts the inputString using the given Public Key.
     * <p>
     * This method is equivalent to: <code>encryptWithPublicKey(inputString.getBytes(), publicKey)</code>
     *
     * @param inputString The String to be encrypted
     * @param publicKey   The public key with which the data is to be encrypted
     *
     * @return A byte array containing the encrypted string.
     */
    public static byte[] encryptWithPublicKey(String inputString, PublicKey publicKey) throws CryptographicException {
        return encryptWithPublicKey(inputString.getBytes(), publicKey);
    }

    /**
     * Encrypts the inputData using the given Public Key.
     * <p>
     * This method is equivalent to: <code>Base64.toBase64String(encryptWithPublicKey(inputData, publicKey))</code>
     *
     * @param inputData The data to be encrypted
     * @param publicKey The public key with which the data is to be encrypted
     *
     * @return A String containing the Base64 encoded encrypted data.
     */
    public static String encryptWithPublicKeyToString(byte[] inputData, PublicKey publicKey) throws CryptographicException {
        return Base64.toBase64String(encryptWithPublicKey(inputData, publicKey));
    }

    /**
     * Encrypts the inputString using the given Public Key.
     * <p>
     * This method is equivalent to:  <code>Base64.toBase64String(encryptWithPublicKey(inputString.getBytes(),
     * publicKey))</code>
     *
     * @param inputString The string to be encrypted
     * @param publicKey   The public key with which the string is to be encrypted
     *
     * @return A String containing the Base64 encoded encrypted string.
     */
    public static String encryptWithPublicKeyToString(String inputString, PublicKey publicKey) throws CryptographicException {
        return encryptWithPublicKeyToString(inputString.getBytes(), publicKey);
    }


    // -------- Public Key Decryption Methods --------

    /**
     * Decrypts the encrypted data using the specified Private Key.
     *
     * @param encryptedData The data to be decrypted
     * @param privateKey    The private key to use when decrypting the data
     *
     * @return The decrypted data.
     *
     * @throws CryptographicException If there is an error while attempting to decrypt the encrypted data.
     */
    public static byte[] decryptWithPrivateKey(byte[] encryptedData, PrivateKey privateKey) throws CryptographicException {
        SecrataAsymmetricCryptoProvider asymmetricCryptoProvider = null;
        if (privateKey.getAlgorithm().equals("RSA")) {
            asymmetricCryptoProvider = RSAAsymmetricCryptoProvider.getInstance();
        } else if (privateKey.getAlgorithm().equals("EC")) {
            asymmetricCryptoProvider = ECAsymmetricCryptoProvider.getInstance();
        }
        return asymmetricCryptoProvider.decryptWithPrivateKey(encryptedData, privateKey);
    }

    /**
     * Decrypts the encrypted string using the specified Private Key.
     * <p>
     * This method is equivalent to: <code>decryptWithPrivateKey(Base64.decode(encryptedString), privateKey)</code>
     *
     * @param encryptedString The encrypted String to be decrypted
     * @param privateKey      The private key to use when decrypting the data
     *
     * @return The decrypted data.
     *
     * @throws CryptographicException If there is an error while attempting to decrypt the encrypted string.
     */
    public static byte[] decryptWithPrivateKey(String encryptedString, PrivateKey privateKey) throws CryptographicException {
        return decryptWithPrivateKey(Base64.decode(encryptedString), privateKey);
    }


    // -------- Secret Key Data Encryption Methods --------

    /**
     * Encrypts the input data using the specified secret key and initialization vector specification.  If no
     * initialization vector information is provided, the data will be encrypted using standard ECB mode instead of CBC
     * mode.
     *
     * @param inputData       The data to be encrypted
     * @param secretKey       The encryption key to use when encrypting the data.
     * @param ivParameterSpec The initialization vector to use when encrypting the data.
     *
     * @return a byte array containing the encrypted data.
     *
     * @throws CryptographicException If there is an error while attempting to encrypt the data.
     */
    public static byte[] encryptWithSecretKey(byte[] inputData, int length, SecretKey secretKey, IvParameterSpec ivParameterSpec) throws CryptographicException {
        try {
            String algorithm = secretKey.getAlgorithm();
            Cipher cipher;
            if (ivParameterSpec != null) {
                algorithm = secretKey.getAlgorithm() + "/CBC/PKCS5Padding";
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            } else {
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            }

            return cipher.doFinal(inputData);
        } catch (NoSuchPaddingException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (InvalidKeyException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (BadPaddingException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (IllegalBlockSizeException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        }
    }

    public static byte[] encryptWithSecretKey(byte[] inputData, SecretKey secretKey, IvParameterSpec ivParameterSpec) throws CryptographicException {
        return encryptWithSecretKey(inputData, inputData.length, secretKey, ivParameterSpec);
    }

    /**
     * Encrypts the input data using the specified secret key and initialization vector specification.
     * <p>
     * This method is equivalent to: <code>encryptWithSecretKey(inputData, secretKey, null)</code>
     *
     * @param inputData The data to be encrypted
     * @param secretKey The encryption key to use when encrypting the data.
     *
     * @return a byte array containing the encrypted data.
     *
     * @throws CryptographicException If there is an error while attempting to encrypt the data.
     */
    public static byte[] encryptWithSecretKey(byte[] inputData, SecretKey secretKey) throws CryptographicException {
        return encryptWithSecretKey(inputData, inputData.length, secretKey, null);
    }

    /**
     * Encrypts the data in the specified InputStream using the specified secret key and initialization vector
     * specification and writes it to the provided OutputStream.  The InputStream and OutputStream will remain open
     * after this operation completes.
     *
     * @param inStream        The data stream to be encrypted
     * @param outStream       The data stream to which the encrypted data is to be written
     * @param secretKey       The encryption key to use when encrypting the data.
     * @param ivParameterSpec The initialization vector to use when encrypting the data.
     *
     * @throws CryptographicException If there is an error while attempting to encrypt the data stream.
     */
    public static void encryptWithSecretKey(InputStream inStream, OutputStream outStream, SecretKey secretKey, IvParameterSpec ivParameterSpec) throws CryptographicException {
        try {
            String algorithm = secretKey.getAlgorithm();
            Cipher cipher;
            if (ivParameterSpec != null) {
                algorithm = secretKey.getAlgorithm() + "/CBC/PKCS5Padding";
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            } else {
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            }

            // Put a custom Filter Output Stream between the Cipher Stream below and the
            // actual Output Stream. This is done so that the required close() of the
            // Cipher Output Stream, which is needed to write the final block, doesn't
            // also close the underlying stream, which may still need to have data written
            // to it.
            FilterOutputStream fos = new FilterOutputStream(outStream) {
                @Override
                public void close() throws IOException {
                    // Do not pass the close operation to the underlying stream.
                }
            };

            CipherOutputStream cipherOutStream = new CipherOutputStream(fos, cipher);
            copyStreamToStream(inStream, cipherOutStream);
            cipherOutStream.close();
        } catch (NoSuchPaddingException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (InvalidKeyException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (IOException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        }
    }

    /**
     * Encrypts the data in the specified InputStream using the specified secret key and initialization vector
     * specification and writes it to the provided OutputStream.
     * <p>
     * This call is equivalent to: <code>encryptWithSecretKey(inStream, outStream, secretKey, null)</code>
     *
     * @param inStream  The data stream to be encrypted
     * @param outStream The data stream to which the encrypted data is to be written
     * @param secretKey The encryption key to use when encrypting the data.
     *
     * @throws CryptographicException If there is an error while attempting to encrypt the data stream.
     */
    public static void encryptWithSecretKey(InputStream inStream, OutputStream outStream, SecretKey secretKey) throws CryptographicException {
        encryptWithSecretKey(inStream, outStream, secretKey, null);
    }


    // -------- Secret Key Data Decryption Methods --------

    /**
     * Decrypts the encrypted data using the specified secret key and initialization vector specification. If no
     * initialization vector is specified, the data is decrypted in ECB mode instead of CBC mode.
     *
     * @param encryptedData   The data to be decrypted
     * @param secretKey       The encryption key to use when decrypting the data
     * @param ivParameterSpec The initialization vector to use when decrypting the data.
     *
     * @return A byte array containing the decrypted data.
     *
     * @throws CryptographicException If there is an error while attempting to decrypt the data.
     */
    public static byte[] decryptWithSecretKey(byte[] encryptedData, SecretKey secretKey, IvParameterSpec ivParameterSpec) throws CryptographicException {
        try {
            String algorithm = secretKey.getAlgorithm();
            Cipher cipher;
            if (ivParameterSpec != null) {
                algorithm = secretKey.getAlgorithm() + "/CBC/PKCS5Padding";
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            } else {
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            }
            byte[] decryptedData = cipher.doFinal(encryptedData);

            return decryptedData;
        } catch (NoSuchPaddingException e) {
            throw new CryptographicException("Failed to decrypt with secret key", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Failed to decrypt with secret key", e);
        } catch (InvalidKeyException e) {
            throw new CryptographicException("Failed to decrypt with secret key", e);
        } catch (BadPaddingException e) {
            throw new CryptographicException("Failed to decrypt with secret key", e);
        } catch (IllegalBlockSizeException e) {
            throw new CryptographicException("Failed to decrypt with secret key", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptographicException("Failed to decrypt with secret key", e);
        }
    }

    /**
     * Decrypts the encrypted data using the specified secret key and initialization vector specification.
     * <p>
     * This method is equivalent to: <code>decryptWithSecretKey(encryptedData, secretKey, null)</code>
     *
     * @param encryptedData The data to be decrypted
     * @param secretKey     The encryption key to use when decrypting the data
     *
     * @return A byte array containing the decrypted data.
     *
     * @throws CryptographicException If there is an error while attempting to decrypt the data.
     */
    public static byte[] decryptWithSecretKey(byte[] encryptedData, SecretKey secretKey) throws CryptographicException {
        return decryptWithSecretKey(encryptedData, secretKey, null);
    }

    /**
     * Decrypts the data in the specified InputStream using the specified secret key and initialization vector
     * specification and writes it to the provided OutputStream.  The InputStream and OutputStream will remain open
     * after this operation completes.
     *
     * @param inStream        The data stream to be decrypted
     * @param outStream       The data stream to which the decrypted data is to be written
     * @param secretKey       The encryption key to use when decrypting the data.
     * @param ivParameterSpec The initialization vector to use when decrypting the data.
     *
     * @throws CryptographicException If there is an error while attempting to decrypt the data stream.
     */
    public static void decryptWithSecretKey(InputStream inStream, OutputStream outStream, SecretKey secretKey, IvParameterSpec ivParameterSpec) throws CryptographicException {
        try {
            String algorithm = secretKey.getAlgorithm();
            Cipher cipher;
            if (ivParameterSpec != null) {
                algorithm = secretKey.getAlgorithm() + "/CBC/PKCS5Padding";
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            } else {
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            }

            CipherInputStream cipherInStream = new CipherInputStream(inStream, cipher);
            copyStreamToStream(cipherInStream, outStream);
        } catch (NoSuchPaddingException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (InvalidKeyException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        } catch (IOException e) {
            throw new CryptographicException("Failed to encrypt with secret key", e);
        }
    }

    /**
     * Decrypts the data in the specified InputStream using the specified secret key and initialization vector
     * specification and writes it to the provided OutputStream.
     * <p>
     * This method is equivalent to: <code>decryptWithSecretKey(inStream, outStream, secretKey, null)</code>
     *
     * @param inStream  The data stream to be decrypted
     * @param outStream The data stream to which the decrypted data is to be written
     * @param secretKey The encryption key to use when decrypting the data.
     *
     * @throws CryptographicException If there is an error while attempting to decrypt the data stream.
     */
    public static void decryptWithSecretKey(InputStream inStream, OutputStream outStream, SecretKey secretKey) throws CryptographicException {
        decryptWithSecretKey(inStream, outStream, secretKey, null);
    }


    // -------- Secret Key String Encryption/Decryption Methods --------

    /**
     * Encrypts the input string using the specified secret key.  A random initialization vector will be used to protect
     * the encrypted data.  The returned string will have the initialization vector encoded into it along with a prefix
     * indicating that the string is encrypted.
     *
     * @param inputString The string to be encrypted.
     * @param secretKey   The key to use to encrypt the input string.
     *
     * @return A String containing a Base64 encoded representation of the initialization vector and encrypted input
     * string along with a prefix indicating that the string contains encrypted data.
     *
     * @throws CryptographicException If there is an error while attempting to encrypt the data.
     */
    public static String encryptStringWithSecretKey(String inputString, SecretKey secretKey) throws CryptographicException {
        if (secretKey == null) {
            _log.debug("workspaceKey is null. Can't encrypt.");
            return inputString;
        }

        String ret;
        String cipherAlgorithm = secretKey.getAlgorithm() + "/CBC/PKCS5Padding";

        // Generate the Initialization Vector
        IvParameterSpec iv = generateIV(secretKey.getAlgorithm());

        // Encrypt the value
        byte[] cipherText = encryptWithSecretKey(inputString.getBytes(), secretKey, iv);

        // Combine the IV and the encrypted value
        byte[] toBeEncoded = new byte[iv.getIV().length + cipherText.length];
        System.arraycopy(iv.getIV(), 0, toBeEncoded, 0, iv.getIV().length);
        System.arraycopy(cipherText, 0, toBeEncoded, iv.getIV().length, cipherText.length);

        // Base64 encode the combined value
        String base64String = Base64.toBase64String(toBeEncoded);

        // Build the return string
        ret = "ENC:" + base64String;

        return ret;
    }

    /**
     * Decrypt the encrypted inputString using the specified secret key.  If an unencrypted string is passed in to this
     * method, it will be returned unchanged from the method.  Encrypted strings are identified by the standard prefix
     * placed on it by the encryptStringWithSecretKey method.
     *
     * @param inputString The encrypted string to be decrypted.
     * @param secretKey   The encryption key to use when decrypting the inputString
     *
     * @return The decrypted value of the inputString, or the original inputString if it wasn't encrypted.
     */
    public static String decryptStringWithSecretKey(String inputString, SecretKey secretKey) throws CryptographicException {
        try {
            if (!inputString.startsWith("ENC:")) {
                _log.warn("'" + inputString + "' doesn't appear to be an encrypted string. Won't decrypt.");
                return inputString;
            }

            if (secretKey == null) {
                _log.warn("secretKey is null. Can't decrypt.");
                return null;
            }

            inputString = inputString.substring(("ENC:").length());

            String cipherAlgorithm = secretKey.getAlgorithm()
                    + "/CBC/PKCS5Padding";

            // Base64 Decode the incoming String
            byte[] toBeDecrypted = Base64.decode(inputString);

            // Setup the Cipher
            byte[] iv = new byte[Cipher.getInstance(cipherAlgorithm).getBlockSize()];
            System.arraycopy(toBeDecrypted, 0, iv, 0, iv.length);

            // Extract the encrypted data from the decoded string
            byte[] encData = new byte[toBeDecrypted.length - iv.length];
            System.arraycopy(toBeDecrypted, iv.length, encData, 0, encData.length);

            // Initialize the Cipher
            byte[] clearData = decryptWithSecretKey(encData, secretKey, new IvParameterSpec(iv));

            return new String(clearData, "UTF-8");
        } catch (NoSuchAlgorithmException ex) {
            throw new CryptographicException("Failed to decrypt String with secret key", ex);
        } catch (NoSuchPaddingException ex) {
            throw new CryptographicException("Failed to decrypt String with secret key", ex);
        } catch (UnsupportedEncodingException ex) {
            throw new CryptographicException("Failed to decrypt String with secret key", ex);
        }
    }


    // -------- Private Methods --------

    private static KeyPairGenerator getKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = keyPairGenerators.get(algorithm);
        if (kpg == null) {
            kpg = KeyPairGenerator.getInstance(algorithm);
            keyPairGenerators.put(algorithm, kpg);
        }

        return kpg;
    }


    private static int copyStreamToStream(InputStream inStream, OutputStream cos) throws IOException {
        // Copy the bytes from the input stream to the output stream.
        byte[] buffer = new byte[8192];
        int totalBytesCopied = 0;
        int bytesRead = 0;
        while ((bytesRead = inStream.read(buffer)) >= 0) {
            cos.write(buffer, 0, bytesRead);
            totalBytesCopied += bytesRead;
        }
        return totalBytesCopied;
    }

    private static int copyStreamToStream(InputStream inStream, OutputStream cos, int length) throws IOException {
        // Copy the bytes from the input stream to the output stream.
        byte[] buffer = new byte[8192];
        int totalBytesCopied = 0;
        int bytesRead = 0;
        while ((totalBytesCopied < length) && (bytesRead = inStream.read(buffer)) >= 0) {
            cos.write(buffer, 0, bytesRead);
            totalBytesCopied += bytesRead;
        }

        return totalBytesCopied;
    }
}
