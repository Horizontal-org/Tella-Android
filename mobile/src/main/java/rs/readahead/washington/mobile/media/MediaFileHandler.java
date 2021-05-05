package rs.readahead.washington.mobile.media;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultException;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;
import com.hzontal.utils.VaultUtils;

import org.apache.commons.io.IOUtils;
import org.hzontal.tella.keys.key.LifecycleMainKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.Completable;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.data.provider.EncryptedFileProvider;
import rs.readahead.washington.mobile.presentation.entity.mapper.PublicMetadataMapper;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.StringUtils;
import timber.log.Timber;


public class MediaFileHandler {
    private Executor executor;
    private KeyDataSource keyDataSource;


    public MediaFileHandler(KeyDataSource keyDataSource) {
        this.keyDataSource = keyDataSource;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static boolean init(Context context) {
        try {
            File mediaPath = new File(context.getFilesDir(), C.MEDIA_DIR);
            boolean ret = FileUtil.mkdirs(mediaPath);

            File metadataPath = new File(context.getFilesDir(), C.METADATA_DIR);
            ret = FileUtil.mkdirs(metadataPath) && ret;

            File tmpPath = new File(context.getFilesDir(), C.TMP_DIR);
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

        if (extraMimeType != null && Build.VERSION.SDK_INT >= 19) {
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

        //if (Build.VERSION.SDK_INT >= 18) {
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        Timber.d("+++++ get multiple");
        // }

        intent.setAction(Intent.ACTION_GET_CONTENT);

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Timber.d(e, activity.getClass().getName());
            Toast.makeText(activity, R.string.gallery_toast_fail_import, Toast.LENGTH_LONG).show();
        }
    }

    public static boolean deleteMediaFile(@NonNull Context context, @NonNull VaultFile vaultFile) {
        File file = getFile(context, vaultFile);
        File metadata = getMetadataFile(context, vaultFile);
        return file.delete() || metadata.delete();
    }

    public static void destroyGallery(@NonNull final Context context) {
        // now is not the time to think about background thread ;)
        FileUtil.emptyDir(new File(context.getFilesDir(), C.MEDIA_DIR));
        FileUtil.emptyDir(new File(context.getFilesDir(), C.METADATA_DIR));
        FileUtil.emptyDir(new File(context.getFilesDir(), C.TMP_DIR));
    }

        public static void exportMediaFile(Context context, VaultFile vaultFile) throws IOException {
        String envDirType;

        if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
            envDirType = Environment.DIRECTORY_PICTURES;
        } else if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
            envDirType = Environment.DIRECTORY_MOVIES;
        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            envDirType = Environment.DIRECTORY_MUSIC;
        } else { // this should not happen anyway..
            if (Build.VERSION.SDK_INT >= 19) {
                envDirType = Environment.DIRECTORY_DOCUMENTS;
            } else {
                envDirType = Environment.DIRECTORY_PICTURES;
            }
        }

        File path;
        if (Build.VERSION.SDK_INT >= 29) {
            path = context.getExternalFilesDir(envDirType);
        } else {
            path = Environment.getExternalStoragePublicDirectory(envDirType);
        }
        File file = new File(path, vaultFile.name);

        InputStream is = null;
        OutputStream os = null;

        try {
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();

            is = MyApplication.vault.getStream(vaultFile);
            if (is == null) {
                throw new IOException();
            }

            os = new FileOutputStream(file);

            IOUtils.copy(is, os);

            MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null, null);
        } catch (VaultException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            FileUtil.close(is);
            FileUtil.close(os);
        }
    }

    public static VaultFile importPhotoUri(Context context, Uri uri) throws Exception {
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

        // Rx version
        return MyApplication.rxVault
                .builder(new ByteArrayInputStream(v_image_jpeg_stream.toByteArray()))
                .setMimeType("image/jpeg")
                .setThumb(v_thumb_jpeg_stream.toByteArray())
                .build()
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    public static Single<VaultFile> saveJpegPhoto(@NonNull byte[] jpegPhoto) throws Exception {

        // create thumb
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 8;

        Bitmap thumb = BitmapFactory.decodeByteArray(jpegPhoto, 0, jpegPhoto.length, opt);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        InputStream input = new ByteArrayInputStream(jpegPhoto);

        thumb = modifyOrientation(thumb, input);
        thumb.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        input.reset();

        return  MyApplication.rxVault
                .builder(input)
                .setMimeType("image/jpeg")
                .setAnonymous(true)
                .setId(UUID.randomUUID().toString())
                .setThumb(getThumbByteArray(thumb))
                .build()
                .subscribeOn(Schedulers.io());

    }

    public static VaultFile savePngImage(@NonNull Context context, @NonNull byte[] pngImage) throws Exception {
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
                .setAnonymous(true)
                .setId(UUID.randomUUID().toString())
                .setThumb(getThumbByteArray(thumb))
                .build()
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    public static VaultFile importVideoUri(Context context, Uri uri) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String mimeType = MediaFile.INSTANCE.getFileExtension(uri,context);
        byte[] thumb = null;
        Long duration = null;

        try {
            retriever.setDataSource(context, uri);
            // duration
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Long.parseLong(time);
            // thumbnail
            thumb = getThumbByteArray(retriever.getFrameAtTime());
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Timber.e(e, MediaFileHandler.class.getName());
        } finally {
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }
        InputStream is = context.getContentResolver().openInputStream(uri);

        return MyApplication.rxVault
                .builder(is)
                .setMimeType(mimeType)
                .setAnonymous(true)
                .setThumb(thumb)
                .setDuration(duration)
                .build()
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    public static VaultFile saveMp4Video(Context context, File video) {
        FileInputStream vis = null;
        InputStream is = null;
        DigestOutputStream os = null;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        VaultFile vaultFile = VaultUtils.INSTANCE.newMp4();

        try {
            vaultFile.anonymous = false; // todo: mp4 can have exif, check if it does

            vis = new FileInputStream(video);
            retriever.setDataSource(vis.getFD());

            // duration
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            vaultFile.duration = Long.parseLong(time);

            // thumbnail
            byte[] thumb = getThumbByteArray(retriever.getFrameAtTime());
            if (thumb != null){
                vaultFile.thumb = thumb;
            }

            is = new FileInputStream(video);
            os = MediaFileHandler.getOutputStream(context, vaultFile);

            if (os == null) throw new NullPointerException();

            IOUtils.copy(is, os);

            vaultFile.hash = StringUtils.hexString(os.getMessageDigest().digest());
            vaultFile.size = getSize(context, vaultFile);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Timber.e(e, MediaFileHandler.class.getName());
        } finally {
            FileUtil.close(vis);
            FileUtil.close(is);
            FileUtil.close(os);
            FileUtil.delete(video);
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }

        return vaultFile;
    }

    /*@NonNull
    public static MediaFileThumbnailData getVideoThumb(@NonNull File file) {
        FileInputStream vis = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            vis = new FileInputStream(file);

            retriever.setDataSource(vis.getFD());

            // thumbnail
            byte[] thumb = getThumbByteArray(retriever.getFrameAtTime());
            if (thumb != null) {
                return new MediaFileThumbnailData(thumb);
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Timber.e(e, MediaFileHandler.class.getName());
        } finally {
            FileUtil.close(vis);
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }

        return MediaFileThumbnailData.NONE;
    }*/

    /*@NonNull
    public static Bitmap getVideoBitmapThumb(@NonNull File file) {
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);

        if (thumb == null) {
            thumb = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        }

        return thumb;
    }*/

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
    static boolean deleteFile(Context context, @NonNull VaultFile mediaFile) {
        try {
            return getFile(context, mediaFile).delete();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    public static InputStream getStream(Context context, VaultFile vaultFile) {
        try {
            File file = getFile(context, vaultFile);
            FileInputStream fis = new FileInputStream(file);
            byte[] key;

            if ((key = MyApplication.getMainKeyHolder().get().getKey().getEncoded()) == null) {
                return null;
            }


            return EncryptedFileProvider.getDecryptedLimitedInputStream(key, fis, file);

        } catch (IOException | LifecycleMainKey.MainKeyUnavailableException e) {
            Timber.d(e, MediaFileHandler.class.getName());
        }

        return null;
    }

    public static Uri getEncryptedUri(Context context, VaultFile mediaFile) {
        File newFile = getFile(context, mediaFile);
        return FileProvider.getUriForFile(context, EncryptedFileProvider.AUTHORITY, newFile);
    }

    @Nullable
    private static Uri getMetadataUri(Context context, VaultFile vaultFile) {
        try {
            VaultFile mmf = maybeCreateMetadataMediaFile(context, vaultFile);
            return FileProvider.getUriForFile(context, EncryptedFileProvider.AUTHORITY,
                    getFile(context, vaultFile));
        } catch (Exception e) {
            Timber.d(e);
            return null;
        }
    }
    //TODO CHECJ CSV FILE
    public static VaultFile maybeCreateMetadataMediaFile(Context context, VaultFile vaultFile) throws Exception {
        VaultFile mmf = new VaultFile();
        File file = getFile(context, vaultFile);

        if (file.createNewFile()) {
            OutputStream os = getMetadataOutputStream(file);

            if (os == null) throw new NullPointerException();

            createMetadataFile(os, vaultFile);
        }

        mmf.size = getSize(file);

        return mmf;
    }

    public static File getTempFile(Context context, VaultFile vaultFile) {
        return getFile(context, vaultFile);
    }

    public static long getSize(Context context, VaultFile vaultFile) {
        return getSize(getFile(context, vaultFile));
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
    static DigestOutputStream getOutputStream(Context context, VaultFile vaultFile) {
        try {
            File file = getFile(context, vaultFile);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] key;

            if ((key = MyApplication.getMainKeyHolder().get().getKey().getEncoded()) == null) {
                return null;
            }


            return new DigestOutputStream(EncryptedFileProvider.getEncryptedOutputStream(key, fos, file.getName()),
                    getMessageDigest());

        } catch (IOException | NoSuchAlgorithmException | LifecycleMainKey.MainKeyUnavailableException e) {
            Timber.d(e, MediaFileHandler.class.getName());
        }

        return null;
    }

    @Nullable
    private static OutputStream getMetadataOutputStream(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            byte[] key;

            if ((key = MyApplication.getMainKeyHolder().get().getKey().getEncoded()) == null) {
                return null;
            }

            return EncryptedFileProvider.getEncryptedOutputStream(key, fos, file.getName());

        } catch (IOException | LifecycleMainKey.MainKeyUnavailableException e) {
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
        shareIntent.putExtra(Intent.EXTRA_STREAM, mediaFileUri);
        shareIntent.setType(FileUtil.getMimeType(vaultFile.name));

        context.startActivity(Intent.createChooser(shareIntent, context.getText(R.string.action_share)));
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
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.setType("*/*");

        context.startActivity(Intent.createChooser(shareIntent, context.getText(R.string.action_share)));
    }

    private static String getMetadataFilename(VaultFile vaultFile) {
        return vaultFile.id + ".csv";
    }

    private static File getFile(@NonNull Context context, VaultFile vaultFile) {
        final File mediaPath = new File(context.getFilesDir(), vaultFile.path);
        return new File(mediaPath, vaultFile.name);
    }

    private static File getMetadataFile(@NonNull Context context, VaultFile vaultFile) {
        final File metadataPath = new File(context.getFilesDir(), C.METADATA_DIR);
        return new File(metadataPath, getMetadataFilename(vaultFile));
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

    private static void copyToMediaFileStream(Context context, VaultFile vaultFile, InputStream is) throws IOException {
        DigestOutputStream os = MediaFileHandler.getOutputStream(context, vaultFile);

        if (os == null) throw new NullPointerException();

        IOUtils.copy(is, os);
        FileUtil.close(is);
        FileUtil.close(os);

        vaultFile.hash = StringUtils.hexString(os.getMessageDigest().digest());
        vaultFile.size = getSize(context, vaultFile);
    }

    private static MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    public Observable<VaultFile> registerMediaFileBundle(final VaultFile vaultFile){
        return keyDataSource.getDataSource()
                .flatMap((Function<DataSource, ObservableSource<VaultFile>>) dataSource ->
                        dataSource.registerMediaFileBundle(vaultFile).toObservable());
    }


    public Observable<VaultFile> registerMediaFile(final VaultFile vaultFile) {
        return keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<VaultFile>>) dataSource -> dataSource.registerMediaFile(vaultFile).toObservable());
    }

    @Nullable
    InputStream getThumbnailStream(Context context, final VaultFile vaultFile) {
        VaultFile thumbnailData = null;
        InputStream inputStream = null;

        try {
            thumbnailData = getThumbnailData(vaultFile);
        } catch (NoSuchElementException e) {
            try {
                thumbnailData = updateThumb(context, vaultFile);
            } catch (Exception e1) {
                Timber.d(e1, getClass().getName());
            }
        } catch (Exception e2) {
            Timber.d(e2, getClass().getName());
        }

        if (thumbnailData != null) {
            inputStream = new ByteArrayInputStream(vaultFile.thumb);
        }

        return inputStream;
    }

    private VaultFile getThumbnailData(final VaultFile vaultFile) throws NoSuchElementException {
        return keyDataSource
                .getDataSource()
                .flatMapMaybe((Function<DataSource, MaybeSource<VaultFile>>) dataSource ->
                        dataSource.getMediaFileThumbnail(vaultFile.id)).blockingFirst();
    }

    private VaultFile updateThumb(final Context context, final VaultFile vaultFile) {
        return Observable
                .fromCallable(() -> createThumb(context, vaultFile))
                .subscribeOn(Schedulers.from(executor)) // creating thumbs in single thread..
                .flatMap((Function<VaultFile, ObservableSource<VaultFile>>) mediaFileThumbnailData ->
                        keyDataSource.getDataSource()
                                .flatMapSingle((Function<DataSource, SingleSource<VaultFile>>) dataSource ->
                                        dataSource.updateMediaFileThumbnail(vaultFile)))
                .blockingFirst();
    }

    @NonNull
    private VaultFile createThumb(Context context, VaultFile vaultFile) {
        try {
            File file = getFile(context, vaultFile);
            FileInputStream fis = new FileInputStream(file);
            byte[] key;

            if ((key = MyApplication.getMainKeyHolder().get().getKey().getEncoded()) == null) {
                return null;
            }


            InputStream inputStream = EncryptedFileProvider.getDecryptedInputStream(key, fis, file.getName()); // todo: move to limited variant
            final Bitmap bm = BitmapFactory.decodeStream(inputStream);

            Bitmap thumb;

            if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
                thumb = ThumbnailUtils.extractThumbnail(bm, bm.getWidth() / 10, bm.getHeight() / 10);
            } else {
                return null;
            }
            VaultFile vaultFile1 = new VaultFile();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            vaultFile1.thumb = stream.toByteArray();
            return vaultFile1;
        } catch (IOException | LifecycleMainKey.MainKeyUnavailableException e) {
            Timber.d(e, getClass().getName());
        }

        return null;
    }
}
