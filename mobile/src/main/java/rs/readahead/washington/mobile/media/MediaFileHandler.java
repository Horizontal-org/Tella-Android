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
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.Completable;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.provider.EncryptedFileProvider;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.RawMediaFile;
import rs.readahead.washington.mobile.domain.entity.TempMediaFile;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.FileUtil;
import timber.log.Timber;


public class MediaFileHandler {
    private CacheWordDataSource cacheWordDataSource;
    private Executor executor;


    public MediaFileHandler(CacheWordDataSource cacheWordDataSource) {
        this.cacheWordDataSource = cacheWordDataSource;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static boolean init(Context context) {
        try {
            File mediaPath = new File(context.getFilesDir(), C.MEDIA_DIR);
            boolean ret = FileUtil.mkdirs(mediaPath);
            File tmpPath = new File(context.getFilesDir(), C.TMP_DIR);
            return FileUtil.mkdirs(tmpPath) && ret;
        } catch (Exception e) {
            Crashlytics.logException(e);
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

        intent.setAction(Intent.ACTION_GET_CONTENT);

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Timber.d(e, activity.getClass().getName());
            Toast.makeText(activity, R.string.ra_error_opening_phone_gallery, Toast.LENGTH_LONG).show();
        }
    }

    public static boolean deleteMediaFile(@NonNull Context context, @NonNull MediaFile mediaFile) {
        File mediaPath = new File(context.getFilesDir(), mediaFile.getPath());
        File file = new File(mediaPath, mediaFile.getFileName());
        return file.delete();
    }

    public static void destroyGallery(@NonNull final Context context) {
        // now is not the time to think about background thread ;)
        FileUtil.emptyDir(new File(context.getFilesDir(), C.MEDIA_DIR));
        FileUtil.emptyDir(new File(context.getFilesDir(), C.TMP_DIR));
    }

    public static void exportMediaFile(Context context, MediaFile mediaFile) throws IOException {
        String envDirType;

        if (mediaFile.getType() == MediaFile.Type.IMAGE) {
            envDirType = Environment.DIRECTORY_PICTURES;
        } else if (mediaFile.getType() == MediaFile.Type.VIDEO) {
            envDirType = Environment.DIRECTORY_MOVIES;
        } else if (mediaFile.getType() == MediaFile.Type.AUDIO) {
            envDirType = Environment.DIRECTORY_MUSIC;
        } else { // this should not happen anyway..
            if (Build.VERSION.SDK_INT >= 19) {
                envDirType = Environment.DIRECTORY_DOCUMENTS;
            } else {
                envDirType = Environment.DIRECTORY_PICTURES;
            }
        }

        File path = Environment.getExternalStoragePublicDirectory(envDirType);
        File file = new File(path, mediaFile.getFileName());

        InputStream is = null;
        OutputStream os = null;

        try {
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();

            is = MediaFileHandler.getStream(context, mediaFile);
            if (is == null) {
                throw new IOException();
            }

            os = new FileOutputStream(file);

            IOUtils.copy(is, os);

            MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null, null);
        } catch (IOException e) {
            Crashlytics.logException(e);
            throw e;
        } finally {
            FileUtil.close(is);
            FileUtil.close(os);
        }
    }

    public static MediaFileBundle importPhotoUri(Context context, Uri uri) throws Exception {
        MediaFileBundle mediaFileBundle = new MediaFileBundle();

        MediaFile mediaFile = MediaFile.newJpeg();
        mediaFileBundle.setMediaFile(mediaFile);

        if (!Preferences.isAnonymousMode()) {
            Metadata metadata = MediaFileProofHandler.getProofMetadata(context, uri);

            if (metadata != null && !metadata.getInternal()) {
                mediaFile.setMetadata(metadata);
            }
        }

        InputStream input = context.getContentResolver().openInputStream(uri);

        Bitmap bm = BitmapFactory.decodeStream(input);
        input = context.getContentResolver().openInputStream(uri);
        bm = modifyOrientation(bm, input);
        Bitmap thumb = ThumbnailUtils.extractThumbnail(bm, bm.getWidth() / 10, bm.getHeight() / 10); // todo: make this smarter and global

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (thumb.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
            mediaFileBundle.setMediaFileThumbnailData(new MediaFileThumbnailData(stream.toByteArray()));
        }

        // todo: killing too much quality like this?

        OutputStream os = MediaFileHandler.getOutputStream(context, mediaFile);

        if (!bm.compress(Bitmap.CompressFormat.JPEG, 100, os)) {
            throw new Exception("JPEG compression failed");
        }

        mediaFile.setSize(getSize(context, mediaFile));

        return mediaFileBundle;
    }


