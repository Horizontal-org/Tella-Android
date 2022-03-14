package rs.readahead.washington.mobile.data.uwazi;

public class UwaziConstants {

    /** Empty strig representation */
    public static final String EMPTY_STRING = "";

    /** Index for no selection */
    public static final int NO_SELECTION = -1;

    /** ID not set to a value */
    public static final int NULL_ID = -1;

    /** Connection type not specified */
    public static final int CONNECTION_NONE = 0;

    /** Infrared connection */
    public static final int CONNECTION_INFRARED = 1;

    /** Bluetooth connection */
    public static final int CONNECTION_BLUETOOTH = 2;

    /** Data cable connection. Can be USB or Serial */
    public static final int CONNECTION_CABLE = 3;

    /** Over The Air or HTTP Connection */
    public static final int CONNECTION_OTA = 4;

    public static final int CONTROL_UNTYPED = -1;
    public static final int CONTROL_INPUT = 1;
    public static final int CONTROL_SELECT_ONE = 2;
    public static final int CONTROL_SELECT_MULTI = 3;
    public static final int CONTROL_TEXTAREA = 4;
    public static final int CONTROL_SECRET = 5;
    public static final int CONTROL_RANGE = 6;
    public static final int CONTROL_UPLOAD = 7;
    public static final int CONTROL_SUBMIT = 8;
    public static final int CONTROL_TRIGGER = 9;
    public static final int CONTROL_IMAGE_CHOOSE = 10;
    public static final int CONTROL_LABEL = 11;
    public static final int CONTROL_AUDIO_CAPTURE = 12;
    public static final int CONTROL_VIDEO_CAPTURE = 13;
    public static final int CONTROL_OSM_CAPTURE = 14;
    public static final int CONTROL_FILE_CAPTURE = 15; // generic upload

    public static final String UWAZI_DATATYPE_TEXT = "text";
    public static final String UWAZI_DATATYPE_NUMERIC = "numeric";
    public static final String UWAZI_DATATYPE_SELECT = "select";
    public static final String UWAZI_DATATYPE_MULTISELECT = "multiselect";
    public static final String UWAZI_DATATYPE_DATE = "date";
    public static final String UWAZI_DATATYPE_DATERANGE = "daterange";
    public static final String UWAZI_DATATYPE_MULTIDATE = "multidate";
    public static final String UWAZI_DATATYPE_MULTIDATERANGE = "multidaterange";
    public static final String UWAZI_DATATYPE_MARKDOWN = "markdown";
    public static final String UWAZI_DATATYPE_LINK = "link";
    public static final String UWAZI_DATATYPE_IMAGE = "image";
    public static final String UWAZI_DATATYPE_PREVIEW = "preview";
    public static final String UWAZI_DATATYPE_MEDIA = "media";
    public static final String UWAZI_DATATYPE_GEOLOCATION = "geolocation";
    public static final String UWAZI_DATATYPE_MULTIFILES = "multifiles";
    public static final String UWAZI_DATATYPE_MULTIPDFFILES = "multipdffiles";

    /** constants for xform tags */
    public static final String XFTAG_UPLOAD = "upload";
}
