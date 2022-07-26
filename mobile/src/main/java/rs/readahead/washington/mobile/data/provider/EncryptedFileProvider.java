package rs.readahead.washington.mobile.data.provider;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import org.hzontal.tella.keys.key.LifecycleMainKey;

import java.io.File;
import java.io.FileNotFoundException;
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

import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.util.LimitedInputStream;
import rs.readahead.washington.mobile.util.Util;
import timber.log.Timber;


public class EncryptedFileProvider extends FileProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + "." + "EncryptedFileProvider";
    public static final int IV_SIZE = 16;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String transformation2 = "AES/CTR/NoPadding";
    private static final int HASH_BYTE_SIZE = 128;
    private static final int PBKDF2_ITERATIONS = 100000;

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

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        ParcelFileDescriptor pfd = super.openFile(uri, mode);
        ParcelFileDescriptor[] pipe;

        try {
            pipe = ParcelFileDescriptor.createPipe();

            if ("r".equals(mode)) {
                new ReadThread(uri.getLastPathSegment(),
                        new ParcelFileDescriptor.AutoCloseInputStream(pfd),
                        new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])).start();

                return pipe[0];
            }

            if ("w".equals(mode) || "wt".equals(mode)) {
                new WriteThread(uri.getLastPathSegment(),
                        new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]),
                        new ParcelFileDescriptor.AutoCloseOutputStream(pfd)).start();

                return pipe[1];
            }
        } catch (Exception e) {
            Timber.e(e, getClass().getSimpleName());
            throw new FileNotFoundException("Could not open pipe for: " + uri.toString());
        }

        throw new IllegalArgumentException("Unsupported mode: " + mode);
    }

    private static class ReadThread extends Thread {
        String filename;
        InputStream in;
        OutputStream out;


        ReadThread(String filename, InputStream in, OutputStream out) {
            this.filename = filename;
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int len;
            InputStream cipherInputStream = null;
            byte[] key;
            try {
                if ((key = MyApplication.getMainKeyHolder().get().getKey().getEncoded()) == null) {
                    throw new SecurityException();
                }

                cipherInputStream = getDecryptedInputStream(key, in, filename); // todo: move to limited variant

                while ((len = cipherInputStream.read(buf)) >= 0) {
                    out.write(buf, 0, len);
                }

                out.flush();
            } catch (IOException | LifecycleMainKey.MainKeyUnavailableException e) {
                Timber.e(e, getClass().getSimpleName());
                //FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                try {
                    if (cipherInputStream != null) cipherInputStream.close();
                    out.close();
                } catch (IOException e) {
                    Timber.e(e, getClass().getSimpleName());
                }
            }
        }
    }

    private static class WriteThread extends Thread {
        String filename;
        InputStream in;
        OutputStream out;


        WriteThread(String filename, InputStream in, OutputStream out) {
            this.filename = filename;
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            int len;
            OutputStream cos = null;
            byte[] key;
            try {
                if ((key = MyApplication.getMainKeyHolder().get().getKey().getEncoded()) == null) {
                    throw new SecurityException();
                }
                cos = getEncryptedOutputStream(key, out, filename);

                while ((len = in.read(buf)) >= 0) {
                    cos.write(buf, 0, len);
                }
            } catch (IOException | LifecycleMainKey.MainKeyUnavailableException e) {
                Timber.e(e, getClass().getSimpleName());
                //FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                try {
                    in.close();
                    if (cos != null) cos.close();
                } catch (IOException e) {
                    Timber.e(e, getClass().getSimpleName());
                }
            }
        }
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
