package rs.readahead.washington.mobile.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.domain.entity.OldMediaFile;
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
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(filename.toLowerCase())
        );
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
    @Nullable
    public static String getFullPathFromTreeUri(@Nullable final Uri treeUri, Context con) {
        if (treeUri == null) return null;
        String volumePath = getVolumePath(getVolumeIdFromTreeUri(treeUri),con);
        if (volumePath == null) return File.separator;
        if (volumePath.endsWith(File.separator))
            volumePath = volumePath.substring(0, volumePath.length() - 1);

        String documentPath = getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator))
            documentPath = documentPath.substring(0, documentPath.length() - 1);

        if (documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator))
                return volumePath + documentPath;
            else
                return volumePath + File.separator + documentPath;
        }
        else return volumePath;
    }


    private static String getVolumePath(final String volumeId, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return getVolumePathForAndroid11AndAbove(volumeId, context);
        else
            return getVolumePathBeforeAndroid11(volumeId, context);
    }


    private static String getVolumePathBeforeAndroid11(final String volumeId, Context context){
        try {
            StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId))    // primary volume?
                    return (String) getPath.invoke(storageVolumeElement);

                if (uuid != null && uuid.equals(volumeId))    // other volumes?
                    return (String) getPath.invoke(storageVolumeElement);
            }
            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private static String getVolumePathForAndroid11AndAbove(final String volumeId, Context context) {
        try {
            StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            List<StorageVolume> storageVolumes = mStorageManager.getStorageVolumes();
            for (StorageVolume storageVolume : storageVolumes) {
                // primary volume?
                if (storageVolume.isPrimary() && PRIMARY_VOLUME_NAME.equals(volumeId))
                    return storageVolume.getDirectory().getPath();

                // other volumes?
                String uuid = storageVolume.getUuid();
                if (uuid != null && uuid.equals(volumeId))
                    return storageVolume.getDirectory().getPath();

            }
            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if (split.length > 0) return split[0];
        else return null;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) return split[1];
        else return File.separator;
    }
}
