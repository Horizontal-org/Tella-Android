package org.horizontal.tella.mobile.util;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.File;
import java.text.DecimalFormat;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.domain.entity.OldMediaFile;
import timber.log.Timber;


public class FileUtil {
    private static final String PRIMARY_VOLUME_NAME = "primary";

    public static boolean delete(final File file) {
        return Single.fromCallable(() -> {
            try {
                return file.delete();
            } catch (Exception e) {
                Timber.w(e, FileUtil.class.getName());
            }

            return false;
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    public static void emptyDir(final File path) {
        Completable.fromCallable(() -> {
            deletePathChildren(path);
            return null;
        }).subscribeOn(Schedulers.io()).blockingAwait();
    }

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            Timber.w(e, FileUtil.class.getName());
        }
    }

    @Nullable
    public static String getPrimaryMime(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return null;
        }
        //noinspection LoopStatementThatDoesntLoop
        for (String token : mimeType.split("/")) {
            return token.toLowerCase();
        }

        return null;
    }

    @Nullable
    public static String getMimeType(@NonNull String filename) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filename.toLowerCase());
        // Return the corresponding MIME type or null if unknown
        return extension != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) : null;
    }

  /*  public static MediaFile.Type getMediaFileType(@NonNull String filename) {
        String mimeType = getMimeType(filename);
        String primaryType = getPrimaryMime(mimeType);

        if ("image".equals(primaryType)) {
            return MediaFile.Type.IMAGE;
        }

        if ("audio".equals(primaryType)) {
            return MediaFile.Type.AUDIO;
        }

        if ("video".equals(primaryType)) {
            return MediaFile.Type.VIDEO;
        }

        return MediaFile.Type.UNKNOWN;
    }*/

    public static boolean mkdirs(File path) {
        return path.exists() || path.mkdirs();
    }

    @NonNull
    public static String getBaseName(@NonNull String filename) {
        int pos = filename.lastIndexOf(".");

        if (pos > 0) {
            return filename.substring(0, pos);
        }

        return filename;
    }

    @NonNull
    public static String getFileSizeString(long size) {
        String fileSize = "";
        double m = size / 1000000.0; //ne 1024 na 2
        double k = size / 1000.0; //ne 1024 na 2
        DecimalFormat dec = new DecimalFormat("0.00");

        if (m > 1) {
            fileSize = dec.format(m).concat(" MB");
        } else {
            fileSize = dec.format(k).concat(" KB");
        }

        return fileSize;
    }

    @NonNull
    public static String getFileSize(long size) {
        String fileSize ;
        double m = size / 1000000.0; //ne 1024 na 2
        double k = size / 1000.0; //ne 1024 na 2
        DecimalFormat dec = new DecimalFormat("0.00");

        if (m > 1) {
            fileSize = dec.format(m);
        } else {
            fileSize = dec.format(k);
        }

        return fileSize;
    }

    @NonNull
    public static String getUploadedFileSize(Float pct, Long size) {
        float uploadedSize = (pct * size);
        return getFileSizeString ((long)uploadedSize) +" / " + getFileSizeString (size);
    }

    private static boolean deletePath(File path) {
        if (path.isDirectory()) {
            File[] children = path.listFiles();

            if (children != null) {
                for (File child : children) {
                    deletePath(child);
                }
            }
        }

        return path.delete();
    }

    private static void deletePathChildren(File path) {
        if (path.isDirectory()) {
            File[] children = path.listFiles();

            if (children != null) {
                for (File child : children) {
                    deletePath(child);
                }
            }
        }
    }

    public static OldMediaFile.Type getOldMediaFileType(@NonNull String filename) {
        String mimeType = getMimeType(filename);
        String primaryType = getPrimaryMime(mimeType);

        if ("image".equals(primaryType)) {
            return OldMediaFile.Type.IMAGE;
        }

        if ("audio".equals(primaryType)) {
            return OldMediaFile.Type.AUDIO;
        }

        if ("video".equals(primaryType)) {
            return OldMediaFile.Type.VIDEO;
        }

        return OldMediaFile.Type.UNKNOWN;
    }
}
