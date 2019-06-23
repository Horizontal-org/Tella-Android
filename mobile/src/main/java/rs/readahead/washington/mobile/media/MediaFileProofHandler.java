package rs.readahead.washington.mobile.media;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;

import org.witness.proofmode.ProofMode;
import org.witness.proofmode.crypto.DetachedSignatureProcessor;
import org.witness.proofmode.crypto.HashUtils;
import org.witness.proofmode.crypto.PgpUtils;
import org.witness.proofmode.service.MediaWatcher;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

import rs.readahead.washington.mobile.domain.entity.Metadata;
import rs.readahead.washington.mobile.domain.entity.MyLocation;
import timber.log.Timber;


class MediaFileProofHandler {
    @Nullable
    static Metadata getProofMetadata(Context context, Uri mediaUri) {
        Metadata metadata = null;

        try {
            String[] projection = {MediaStore.Images.Media.DATA};

            Cursor cursor = context.getContentResolver().query(getRealUri(mediaUri), projection, null, null, null);

            if (cursor == null) {
                return null;
            }

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                final String mediaPath = getPath(context, mediaUri);

                if (mediaPath != null) {
                    String hash = HashUtils.getSHA256FromFileContent(new File(mediaPath));

                    if (hash != null) {
                        File fileMedia = new File(mediaPath);
                        File fileFolder = MediaWatcher.getHashStorageDir(hash);

                        if (fileFolder != null) {
                            File fileMediaSig = new File(fileFolder, fileMedia.getName() + ProofMode.OPENPGP_FILE_TAG);
                            File fileMediaProof = new File(fileFolder, fileMedia.getName() + ProofMode.PROOF_FILE_TAG);
                            File fileMediaProofSig = new File(fileFolder, fileMedia.getName() + ProofMode.PROOF_FILE_TAG + ProofMode.OPENPGP_FILE_TAG);

                            if (fileMediaSig.exists() && fileMediaProof.exists() && fileMediaProofSig.exists()) {
                                if (verifyDetachedSignature(context, fileMediaProof.getAbsolutePath(), fileMediaProofSig.getAbsolutePath())) {
                                    metadata = makeMetadataFromCSV(fileMediaProof);
                                }
                                else {
                                    Timber.d("Verify signature failed for: %s", fileMediaProof.getName());
                                }
                            }

                            // todo: generate now?
                        }
                    }
                }
            }

            cursor.close();
        } catch (Exception e) {
            Timber.d(e);
        }

        return metadata;
    }

    @NonNull
    private static Metadata makeMetadataFromCSV(File csvFile) {
        Metadata metadata = new Metadata();

        try {
            Scanner scanner = new Scanner(csvFile);

            String[] keys = scanner.nextLine().split(",");
            String[] values = scanner.nextLine().split(",");

            for (int i = 0; i < keys.length; i++) {
                try {
                    String key = keys[i];
                    String value = (i < values.length ? values[i] : "");

                    setMetadataValue(metadata, key, value);
                } catch (Exception e) {
                    Timber.d(e);
                }
            }

            scanner.close();
        } catch (Exception exception) {
            Crashlytics.logException(exception);
            return metadata;
        }

        metadata.setInternal(false);

        return metadata;
    }

    private static Uri getRealUri(Uri contentUri) {
        String unusablePath = contentUri.getPath();

        if (unusablePath == null) {
            return contentUri;
        }

        int startIndex = unusablePath.indexOf("external/");
        int endIndex = unusablePath.indexOf("/ACTUAL");

        if (startIndex != -1 && endIndex != -1) {
            String embeddedPath = unusablePath.substring(startIndex, endIndex);

            Uri.Builder builder = contentUri.buildUpon();
            builder.path(embeddedPath);
            builder.authority("media");

            return builder.build();
        }

        return contentUri;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @Nullable
    private static String getPath(final Context context, final Uri uri) {
        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) { // ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes

            } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) { // MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) { // MediaStore (and general)
            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) { // File
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        final String column = "_data";
        final String[] projection = {
                column
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }

        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private static void setMetadataValue(@NonNull Metadata metadata, String key, String value) {
        switch (key) {
            case "Locale":
                metadata.setLocale(value);
                break;

            case "IPv6":
                metadata.setIPv6(value);
                break;

            case "IPv4":
                metadata.setIPv4(value);
                break;

            case "Language":
                metadata.setLanguage(value);
                break;

            case "NetworkType":
                metadata.setNetworkType(value);
                break;

            case "Network":
                metadata.setNetwork(value);
                break;

            case "Manufacturer":
                metadata.setManufacturer(value);
                break;

            case "DataType":
                metadata.setDataType(value);
                break;

            case "DeviceID":
                metadata.setDeviceID(value);
                break;

            case "Hardware":
                metadata.setHardware(value);
                break;

            case "Wifi MAC":
                metadata.setWifiMac(value);
                break;

            case "File Path":
                metadata.setFilePath(value);
                break;

            case "File Hash SHA256":
                metadata.setFileHashSHA256(value);
                break;

            case "File Modified":
                metadata.setFileModified(value);
                break;

            case "Proof Generated":
                metadata.setProofGenerated(value);
                break;

            case "CellInfo":
                metadata.setCellInfo(value);
                break;

            case "ScreenSize":
                metadata.setScreenSize(value);
                break;

            case "Location.Accuracy":
                getMetadataLocation(metadata).setAccuracy(Float.parseFloat(value));
                break;

            case "Location.Altitude":
                getMetadataLocation(metadata).setAltitude(Double.parseDouble(value));
                break;

            case "Location.Time":
                getMetadataLocation(metadata).setTimestamp(Long.parseLong(value));
                break;

            case "Location.Latitude":
                getMetadataLocation(metadata).setLatitude(Location.convert(value));
                break;

            case "Location.Longitude":
                getMetadataLocation(metadata).setLongitude(Location.convert(value));
                break;

            case "Location.Provider":
                getMetadataLocation(metadata).setProvider(value);
                break;

            case "Location.Speed":
                getMetadataLocation(metadata).setSpeed(Float.parseFloat(value));
                break;
        }
    }

    private static MyLocation getMetadataLocation(@NonNull Metadata metadata) {
        if (metadata.getMyLocation() == null) {
            metadata.setMyLocation(new MyLocation());
        }

        return metadata.getMyLocation();
    }

    private static boolean verifyDetachedSignature(Context context, String mediaFileName, String mediaFileSigName) throws Exception {
        boolean result = false;
        PgpUtils pgpUtils = PgpUtils.getInstance(context);

        InputStream in = new BufferedInputStream(new FileInputStream(mediaFileSigName));
        InputStream keyIn = new ByteArrayInputStream(pgpUtils.getPublicKey().getBytes());

        try {
            result = DetachedSignatureProcessor.verifySignature(mediaFileName, in, keyIn);
        } catch (Exception e) {
            Timber.d(e);
        }

        keyIn.close();
        in.close();

        return result;
    }
}
