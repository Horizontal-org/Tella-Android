package org.hzontal.tella.keys.wrapper;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import org.hzontal.tella.keys.key.WrappedMainKey;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class AndroidKeyStoreHelper {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "TellaMainKeyWrapper";

    @RequiresApi(Build.VERSION_CODES.M)
    public static WrappedMainKey wrap(@NonNull byte[] input, IMainKeyWrapper wrapper) {
        SecretKey secretKey = hasKey() ? getKey() : createKey();

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] iv = cipher.getIV();
            byte[] data = cipher.doFinal(input);

            WrappedMainKey wrappedMainKey = new WrappedMainKey(wrapper.getName());
            wrappedMainKey.data = data;
            wrappedMainKey.iv = iv;

            return wrappedMainKey;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            throw new AssertionError(e);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public static byte[] unwrap(@NonNull WrappedMainKey wrappedMainKey) {
        SecretKey secretKey = getKey();

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, wrappedMainKey.iv));

            return cipher.doFinal(wrappedMainKey.data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new AssertionError(e);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private static SecretKey createKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(10)
                    .build();

            keyGenerator.init(keyGenParameterSpec);

            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private static SecretKey getKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                CertificateException | UnrecoverableEntryException e) {
            throw new AssertionError(e);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private static boolean hasKey() {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
            ks.load(null);

            return ks.containsAlias(KEY_ALIAS) && ks.entryInstanceOf(KEY_ALIAS, KeyStore.SecretKeyEntry.class);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new AssertionError(e);
        }
    }
}
