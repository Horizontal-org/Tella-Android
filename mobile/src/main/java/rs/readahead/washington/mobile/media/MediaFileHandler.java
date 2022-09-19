package rs.readahead.washington.mobile.media;

import static rs.readahead.washington.mobile.util.C.IMPORT_MULTIPLE_FILES;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultException;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.filter.FilterType;
import com.hzontal.tella_vault.filter.Limits;
import com.hzontal.tella_vault.filter.Sort;
import com.hzontal.tella_vault.rx.RxVaultFileBuilder;
import com.hzontal.utils.MediaFile;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.provider.EncryptedFileProvider;
import rs.readahead.washington.mobile.presentation.entity.mapper.PublicMetadataMapper;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.util.FileUtil;
import timber.log.Timber;


public class MediaFileHandler {
    private static File tmpPath;

    public MediaFileHandler() {
    }

    public static boolean init(Context context) {
        try {
            File mediaPath = new File(context.getFilesDir(), C.MEDIA_DIR); // todo: vault will do this
            boolean ret = FileUtil.mkdirs(mediaPath);

            File metadataPath = new File(context.getFilesDir(), C.METADATA_DIR);
            ret = FileUtil.mkdirs(metadataPath) && ret;

            tmpPath = new File(context.getFilesDir(), C.TMP_DIR);
            return FileUtil.mkdirs(tmpPath) && ret;
        } catch (Exception e) {
            Timber.e(e);
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        }
    }

    public static void emptyTmp(final Context context) {
        Completable.fromCallable((Callable<Void>) () -> {
            FileUtil.emptyDir(new File(context.getFilesDir(), C.TMP_DIR));
            return null;
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    public static void startSelectMediaActivity(Activity activity, @NonNull String type, @Nullable String[] extraMimeType, int requestCode) {
        Intent intent = new Intent();
        intent.setType(type);

        if (extraMimeType != null) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeType);
        }

        if (Build.VERSION.SDK_INT >= 19) {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            try {
                activity.startActivityForResult(intent, requestCode);
                return;
            } catch (ActivityNotFoundException e) {
                Timber.d(e, activity.getClass().getName());
            }
        }

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        intent.setAction(Intent.ACTION_GET_CONTENT);

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Timber.d(e, activity.getClass().getName());
            Toast.makeText(activity, R.string.gallery_toast_fail_import, Toast.LENGTH_LONG).show();
        }
    }

    public static void destroyGallery(@NonNull final Context context) {
        // now is not the time to think about background thread ;)
        FileUtil.emptyDir(new File(context.getFilesDir(), C.MEDIA_DIR));
        FileUtil.emptyDir(new File(context.getFilesDir(), C.METADATA_DIR));
        FileUtil.emptyDir(new File(context.getFilesDir(), C.TMP_DIR));
    }

