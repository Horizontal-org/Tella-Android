package org.horizontal.tella.mobile.data.database;


class D {
    /* DEFAULT TYPES FOR DATABASE */
    static final String INTEGER = " INTEGER ";
    static final String TEXT = " TEXT ";
    //static final String REAL = " REAL ";
    static final String BLOB = " BLOB ";
    //static final String DATE = " DATE ";

    /* DATABASE */
    public static final String CIPHER3_DATABASE_NAME = "tella.db";
    public static final String DATABASE_NAME = "tella-v4.db";


    // 1=start,2=form media file status,3=updated flag in xforms
    // 4=xml form part status,5=media file hash,6=tella upload server,7=file uploads table
    static final int DATABASE_VERSION = 16;
    static final int MIN_DATABASE_VERSION = 1;

    /* DATABASE TABLES */
    static final String T_COLLECT_SERVER = "t_collect_server";
    static final String T_COLLECT_BLANK_FORM = "t_collect_blank_xform";
    static final String T_COLLECT_FORM_INSTANCE = "t_collect_xform_instance";
    static final String T_REPORT_FORM_INSTANCE = "t_report_form_instance";
    static final String T_GOOGLE_DRIVE_FORM_INSTANCE = "t_google_drive_form_instance";
    static final String T_DROPBOX_FORM_INSTANCE = "t_dropbox_form_instance";
    static final String T_NEXT_CLOUD_FORM_INSTANCE = "t_next_cloud_form_instance";
    static final String T_MEDIA_FILE = "t_media_file";
    static final String T_COLLECT_FORM_INSTANCE_MEDIA_FILE = "t_collect_xform_instance_media_file";
    static final String T_SETTINGS = "t_settings";
    static final String T_TELLA_UPLOAD_SERVER = "t_tella_upload_server";
    static final String T_MEDIA_FILE_UPLOAD = "t_media_file_upload";
    static final String T_REPORT_FILES_UPLOAD = "t_report_files_upload";
    static final String T_COLLECT_FORM_INSTANCE_VAULT_FILE = "t_collect_xform_instance_vault_file";
    static final String A_TELLA_UPLOAD_INSTANCE_ID = "a_tella_upload_instance_id";

    /* UWAZI DATABASE TABLE*/
    static final String T_UWAZI_SERVER = "t_uwazi_server";
    static final String T_UWAZI_BLANK_TEMPLATES = "t_uwazi_collect_blank_templates";
    static final String T_UWAZI_ENTITY_INSTANCES = "t_uwazi_entity_instances";
    static final String A_UWAZI_ENTITY_INSTANCE_ID = "a_uwazi_entity_instance_id";

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
    static final String C_USERID = "c_userid";
    static final String C_PASSWORD = "c_password";
    static final String C_VERSION = "c_version";
    static final String C_HASH = "c_hash";
    static final String C_CONNECT_COOKIES = "c_connect_cookies";
    static final String C_LOCALE_COOKIES = "c_locale_cookies";
    static final String C_ACCESS_TOKEN = "c_access_token";
    static final String C_ACTIVATED_METADATA = "c_activated_metadata";
    static final String C_BACKGROUND_UPLOAD = "c_background_upload";
    static final String C_AUTO_UPLOAD = "c_auto_upload";
    static final String C_AUTO_DELETE = "c_auto_delete";
    static final String C_PROJECT_ID = "c_project_id";
    static final String C_PROJECT_NAME = "c_project_name";
    static final String C_PROJECT_SLUG = "c_project_slug";

    static final String C_DESCRIPTION_TEXT = "c_description_text";
    static final String C_DOWNLOAD_URL = "c_download_url";
    //static final String C_MANIFEST_URL = "c_manifest_url";
    static final String C_FORM_ID = "c_form_id";
    static final String C_COLLECT_SERVER_ID = "c_collect_server_id";
    static final String C_REPORT_SERVER_ID = "c_report_server_id";
    static final String C_REPORT_API_ID = "c_report_api_id";