    public static MediaFileBundle saveJpegPhoto(@NonNull Context context, @NonNull byte[] jpegPhoto) throws Exception {
        MediaFileBundle mediaFileBundle = new MediaFileBundle();

        MediaFile mediaFile = MediaFile.newJpeg();
        mediaFile.setAnonymous(true);

        mediaFileBundle.setMediaFile(mediaFile);

        // create thumb
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 8;

        Bitmap thumb = BitmapFactory.decodeByteArray(jpegPhoto, 0, jpegPhoto.length, opt);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        InputStream input = new ByteArrayInputStream(jpegPhoto);

        thumb = modifyOrientation(thumb, input);
        thumb.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        mediaFileBundle.setMediaFileThumbnailData(new MediaFileThumbnailData(stream.toByteArray()));

        input.reset();
        OutputStream os = MediaFileHandler.getOutputStream(context, mediaFile);

        IOUtils.copy(input, os);
        FileUtil.close(input);
        FileUtil.close(os);

        mediaFile.setSize(getSize(context, mediaFile));

        return mediaFileBundle;
    }

    public static MediaFileBundle savePngImage(@NonNull Context context, @NonNull byte[] pngImage) throws Exception {
        MediaFileBundle mediaFileBundle = new MediaFileBundle();

        MediaFile mediaFile = MediaFile.newPng();
        mediaFile.setAnonymous(true);

        mediaFileBundle.setMediaFile(mediaFile);

        // create thumb
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 4;

        final Bitmap thumb = BitmapFactory.decodeByteArray(pngImage, 0, pngImage.length, opt);
        thumb.compress(Bitmap.CompressFormat.PNG, 100, stream);

        mediaFileBundle.setMediaFileThumbnailData(new MediaFileThumbnailData(stream.toByteArray()));

        // encode png
        InputStream input = new ByteArrayInputStream(pngImage);
        OutputStream os = MediaFileHandler.getOutputStream(context, mediaFile);

        IOUtils.copy(input, os);
        FileUtil.close(input);
        FileUtil.close(os);

        mediaFile.setSize(getSize(context, mediaFile));

        return mediaFileBundle;
    }

