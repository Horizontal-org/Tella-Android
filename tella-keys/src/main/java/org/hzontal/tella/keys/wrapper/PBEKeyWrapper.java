package org.hzontal.tella.keys.wrapper;

import org.hzontal.tella.keys.key.MainKey;
import org.hzontal.tella.keys.key.WrappedMainKey;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PBEKeyWrapper implements IMainKeyWrapper {
    @Override
    public String getName() {
        return PBEKeyWrapper.class.getName();
    }

    @Override
    public void wrap(MainKey mainKey, KeySpec keySpec, IWrapCallback callback) {
        try {
            PBEKeySpec pbeKeySpec = (PBEKeySpec) keySpec;

            byte[] salt = generateSalt();
            int iterationCount = calculateIterationCount();
            SecretKeySpec secretKeySpec = createAESKey(pbeKeySpec.getPassword(), salt, iterationCount);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            byte[] iv = cipher.getIV();
            byte[] data = cipher.doFinal(mainKey.getKey().getEncoded());

            WrappedMainKey wrappedMainKey = new WrappedMainKey(getName());
            wrappedMainKey.data = data;
            wrappedMainKey.iv = iv;
            wrappedMainKey.salt = salt;
            wrappedMainKey.iterationCount = iterationCount;

            callback.onReady(wrappedMainKey);

        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public void unwrap(WrappedMainKey wrapped, KeySpec keySpec, IUnwrapCallback callback) {
        try {
            PBEKeySpec pbeKeySpec = (PBEKeySpec) keySpec;

            SecretKeySpec secretKeySpec = createAESKey(pbeKeySpec.getPassword(),
                    wrapped.salt, wrapped.iterationCount);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(wrapped.iv));
            byte[] key = cipher.doFinal(wrapped.data);

            MainKey mainKey = new MainKey(new SecretKeySpec(key, "AES"));

            callback.onReady(mainKey);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

     SecretKeySpec createAESKey(char[] password, byte[] salt, int iterationCount)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec keySpec = new PBEKeySpec(password, salt, iterationCount, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(getAlgorithm());
        SecretKey sk = factory.generateSecret(keySpec);

        return new SecretKeySpec(sk.getEncoded(), "AES");
    }

    private String getAlgorithm() {
        return "PBKDF2withHmacSHA1";
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];

        random.nextBytes(salt);

        return salt;
    }

    private int calculateIterationCount() {
        // todo: implement this
        return getIterationCount();
    }

    private int getIterationCount() {
        return 10000;
    }
}