    static final String C_CURRENT_UPLOAD = "C_CURRENT_UPLOAD";
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
    static final String C_REPORT_FILE_ID = "c_report_file_id";
    static final String C_CREATED = "c_created";
    static final String C_COLLECT_FORM_INSTANCE_ID = "c_collect_form_instance_id";
    static final String C_DURATION = "c_duration";
    static final String C_ANONYMOUS = "c_anonymous";
    static final String C_SIZE = "c_size";
    static final String C_INT_VALUE = "c_int_value";
    static final String C_TEXT_VALUE = "c_text_value";
    static final String C_CHECKED = "c_checked";
    static final String C_FORM_PART_STATUS = "c_form_part_status";
    static final String C_UPLOADED = "c_uploaded";
    static final String C_SET = "c_set";
    static final String C_RETRY_COUNT = "c_retry_count";
    static final String C_INCLUDE_METADATA = "c_include_metadata";
    static final String C_SERVER_ID = "c_server_id";
    static final String C_MANUAL_UPLOAD = "c_manual_upload";
    static final String C_VAULT_FILE_ID = "c_vault_file_id";
    static final String C_TEMPLATE = "c_template";
    static final String C_TITLE = "c_title";
    static final String C_TYPE = "c_type";
    static final String T_UWAZI_ENTITY_INSTANCE_VAULT_FILE = "t_uwazi_entity_instance_vault_file";
    static final String C_UWAZI_ENTITY_INSTANCE_ID = "c_uwazi_entity_instance_id";
    static final String T_REPORT_INSTANCE_VAULT_FILE = "t_report_instance_vault_file";
    static final String T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE = "t_google_drive_instance_vault_file";
    static final String T_NEXT_CLOUD_INSTANCE_VAULT_FILE = "t_next_cloud_instance_vault_file";
    static final String T_DROPBOX_INSTANCE_VAULT_FILE = "t_dropbox_instance_vault_file";
    static final String C_REPORT_INSTANCE_ID = "c_report_instance_id";
    static final String C_UPLOADED_SIZE = "c_uploaded_size";

    //Uwazi cloumns
    static final String C_UWAZI_SERVER_ID = "c_uwazi_server_id";
    static final String C_TEMPLATE_ENTITY = "c_template_entity";
    static final String A_SERVER_NAME = "a_server_name";
    static final String A_COLLECT_BLANK_FORM_ID = "a_collect_blank_xform_id";
    static final String A_COLLECT_FORM_INSTANCE_ID = "a_collect_form_instance_id";
    static final String A_MEDIA_FILE_ID = "a_media_file_id";
    static final String A_SERVER_USERNAME = "a_server_username";
    static final String A_FORM_MEDIA_FILE_STATUS = "a_form_media_file_status";

    //Feedback
    static final String T_FEEDBACK = "t_feedback";


    /* RESOURCES */
    static final String T_RESOURCES = "t_resources";
    static final String C_RESOURCES_ID = "c_resources_id";
    static final String C_RESOURCES_TITLE = "c_resources_title";
    static final String C_RESOURCES_FILE_NAME = "c_resources_file_name";
    static final String C_RESOURCES_SIZE = "c_resources_size";
    static final String C_RESOURCES_CREATED = "c_resources_created";
    static final String C_RESOURCES_SAVED = "c_resources_saved";
    static final String C_RESOURCES_PROJECT = "c_resources_project";
    static final String C_RESOURCES_FILE_ID = "c_resources_file_id";

    /* NEXT CLOUD */
    static final String T_NEXT_CLOUD = "t_next_cloud";
    static final String C_NEXT_CLOUD_SERVER_NAME = "c_next_cloud_server_name";
    static final String C_NEXT_CLOUD_FOLDER_ID = "c_next_cloud_folder_id";
    static final String C_NEXT_CLOUD_FOLDER_NAME = "c_next_cloud_folder_name";
    static final String C_NEXT_CLOUD_USER_ID = "c_next_cloud_user_id";

    /* GOOGLE DRIVE */
    static final String T_GOOGLE_DRIVE = "t_google_drive";
    static final String C_GOOGLE_DRIVE_FOLDER_ID = "c_google_drive_folder_id";
    static final String C_GOOGLE_DRIVE_FOLDER_NAME = "c_google_drive_folder_name";
    static final String C_GOOGLE_DRIVE_SERVER_NAME = "c_google_drive_server_name";

    /* DropBox*/
    static final String T_DROPBOX = "t_dropbox";
    static final String C_DROPBOX_ACCESS_TOKEN = "c_dropbox_access_token";
    static final String C_DROPBOX_SERVER_NAME = "c_dropbox_server_name";

}