    public static void exportMediaFile(Context context, VaultFile vaultFile, @Nullable Uri envDirUri) throws IOException {

        String envDirType;
        if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
            envDirType = Environment.DIRECTORY_PICTURES;
        } else if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
            envDirType = Environment.DIRECTORY_MOVIES;
        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            envDirType = Environment.DIRECTORY_MUSIC;
        } else { // this should not happen anyway..
            envDirType = Environment.DIRECTORY_DOCUMENTS;
        }

        InputStream is = null;
        OutputStream os = null;

        File path = null;
        if (envDirUri == null) {
            if (Build.VERSION.SDK_INT >= 29) {
                path = context.getExternalFilesDir(envDirType);
            } else {
                path = Environment.getExternalStoragePublicDirectory(envDirType);
            }
        } else {
            DocumentFile documentFile = DocumentFile.fromTreeUri(context, envDirUri);
            if (documentFile != null) {
                DocumentFile newDocumentFile = documentFile.createFile(vaultFile.mimeType, vaultFile.name);
                ContentResolver contentResolver = context.getContentResolver();
                assert newDocumentFile != null;
                os = contentResolver.openOutputStream(newDocumentFile.getUri());
            }
        }
        File file = null;
        if (path != null) {
            file = new File(path.getAbsolutePath(), MyApplication.rxVault.getFile(vaultFile).getName());
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            if (path != null) {
                if (!path.exists()) path.mkdirs();
            }

            is = MyApplication.vault.getStream(vaultFile);
            if (is == null) {
                throw new IOException();
            }

            if (os == null) {
                os = new FileOutputStream(file);
            }

            IOUtils.copy(is, os);
            if (file != null){
                MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null, null);
            }
        } catch (VaultException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            FileUtil.close(is);
            FileUtil.close(os);
        }
    }


    public static VaultFile importPhotoUri(Context context, Uri uri, @Nullable String parentId) throws Exception {
        // Vault replacement methods
        InputStream v_input = context.getContentResolver().openInputStream(uri); // original photo
        Bitmap v_bm = modifyOrientation(BitmapFactory.decodeStream(v_input), v_input); // bitmap of photo
        Bitmap v_thumb = ThumbnailUtils.extractThumbnail(v_bm, v_bm.getWidth() / 10, v_bm.getHeight() / 10); // bitmap of thumb

        ByteArrayOutputStream v_thumb_jpeg_stream = new ByteArrayOutputStream();
        v_thumb.compress(Bitmap.CompressFormat.JPEG, 100, v_thumb_jpeg_stream);

        ByteArrayOutputStream v_image_jpeg_stream = new ByteArrayOutputStream();
        if (!v_bm.compress(Bitmap.CompressFormat.JPEG, 100, v_image_jpeg_stream)) {
            throw new Exception("JPEG compression failed");
        }

        return MyApplication.rxVault
                .builder(new ByteArrayInputStream(v_image_jpeg_stream.toByteArray()))
                .setMimeType("image/jpeg")
                .setType(VaultFile.Type.FILE)
                .setThumb(v_thumb_jpeg_stream.toByteArray())
                .build(parentId)
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    public static Single<VaultFile> saveJpegPhoto(@NonNull byte[] jpegPhoto, @Nullable String parent) throws Exception {
        // create thumb
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 8;

        Bitmap thumb = BitmapFactory.decodeByteArray(jpegPhoto, 0, jpegPhoto.length, opt);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        InputStream input = new ByteArrayInputStream(jpegPhoto);

        thumb = modifyOrientation(thumb, input);
        thumb.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        input.reset();
        String uid = UUID.randomUUID().toString();
        RxVaultFileBuilder rxVaultFileBuilder = MyApplication.rxVault
                .builder(input)
                .setMimeType("image/jpeg")
                .setName("Photo " + DateUtil.getDate(System.currentTimeMillis()) + ".jpg")
                .setAnonymous(true)
                .setType(VaultFile.Type.FILE)
                .setId(uid)
                .setThumb(getThumbByteArray(thumb));
        if (parent == null) {
            return rxVaultFileBuilder
                    .build()
                    .subscribeOn(Schedulers.io());
        } else {
            return rxVaultFileBuilder
                    .build(parent)
                    .subscribeOn(Schedulers.io());
        }
    }

    public static VaultFile savePngImage(@NonNull byte[] pngImage) {
        // create thumb
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 4;

        final Bitmap thumb = BitmapFactory.decodeByteArray(pngImage, 0, pngImage.length, opt);
        thumb.compress(Bitmap.CompressFormat.PNG, 100, stream);

        // encode png
        InputStream input = new ByteArrayInputStream(pngImage);

        return MyApplication.rxVault
                .builder(input)
                .setMimeType("image/png")
                .setName("Photo " + DateUtil.getDate(System.currentTimeMillis()) + ".png")
                .setAnonymous(true)
                .setType(VaultFile.Type.FILE)
                .setThumb(getThumbByteArray(thumb))
                .build()
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    public static VaultFile importVideoUri(Context context, Uri uri, String parentID) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String mimeType = context.getContentResolver().getType(uri);

        try {
            retriever.setDataSource(context, uri);

            // duration
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long duration = Long.parseLong(time);

            // thumbnail
            byte[] thumb = getThumbByteArray(retriever.getFrameAtTime());

            InputStream is = context.getContentResolver().openInputStream(uri);

            return MyApplication.rxVault
                    .builder(is)
                    .setMimeType(mimeType)
                    .setAnonymous(true)
                    .setThumb(thumb)
                    .setType(VaultFile.Type.FILE)
                    .setDuration(duration)
                    .build(parentID)
                    .subscribeOn(Schedulers.io())
                    .blockingGet();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Timber.e(e, MediaFileHandler.class.getName());

            throw e;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }
    }

    public static VaultFile importOthersUri(Context context, Uri uri, String parentId) throws Exception {
        String mimeType = context.getContentResolver().getType(uri);

        try {

            InputStream is = context.getContentResolver().openInputStream(uri);

            assert DocumentFile.fromSingleUri(context, uri) != null;
            return MyApplication.rxVault
                    .builder(is)
                    .setMimeType(mimeType)
                    .setAnonymous(true)
                    .setName(DocumentFile.fromSingleUri(context, uri).getName())
                    .setType(VaultFile.Type.FILE)
                    .build(parentId)
                    .subscribeOn(Schedulers.io())
                    .blockingGet();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Timber.e(e, MediaFileHandler.class.getName());

            throw e;
        }
    }

    @WorkerThread
    public static VaultFile saveMp4Video(File video, String parent) throws IOException {
        FileInputStream vis = null;
        InputStream is = null;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            vis = new FileInputStream(video);
            retriever.setDataSource(vis.getFD());

            // duration
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            // thumbnail
            byte[] thumb = getThumbByteArray(retriever.getFrameAtTime());

            RxVaultFileBuilder rxVaultFileBuilder = MyApplication.rxVault
                    .builder(new FileInputStream(video))
                    .setAnonymous(false)
                    .setDuration(Long.parseLong(time))
                    .setType(VaultFile.Type.FILE)
                    .setName("Video " + DateUtil.getDate(System.currentTimeMillis()) + ".mp4")
                    .setMimeType("video/mp4")
                    .setThumb(thumb);

            if (parent == null) {
                return rxVaultFileBuilder
                        .build()
                        .blockingGet();
            } else {
                return rxVaultFileBuilder
                        .build(parent)
                        .blockingGet();
            }
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Timber.e(e, MediaFileHandler.class.getName());

            throw e;
        } finally {
            FileUtil.close(vis);
            FileUtil.close(is);
            FileUtil.delete(video);
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }
    }

    public static List<VaultFile> importVaultFilesUris(Context context, @Nullable List<Uri> uris, String parentId) throws Exception {
        List<VaultFile> vaultFiles = new ArrayList<>();
        assert uris != null;
        for (Uri uri : uris) {
            String mimeType = getMimeType(uri, context.getContentResolver());
            if (mimeType != null) {
                if (MediaFile.INSTANCE.isImageFileType(mimeType)) {
                    vaultFiles.add(importPhotoUri(context, uri, parentId));
                } else if (MediaFile.INSTANCE.isVideoFileType(mimeType)) {
                    vaultFiles.add(importVideoUri(context, uri, parentId));
                } else {
                    vaultFiles.add(importOthersUri(context, uri, parentId));
                }
            }
        }
        return vaultFiles;
    }

    @Nullable
    private static String getMimeType(Uri uri, ContentResolver contentResolver) {
        String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            mimeType = contentResolver.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase(Locale.getDefault()));
        }
        return mimeType;
    }

    @Nullable
    private static byte[] getThumbByteArray(@Nullable Bitmap frame) {
        if (frame != null) {
            // todo: make this smarter (maxWith/height or float ratio, keeping aspect)
            Bitmap thumb = ThumbnailUtils.extractThumbnail(frame, frame.getWidth() / 4, frame.getHeight() / 4);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (thumb.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                return stream.toByteArray();
            }
        }

        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    static boolean deleteFile(@NonNull VaultFile vaultFile) {
        try {
            return MyApplication.rxVault.delete(vaultFile)
                    .subscribeOn(Schedulers.io())
                    .blockingGet();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static VaultFile renameFile(@NonNull VaultFile vaultFile, String fileName) {
        try {
            return MyApplication.rxVault.rename(vaultFile.id, fileName)
                    .subscribeOn(Schedulers.io())
                    .blockingGet();
        } catch (Throwable ignored) {
            return vaultFile;
        }
    }

    @Nullable
    public static InputStream getStream(VaultFile vaultFile) {
        try {
            return MyApplication.rxVault.getStream(vaultFile);
        } catch (VaultException e) {
            Timber.d(e, MediaFileHandler.class.getName());
        }

        return null;
    }

    public static Uri getEncryptedUri(Context context, VaultFile vaultFile) {
        File newFile = getFile(vaultFile);
        return FileProvider.getUriForFile(context, EncryptedFileProvider.AUTHORITY, newFile);
    }

    @Nullable
    private static Uri getMetadataUri(Context context, VaultFile vaultFile) {
        try {
            VaultFile mmf = maybeCreateMetadataMediaFile(vaultFile);
            return FileProvider.getUriForFile(context, EncryptedFileProvider.AUTHORITY,
                    getFile(mmf));
        } catch (Exception e) {
            Timber.d(e);
            return null;
        }
    }

    //TODO CHECJ CSV FILE
    public static VaultFile maybeCreateMetadataMediaFile(VaultFile vaultFile) throws Exception {
        RxVaultFileBuilder rxVaultFileBuilder = MyApplication.rxVault.builder()
                .setName(vaultFile.name + ".csv")
                .setMimeType("text/csv");

        VaultFile mmf = rxVaultFileBuilder.
                build()
                .blockingGet();

        OutputStream os = getMetadataOutputStream(mmf);

        if (os == null) throw new NullPointerException();

        createMetadataFile(os, vaultFile);

        return mmf;
    }

    public static File getTempFile() {
        if (tmpPath == null) {
            throw new IllegalStateException("MediaFileHandler not initialized");
        }

        return new File(tmpPath, UUID.randomUUID().toString());
    }

    public static long getSize(VaultFile vaultFile) {
        return getSize(getFile(vaultFile));
    }

    private static long getSize(File file) {
        return file.length() - EncryptedFileProvider.IV_SIZE;
    }

    private static void createMetadataFile(@NonNull OutputStream os, @NonNull VaultFile vaultFile) {
        LinkedHashMap<String, String> map = PublicMetadataMapper.transformToMap(vaultFile);

        PrintStream ps = new PrintStream(os);
        ps.println(TextUtils.join(",", map.keySet()));
        ps.println(TextUtils.join(",", map.values()));
        ps.flush();
        ps.close();
    }

    @Nullable
    private static OutputStream getMetadataOutputStream(VaultFile file) {
        try {
            return MyApplication.rxVault.getOutStream(file);
        } catch (VaultException e) {
            Timber.d(e, MediaFileHandler.class.getName());
        }

        return null;
    }

    public static void startShareActivity(Context context, VaultFile vaultFile, boolean includeMetadata) {
        if (includeMetadata) {
            startShareActivity(context, Collections.singletonList(vaultFile), true);
            return;
        }

        Uri mediaFileUri = getEncryptedUri(context, vaultFile);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType(vaultFile.mimeType);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(shareIntent, context.getText(R.string.action_share));
        chooser.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, mediaFileUri);

        context.startActivity(chooser);
    }

    public static void startShareActivity(Context context, List<VaultFile> mediaFiles, boolean includeMetadata) {
        ArrayList<Uri> uris = new ArrayList<>();

        for (VaultFile vaultFile : mediaFiles) {
            uris.add(getEncryptedUri(context, vaultFile));

            if (includeMetadata && vaultFile.metadata != null) {
                Uri metadataUri = getMetadataUri(context, vaultFile);
                if (metadataUri != null) {
                    uris.add(metadataUri);
                }
            }
        }

        Intent shareIntent = new Intent();
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.setType("*/*");

        Intent chooser = Intent.createChooser(shareIntent, context.getText(R.string.action_share));
        chooser.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        context.startActivity(chooser);
    }

    private static String getMetadataFilename(VaultFile vaultFile) {
        return vaultFile.id + ".csv";
    }

    private static File getFile(VaultFile vaultFile) {
        return MyApplication.rxVault.getFile(vaultFile);
    }

    private static Bitmap modifyOrientation(Bitmap bitmap, InputStream inputStream) throws IOException {
        ExifInterface ei = new ExifInterface(inputStream);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    private static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Observable<List<VaultFile>> getLastVaultFileFromDb() {
        Limits limits = new Limits();
        limits.limit = 1;
        Sort sort = new Sort();
        sort.type = Sort.Type.DATE;
        sort.direction = Sort.Direction.DESC;
        return MyApplication.rxVault.list(null, FilterType.PHOTO_VIDEO, sort, limits)
                .toObservable();
    }

    public static void startImportFiles(Activity context, Boolean multipleFile, String type) {
        Intent intent = new Intent()
                .setType(type)
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multipleFile);

        context.startActivityForResult(
                Intent.createChooser(intent, "Import files"),
                IMPORT_MULTIPLE_FILES
        );
    }

    public static List<VaultFile> walkAllFiles(List<VaultFile> vaultFiles) {
        List<VaultFile> resultList = new ArrayList<>();
        for (VaultFile vaultFile : vaultFiles) {
            if (vaultFile.type == VaultFile.Type.DIRECTORY) {
                resultList.addAll(getAllFiles(vaultFile));
            } else {
                resultList.add(vaultFile);
            }
        }
        return resultList;
    }

    public static List<VaultFile> walkAllFilesWithDirectories(List<VaultFile> vaultFiles) {
        List<VaultFile> resultList = new ArrayList<>(vaultFiles);
        FileWalker fileWalker = new FileWalker();
        for (VaultFile vaultFile : vaultFiles) {
            if (vaultFile.type == VaultFile.Type.DIRECTORY) {
                resultList.addAll(fileWalker.walkWithDirectories(vaultFile));
            }
        }
        return resultList;
    }

    public static VaultFile importVaultFileUri(Context context, @Nullable Uri uri, String parentId) throws Exception {
        VaultFile vaultFile = new VaultFile();
        if (uri != null) {
            String mimeType = getMimeType(uri, context.getContentResolver());
            if (mimeType != null) {
                if (MediaFile.INSTANCE.isImageFileType(mimeType)) {
                    vaultFile = importPhotoUri(context, uri, parentId);
                } else if (MediaFile.INSTANCE.isVideoFileType(mimeType)) {
                    vaultFile = importVideoUri(context, uri, parentId);
                } else {
                    vaultFile = importOthersUri(context, uri, parentId);
                }
            }
        }
        return vaultFile;
    }

    private static List<VaultFile> getAllFiles(VaultFile vaultFile) {
        FileWalker fileWalker = new FileWalker();
        return fileWalker.walk(vaultFile);
    }

    private static String getOriginalFileName(Context context, Uri uri) {
        Cursor query = context.getContentResolver().query(uri, null, null, null, null);
        int nameColumnIndex = query.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        query.moveToFirst();
        return query.getString(nameColumnIndex);
    }

    @Nullable
    public InputStream getThumbnailStream(final VaultFile vaultFile) {
        if (vaultFile.thumb != null) {
            return new ByteArrayInputStream(vaultFile.thumb);
        }

        return null;
    }
}
