package com.hzontal.tella_vault;

import com.hzontal.tella_vault.exceptions.LimitedInputStream;
import com.hzontal.utils.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class CipherStreamUtils {
    public static final int IV_SIZE = 16;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String transformation2 = "AES/CBC/NoPadding";
    private static final int HASH_BYTE_SIZE = 128;
    private static final int PBKDF2_ITERATIONS = 1000;

    public static InputStream getDecryptedLimitedInputStream(byte[] key, InputStream in, File file) throws IOException {
        try {
            IvParameterSpec iv = readIV(IV_SIZE, in);

            SecretKeySpec sks = createSecretKey(key, file.getName());
            Cipher cipher = Cipher.getInstance(transformation2);
            cipher.init(Cipher.DECRYPT_MODE, sks, iv);
            return new CipherInputStreamWrapper(new LimitedInputStream(in, file.length() - IV_SIZE), cipher);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidKeySpecException | InvalidAlgorithmParameterException e) {
            throw new IOException(e);
        }
    }

    public static InputStream getDecryptedInputStream(byte[] key, InputStream in, String filename) throws IOException {
        try {
            IvParameterSpec iv = readIV(IV_SIZE, in);

            SecretKeySpec sks = createSecretKey(key, filename);
            Cipher cipher = Cipher.getInstance(transformation2);
            cipher.init(Cipher.DECRYPT_MODE, sks, iv);
            return new CipherInputStream(in, cipher);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidKeySpecException | InvalidAlgorithmParameterException e) {
            throw new IOException(e);
        }
    }

    private static SecretKeySpec createSecretKey(byte[] key, String fileName) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        byte[] salt = fileName.getBytes(Charset.forName("UTF-8"));
        char[] password = new String(key, "UTF-8").toCharArray();

        KeySpec keySpec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey sk = factory.generateSecret(keySpec);

        return new SecretKeySpec(sk.getEncoded(), "AES");
    }

    public static OutputStream getEncryptedOutputStream(byte[] key, OutputStream out, String filename) throws IOException {
        try {
            SecretKeySpec sks = createSecretKey(key, filename);
            byte[] ivBytes = getIvBytes();

            Cipher cipher = Cipher.getInstance(transformation2);
            cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(ivBytes));

            out.write(ivBytes);

            return new CipherOutputStream(out, cipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidKeySpecException | InvalidAlgorithmParameterException e) {
            throw new IOException(e);
        }
    }

    private static IvParameterSpec readIV(final int ivSizeBytes, final InputStream is) throws IOException {
        final byte[] iv = new byte[ivSizeBytes];
        int offset = 0;

        while (offset < ivSizeBytes) {
            final int read = is.read(iv, offset, ivSizeBytes - offset);

            if (read == -1) {
                throw new IOException("Too few bytes for IV in input stream");
            }

            offset += read;
        }

        return new IvParameterSpec(iv);
    }

    private static byte[] getIvBytes() {
        byte[] ivBytes = new byte[IV_SIZE];
        secureRandom.nextBytes(ivBytes);
        return ivBytes;
    }

    private static class CipherInputStreamWrapper extends CipherInputStream {
        CipherInputStreamWrapper(InputStream is, Cipher c) {
            super(is, c);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } catch (Throwable t) {
                Timber.w(t);
            }
        }

        @Override
        public long skip(long skipAmount)
                throws IOException {
            long remaining = skipAmount;

            if (skipAmount <= 0) {
                return 0;
            }

            byte[] skipBuffer = new byte[4092];

            while (remaining > 0) {
                int read = super.read(skipBuffer, 0, Util.toIntExact(Math.min(skipBuffer.length, remaining)));

                if (read < 0) {
                    break;
                }

                remaining -= read;
            }

            return skipAmount - remaining;
        }
    }
}
