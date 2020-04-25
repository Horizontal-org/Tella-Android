package rs.readahead.washington.mobile.data.database;


class D {
    /* DEFAULT TYPES FOR DATABASE */
    static final String INTEGER = " INTEGER ";
    static final String TEXT = " TEXT ";
    //static final String REAL = " REAL ";
    static final String BLOB = " BLOB ";
    //static final String DATE = " DATE ";

    /* DATABASE */
    static final String DATABASE_NAME = "tella.db";
    // 1=start,2=form media file status,3=updated flag in xforms
    // 4=xml form part status,5=media file hash,6=tella upload server
    static final int DATABASE_VERSION = 6;

    /* DATABASE TABLES */
    static final String T_COLLECT_SERVER = "t_collect_server";
    static final String T_COLLECT_BLANK_FORM = "t_collect_blank_xform";
    static final String T_COLLECT_FORM_INSTANCE = "t_collect_xform_instance";
    static final String T_MEDIA_FILE = "t_media_file";
    static final String T_COLLECT_FORM_INSTANCE_MEDIA_FILE = "t_collect_xform_instance_media_file";
    static final String T_SETTINGS = "t_settings";
    static final String T_TELLA_UPLOAD_SERVER = "t_tella_upload_server";

    /* DATABASE COLUMNS */
    static final String C_ID = "c_id";

    static final String C_MAIL = "c_mail";
    static final String C_NAME = "c_name";
    static final String C_PHONE = "c_phone";
    static final String C_UID = "c_uid";
    static final String C_METADATA = "c_metadata";
    static final String C_PATH = "c_path";
    static final String C_URL = "c_url";
    static final String C_USERNAME = "c_username";
    static final String C_PASSWORD = "c_password";
    static final String C_VERSION = "c_version";
    static final String C_HASH = "c_hash";
    //static final String C_DESCRIPTION_TEXT = "c_description_text";
    static final String C_DOWNLOAD_URL = "c_download_url";
    //static final String C_MANIFEST_URL = "c_manifest_url";
    static final String C_FORM_ID = "c_form_id";
    static final String C_COLLECT_SERVER_ID = "c_collect_server_id";
    static final String C_FORM_DEF = "c_form_def";
    static final String C_FORM_NAME = "c_form_name";
    static final String C_INSTANCE_NAME = "c_instance_name";
    static final String C_STATUS = "c_status";
    static final String C_UPDATED = "c_updated";
    static final String C_DOWNLOADED = "c_downloaded";
    static final String C_FAVORITE = "c_favorite";
    static final String C_THUMBNAIL = "c_thumbnail";
    static final String C_FILE_NAME = "c_file_name";
    static final String C_MEDIA_FILE_ID = "c_media_file_id";
    static final String C_CREATED = "c_created";
    static final String C_COLLECT_FORM_INSTANCE_ID = "c_collect_form_instance_id";
    static final String C_DURATION = "c_duration";
    static final String C_ANONYMOUS = "c_anonymous";
    static final String C_SIZE = "c_size";
    static final String C_INT_VALUE = "c_int_value";
    static final String C_TEXT_VALUE = "c_text_value";
    static final String C_CHECKED = "c_checked";
    static final String C_FORM_PART_STATUS = "c_form_part_status";

    static final String A_SERVER_NAME = "a_server_name";
    static final String A_COLLECT_BLANK_FORM_ID = "a_collect_blank_xform_id";
    static final String A_COLLECT_FORM_INSTANCE_ID = "a_collect_form_instance_id";
    static final String A_MEDIA_FILE_ID = "a_media_file_id";
    static final String A_SERVER_USERNAME = "a_server_username";
    static final String A_FORM_MEDIA_FILE_STATUS = "a_form_media_file_status";
}
