package org.horizontal.tella.mobile.data.provider;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.hzontal.tella_vault.VaultException;
import com.hzontal.tella_vault.rx.RxVault;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import org.horizontal.tella.mobile.BuildConfig;
import org.horizontal.tella.mobile.MyApplication;

import timber.log.Timber;


public class EncryptedFileProvider extends FileProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + "." + "EncryptedFileProvider";
    public static final int IV_SIZE = 16;

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        ParcelFileDescriptor pfd = super.openFile(uri, mode);
        ParcelFileDescriptor[] pipe;

        try {
            pipe = ParcelFileDescriptor.createPipe();

            if ("r".equals(mode)) {


                new EncryptedFileProvider.ReadThread(uri.getLastPathSegment(),
                        new ParcelFileDescriptor.AutoCloseInputStream(pfd),
                        new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])).start();

                return pipe[0];
            }

            if ("w".equals(mode) || "wt".equals(mode)) {
                new EncryptedFileProvider.WriteThread(uri.getLastPathSegment(),
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

            try {
                try {
                    RxVault rxVault = MyApplication.keyRxVault.getRxVault().blockingFirst(); // ✅ blocking for sync use
                    cipherInputStream = rxVault.getStream(filename); // ✅ safe call with initialized vault
                } catch (VaultException | RuntimeException e) {
                    e.printStackTrace();
                    return;
                }

                while ((len = cipherInputStream.read(buf)) >= 0) {
                    out.write(buf, 0, len);
                }

                out.flush();

            } catch (IOException e) {
                Timber.e(e, getClass().getSimpleName());
                //FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                try {
                    if (cipherInputStream != null) cipherInputStream.close();
                    if (out != null) out.close();
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

            try {
                // Get RxVault synchronously
                RxVault rxVault = MyApplication.keyRxVault.getRxVault().blockingFirst();
                cos = rxVault.getOutStream(filename);

                while ((len = in.read(buf)) >= 0) {
                    cos.write(buf, 0, len);
                }

            } catch (IOException | VaultException | RuntimeException e) {
                Timber.e(e, getClass().getSimpleName());
                // FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                try {
                    if (in != null) in.close();
                    if (cos != null) cos.close();
                } catch (IOException e) {
                    Timber.e(e, getClass().getSimpleName());
                }
            }
        }

    }

}