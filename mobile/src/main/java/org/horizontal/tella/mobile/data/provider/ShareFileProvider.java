package org.horizontal.tella.mobile.data.provider;

import androidx.core.content.FileProvider;

import org.horizontal.tella.mobile.BuildConfig;

/**
 * Serves plaintext files from cache for sharing (e.g. a Signal zip of media + verification CSV).
 * Encrypted vault files stay on {@link EncryptedFileProvider}.
 */
public class ShareFileProvider extends FileProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".ShareFileProvider";
}
