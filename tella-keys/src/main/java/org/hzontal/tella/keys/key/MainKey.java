package org.hzontal.tella.keys.key;

import org.hzontal.tella.keys.util.Wiper;

import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class MainKey {
    private SecretKeySpec key;

    public static MainKey generate() {
        return new MainKey(new SecretKeySpec(generateSecret(), "AES"));
    }

    public MainKey(SecretKeySpec encryptionKey) {
        this.key = encryptionKey;
    }

    public SecretKeySpec getKey() {
        return key;
    }

    public void setKey(SecretKeySpec key) {
        this.key = key;
    }

    public void wipe() {
        Timber.d("*** MainKey.wipe");

        wipeKey();
    }

    private static byte[] generateSecret() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);

            SecretKey key = generator.generateKey();
            return key.getEncoded();
        } catch (NoSuchAlgorithmException ex) {

            return null;
        }
    }

    private void wipeKey() {
        try {
            Field field = SecretKeySpec.class.getDeclaredField("key");
            field.setAccessible(true);

            byte[] bytes = (byte[]) field.get(key);

            Wiper.wipe(bytes);
        } catch (Exception ignored) {}
    }
}
