package org.hzontal.tella.keys.key;

import org.hzontal.tella.keys.util.Wiper;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class MainKey {
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE_BITS = 256;

    /**
     * We keep the raw key material here.
     * SecretKeySpec instances will be derived from this on demand.
     */
    private byte[] keyBytes;

    /**
     * Generate a new random AES key and wrap it as MainKey.
     */
    public static MainKey generate() {
        byte[] secret = generateSecret();
        return new MainKey(secret);
    }

    /**
     * Construct from an existing SecretKeySpec.
     * We copy out the encoded bytes so we control wiping.
     */
    public MainKey(SecretKeySpec encryptionKey) {
        if (encryptionKey == null) {
            throw new IllegalArgumentException("encryptionKey must not be null");
        }
        byte[] encoded = encryptionKey.getEncoded();
        if (encoded == null || encoded.length == 0) {
            throw new IllegalStateException("MainKey: provided SecretKeySpec has no encodable key material");
        }
        this.keyBytes = encoded;
    }

    /**
     * Construct directly from raw key bytes.
     */
    public MainKey(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length == 0) {
            throw new IllegalArgumentException("MainKey: keyBytes must not be null or empty");
        }
        // Keep a defensive copy in case caller mutates the array
        this.keyBytes = keyBytes.clone();
    }

    /**
     * Get a SecretKeySpec view of the key.
     * Returns null if the key has been wiped.
     */
    public SecretKeySpec getKey() {
        if (keyBytes == null) {
            return null;
        }
        // SecretKeySpec copies internally, so this is safe.
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    /**
     * Replace key with a new SecretKeySpec.
     */
    public void setKey(SecretKeySpec key) {
        if (key == null) {
            wipe();
            return;
        }
        byte[] encoded = key.getEncoded();
        if (encoded == null || encoded.length == 0) {
            throw new IllegalStateException("MainKey.setKey: provided SecretKeySpec has no encodable key material");
        }
        // Wipe previous key and replace
        wipe();
        this.keyBytes = encoded;
    }

    /**
     * Securely wipe the key material in memory.
     * After this call, getKey() will return null.
     */
    public void wipe() {
        Timber.d("*** MainKey.wipe");
        if (keyBytes != null) {
            Wiper.wipe(keyBytes);  // zero the bytes
            keyBytes = null;       // drop reference
        }
    }

    /**
     * Generate a random AES key (256-bit).
     * Crashes loudly if AES KeyGenerator is not available.
     */
    private static byte[] generateSecret() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(KEY_ALGORITHM);
            generator.init(KEY_SIZE_BITS);

            SecretKey secretKey = generator.generateKey();
            byte[] encoded = secretKey.getEncoded();

            if (encoded == null || encoded.length == 0) {
                IllegalStateException ex =
                        new IllegalStateException("Generated AES key is empty or null");
                Timber.e(ex, "MainKey.generateSecret: empty key returned by KeyGenerator");
                throw ex;
            }

            return encoded;
        } catch (NoSuchAlgorithmException ex) {
            Timber.e(ex, "MainKey.generateSecret: AES KeyGenerator not available");
            throw new IllegalStateException(
                    "Unable to generate AES key (algorithm " + KEY_ALGORITHM + " not available)", ex
            );
        }
    }
}