    public static MediaFileBundle importVideoUri(Context context, Uri uri) throws Exception {
        MediaFileBundle mediaFileBundle = new MediaFileBundle();

        MediaFile mediaFile = MediaFile.newMp4();
        mediaFileBundle.setMediaFile(mediaFile);

        if (!Preferences.isAnonymousMode()) {
            Metadata metadata = MediaFileProofHandler.getProofMetadata(context, uri);
            if (metadata != null && !metadata.getInternal()) {
                mediaFile.setMetadata(metadata);
            }
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(context, uri);

            // duration
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            mediaFile.setDuration(Long.parseLong(time));

            // thumbnail
            byte[] thumb = getThumbByteArray(retriever.getFrameAtTime());
            if (thumb != null) {
                mediaFileBundle.setMediaFileThumbnailData(new MediaFileThumbnailData(thumb));
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
            Timber.e(e, MediaFileHandler.class.getName());
        } finally {
            try {
                retriever.release();
            } catch (Exception ignore) {
            }
        }

        InputStream is = context.getContentResolver().openInputStream(uri);
        OutputStream os = MediaFileHandler.getOutputStream(context, mediaFile);

        //noinspection ConstantConditions
        IOUtils.copy(is, os);
        FileUtil.close(is);
        FileUtil.close(os);

        mediaFile.setSize(getSize(context, mediaFile));

        return mediaFileBundle;
    }

    public static MediaFileBundle saveMp4Video(Context context, File video) {
        FileInputStream vis = null;
        InputStream is = null;
        OutputStream os = null;

        MediaFileBundle mediaFileBundle = new MediaFileBundle();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            MediaFile mediaFile = MediaFile.newMp4();
            mediaFileBundle.setMediaFile(mediaFile);

            mediaFile.setAnonymous(false); // todo: mp4 can have exif, check if it does

            vis = new FileInputStream(video);
            retriever.setDataSource(vis.getFD());

            // duration
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            mediaFile.setDuration(Long.parseLong(time));

            // thumbnail
            byte[] thumb = getThumbByteArray(retriever.getFrameAtTime());
            if (thumb != null) {
                mediaFileBundle.setMediaFileThumbnailData(new MediaFileThumbnailData(thumb));
            }

            is = new FileInputStream(video);
            os = MediaFileHandler.getOutputStream(context, mediaFile);

            //noinspection ConstantConditions
            IOUtils.copy(is, os);

            mediaFile.setSize(getSize(context, mediaFile));
        } catch (Exception e) {
            Crashlytics.logException(e);
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

        return mediaFileBundle;
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
            Crashlytics.logException(e);
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

    @NonNull
    public static Bitmap getVideoBitmapThumb(@NonNull File file) {
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);

        if (thumb == null) {
            thumb = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        }

        return thumb;
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

    public Observable<MediaFileBundle> registerMediaFileBundle(final MediaFileBundle mediaFileBundle) {
        return cacheWordDataSource.getDataSource()
                .flatMap((Function<DataSource, ObservableSource<MediaFileBundle>>) dataSource ->
                        dataSource.registerMediaFileBundle(mediaFileBundle).toObservable());
    }

    public Observable<MediaFile> registerMediaFile(final MediaFileBundle mediaFileBundle) {
        return registerMediaFile(mediaFileBundle.getMediaFile(), mediaFileBundle.getMediaFileThumbnailData());
    }

    public Observable<MediaFile> registerMediaFile(final MediaFile mediaFile, final MediaFileThumbnailData thumbnailData) {
        return cacheWordDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<MediaFile>>) dataSource ->
                dataSource.registerMediaFile(mediaFile, thumbnailData).toObservable());
    }

