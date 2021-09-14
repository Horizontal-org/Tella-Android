package rs.readahead.washington.mobile.util;


public class C {
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    public static final String GOOGLE_MAPS_TEST = "maps.google.com/maps?q=";
    public static final String MEDIA_DIR = "media";
    public static final String METADATA_DIR = "metadata";
    public static final String TMP_DIR = "tmp";
    public static final String TRAIN_DIR = "train";
    public static final String OPEN_ROSA_XML_PART_NAME = "xml_submission_file";

    // onActivityResult requestCode
    public static final int PICKED_IMAGE                = 10001;
    public static final int CAPTURED_IMAGE              = 10002;
    public static final int CAPTURED_VIDEO              = 10003;
    public static final int RECORDED_AUDIO              = 10004;
    public static final int IMPORT_IMAGE                = 10009;
    public static final int IMPORT_VIDEO                = 10010;
    public static final int IMPORT_MEDIA                = 10011;
    public static final int CAMERA_CAPTURE              = 10012;
    public static final int START_CAMERA_CAPTURE        = 10013; // return from location settings handling
    public static final int START_AUDIO_RECORD          = 10014; // return from location settings handling
    public static final int RECIPIENT_IDS               = 10015;
    public static final int MEDIA_FILE_IDS              = 10017;
    public static final int MEDIA_FILE_ID               = 10018;
    public static final int SELECTED_LOCATION           = 10019;
    public static final int GPS_PROVIDER                = 10020;
    public static final int IMPORT_MULTIPLE_FILES       = 10021;
    public static final int RECORD_REQUEST_CODE         = 10022;

    // "global" intent keys
    public static final String CAPTURED_MEDIA_FILE_ID = "cmfi";
    public static final String SMS_SENT = "SMS_SENT";
    public static final String SMS_DELIVERED = "SMS_DELIVERED";

    //A day in milliseconds
    public static final long DAY = 86400000;

    //An hour in milliseconds
    public static final long UPLOAD_SET_DURATION = 3600000;
}