    @SuppressWarnings("UnusedReturnValue")
    static boolean deleteFile(Context context, @NonNull MediaFile mediaFile) {
        try {
            return getFile(context, mediaFile).delete();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /*public Single<MediaFileThumbnailData> updateThumbnail(final MediaFile mediaFile, final MediaFileThumbnailData mediaFileThumbnailData) {
        return cacheWordDataSource.getDataSource().flatMap(new Function<DataSource, ObservableSource<MediaFileThumbnailData>>() {
            @Override
            public ObservableSource<MediaFileThumbnailData> apply(@NonNull DataSource dataSource) throws Exception {
                return dataSource.updateMediaFileThumbnail(mediaFile.getId(), mediaFileThumbnailData).toObservable();
            }
        }).singleOrError();
    }*/

    @Nullable
    public static InputStream getStream(Context context, MediaFile mediaFile) {
        try {
            File file = getFile(context, mediaFile);
            FileInputStream fis = new FileInputStream(file);
            KeyBundle keyBundle;

            if ((keyBundle = MyApplication.getKeyBundle()) == null) {
                return null;
            }

            byte[] key = keyBundle.getKey();
            if (key == null) {
                return null;
            }

            return EncryptedFileProvider.getDecryptedLimitedInputStream(key, fis, file);

        } catch (IOException e) {
            Timber.d(e, MediaFileHandler.class.getName());
        }

        return null;
    }

    public static Uri getEncryptedUri(Context context, MediaFile mediaFile) {
        File newFile = getFile(context, mediaFile);
        return FileProvider.getUriForFile(context, EncryptedFileProvider.AUTHORITY, newFile);
    }

    public static File getTempFile(Context context, TempMediaFile mediaFile) {
        return getFile(context, mediaFile);
    }

    public static long getSize(Context context, MediaFile mediaFile) {
        return getFile(context, mediaFile).length() - EncryptedFileProvider.IV_SIZE;
    }

    @Nullable
    static OutputStream getOutputStream(Context context, MediaFile mediaFile) {
        try {
            File file = getFile(context, mediaFile);
            FileOutputStream fos = new FileOutputStream(file);
            KeyBundle keyBundle;

            if ((keyBundle = MyApplication.getKeyBundle()) == null) {
                return null;
            }

            byte[] key = keyBundle.getKey();
            if (key == null) {
                return null;
            }

            return EncryptedFileProvider.getEncryptedOutputStream(key, fos, file.getName());

        } catch (IOException e) {
            Timber.d(e, MediaFileHandler.class.getName());
        }

        return null;
    }

    @Nullable
    InputStream getThumbnailStream(Context context, final MediaFile mediaFile) {
        MediaFileThumbnailData thumbnailData = null;
        InputStream inputStream = null;

        try {
            thumbnailData = getThumbnailData(mediaFile);
        } catch (NoSuchElementException e) {
            try {
                thumbnailData = updateThumb(context, mediaFile);
            } catch (Exception e1) {
                Timber.d(e1, getClass().getName());
            }
        } catch (Exception e2) {
            Timber.d(e2, getClass().getName());
        }

        if (thumbnailData != null) {
            inputStream = new ByteArrayInputStream(thumbnailData.getData());
        }

        return inputStream;
    }

    public static void startShareActivity(Context context, MediaFile mediaFile) {
        Uri mediaFileUri = getEncryptedUri(context, mediaFile);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, mediaFileUri);
        shareIntent.setType(FileUtil.getMimeType(mediaFile.getFileName()));

        context.startActivity(Intent.createChooser(shareIntent, context.getText(R.string.ra_share)));
    }

    public static void startShareActivity(Context context, List<MediaFile> mediaFiles) {
        ArrayList<Uri> uris = new ArrayList<>();

        for (MediaFile mediaFile : mediaFiles) {
            uris.add(getEncryptedUri(context, mediaFile));
        }

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.setType("*/*");

        context.startActivity(Intent.createChooser(shareIntent, context.getText(R.string.ra_share)));
    }

    private static File getFile(@NonNull Context context, RawMediaFile mediaFile) {
        final File mediaPath = new File(context.getFilesDir(), mediaFile.getPath());
        return new File(mediaPath, mediaFile.getFileName());
    }

    private MediaFileThumbnailData getThumbnailData(final MediaFile mediaFile) throws NoSuchElementException {
        return cacheWordDataSource
                .getDataSource()
                .flatMapMaybe((Function<DataSource, MaybeSource<MediaFileThumbnailData>>) dataSource ->
                        dataSource.getMediaFileThumbnail(mediaFile.getUid())).blockingFirst();
    }

    private MediaFileThumbnailData updateThumb(final Context context, final MediaFile mediaFile) {
        return Observable
                .fromCallable(() -> createThumb(context, mediaFile))
                .subscribeOn(Schedulers.from(executor)) // creating thumbs in single thread..
                .flatMap((Function<MediaFileThumbnailData, ObservableSource<MediaFileThumbnailData>>) mediaFileThumbnailData ->
                        cacheWordDataSource.getDataSource()
                                .flatMapSingle((Function<DataSource, SingleSource<MediaFileThumbnailData>>) dataSource ->
                                        dataSource.updateMediaFileThumbnail(mediaFile.getId(), mediaFileThumbnailData)))
                .blockingFirst();
    }

    @NonNull
    private MediaFileThumbnailData createThumb(Context context, MediaFile mediaFile) {
        try {
            File file = getFile(context, mediaFile);
            FileInputStream fis = new FileInputStream(file);
            KeyBundle keyBundle;

            if ((keyBundle = MyApplication.getKeyBundle()) == null) {
                return MediaFileThumbnailData.NONE;
            }

            byte[] key = keyBundle.getKey();
            if (key == null) {
                return MediaFileThumbnailData.NONE;
            }

            InputStream inputStream = EncryptedFileProvider.getDecryptedInputStream(key, fis, file.getName()); // todo: move to limited variant
            final Bitmap bm = BitmapFactory.decodeStream(inputStream);

            Bitmap thumb;

            if (mediaFile.getType() == MediaFile.Type.IMAGE) {
                thumb = ThumbnailUtils.extractThumbnail(bm, bm.getWidth() / 10, bm.getHeight() / 10);
            } else {
                return MediaFileThumbnailData.NONE;
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            return new MediaFileThumbnailData(stream.toByteArray());
        } catch (IOException e) {
            Timber.d(e, getClass().getName());
        }

        return MediaFileThumbnailData.NONE;
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

}
