package org.horizontal.tella.mobile.data.database

/**
 * Database schema constants.
 * 
 * Note: These are compile-time constants used in SQL queries.
 * They should NOT be moved to resources as they need to be available
 * at compile time for type safety and SQL generation.
 */
object D {
    /* ========== DATABASE TYPES ========== */
    object Types {
        const val INTEGER = " INTEGER "
        const val TEXT = " TEXT "
        const val BLOB = " BLOB "
        // const val REAL = " REAL "
        // const val DATE = " DATE "
    }

    /* ========== DATABASE CONFIGURATION ========== */
    object Database {
        const val CIPHER3_DATABASE_NAME = "tella.db"
        const val DATABASE_NAME = "tella-v4.db"
        const val DATABASE_VERSION = 16
        const val MIN_DATABASE_VERSION = 1
    }

    /* ========== DATABASE TABLES ========== */
    object Tables {
        // Collect tables
        const val T_COLLECT_SERVER = "t_collect_server"
        const val T_COLLECT_BLANK_FORM = "t_collect_blank_xform"
        const val T_COLLECT_FORM_INSTANCE = "t_collect_xform_instance"
        const val T_COLLECT_FORM_INSTANCE_MEDIA_FILE = "t_collect_xform_instance_media_file"
        const val T_COLLECT_FORM_INSTANCE_VAULT_FILE = "t_collect_xform_instance_vault_file"

        // Report tables
        const val T_REPORT_FORM_INSTANCE = "t_report_form_instance"
        const val T_REPORT_FILES_UPLOAD = "t_report_files_upload"
        const val T_REPORT_INSTANCE_VAULT_FILE = "t_report_instance_vault_file"

        // Cloud service tables
        const val T_GOOGLE_DRIVE_FORM_INSTANCE = "t_google_drive_form_instance"
        const val T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE = "t_google_drive_instance_vault_file"
        const val T_DROPBOX_FORM_INSTANCE = "t_dropbox_form_instance"
        const val T_DROPBOX_INSTANCE_VAULT_FILE = "t_dropbox_instance_vault_file"
        const val T_NEXT_CLOUD_FORM_INSTANCE = "t_next_cloud_form_instance"
        const val T_NEXT_CLOUD_INSTANCE_VAULT_FILE = "t_next_cloud_instance_vault_file"

        // Media tables
        const val T_MEDIA_FILE = "t_media_file"
        const val T_MEDIA_FILE_UPLOAD = "t_media_file_upload"

        // Settings
        const val T_SETTINGS = "t_settings"

        // Upload server
        const val T_TELLA_UPLOAD_SERVER = "t_tella_upload_server"

        // Uwazi tables
        const val T_UWAZI_SERVER = "t_uwazi_server"
        const val T_UWAZI_BLANK_TEMPLATES = "t_uwazi_collect_blank_templates"
        const val T_UWAZI_ENTITY_INSTANCES = "t_uwazi_entity_instances"
        const val T_UWAZI_ENTITY_INSTANCE_VAULT_FILE = "t_uwazi_entity_instance_vault_file"

        // Resources
        const val T_RESOURCES = "t_resources"

        // NextCloud
        const val T_NEXT_CLOUD = "t_next_cloud"

        // Google Drive
        const val T_GOOGLE_DRIVE = "t_google_drive"

        // DropBox
        const val T_DROPBOX = "t_dropbox"

        // Feedback
        const val T_FEEDBACK = "t_feedback"
    }

    /* ========== DATABASE COLUMNS ========== */
    object Columns {
        // Common columns
        const val C_ID = "c_id"
        const val C_NAME = "c_name"
        const val C_URL = "c_url"
        const val C_USERNAME = "c_username"
        const val C_PASSWORD = "c_password"
        const val C_UID = "c_uid"
        const val C_METADATA = "c_metadata"
        const val C_PATH = "c_path"
        const val C_VERSION = "c_version"
        const val C_HASH = "c_hash"
        const val C_CREATED = "c_created"
        const val C_UPDATED = "c_updated"
        const val C_STATUS = "c_status"
        const val C_SIZE = "c_size"
        const val C_TYPE = "c_type"
        const val C_TITLE = "c_title"
        const val C_DESCRIPTION_TEXT = "c_description_text"

        // User/Account columns
        const val C_MAIL = "c_mail"
        const val C_PHONE = "c_phone"
        const val C_USERID = "c_userid"
        const val C_ACCESS_TOKEN = "c_access_token"
        const val C_CONNECT_COOKIES = "c_connect_cookies"
        const val C_LOCALE_COOKIES = "c_locale_cookies"

        // Server/Project columns
        const val C_SERVER_ID = "c_server_id"
        const val C_PROJECT_ID = "c_project_id"
        const val C_PROJECT_NAME = "c_project_name"
        const val C_PROJECT_SLUG = "c_project_slug"
        const val C_ACTIVATED_METADATA = "c_activated_metadata"
        const val C_BACKGROUND_UPLOAD = "c_background_upload"
        const val C_AUTO_UPLOAD = "c_auto_upload"
        const val C_AUTO_DELETE = "c_auto_delete"

        // Form columns
        const val C_FORM_ID = "c_form_id"
        const val C_FORM_DEF = "c_form_def"
        const val C_FORM_NAME = "c_form_name"
        const val C_FORM_PART_STATUS = "c_form_part_status"
        const val C_COLLECT_SERVER_ID = "c_collect_server_id"
        const val C_COLLECT_FORM_INSTANCE_ID = "c_collect_form_instance_id"

        // Report columns
        const val C_REPORT_SERVER_ID = "c_report_server_id"
        const val C_REPORT_API_ID = "c_report_api_id"
        const val C_REPORT_FILE_ID = "c_report_file_id"
        const val C_REPORT_INSTANCE_ID = "c_report_instance_id"
        const val C_CURRENT_UPLOAD = "C_CURRENT_UPLOAD"

        // Media columns
        const val C_MEDIA_FILE_ID = "c_media_file_id"
        const val C_FILE_NAME = "c_file_name"
        const val C_THUMBNAIL = "c_thumbnail"
        const val C_DURATION = "c_duration"
        const val C_ANONYMOUS = "c_anonymous"

        // Upload columns
        const val C_UPLOADED = "c_uploaded"
        const val C_UPLOADED_SIZE = "c_uploaded_size"
        const val C_RETRY_COUNT = "c_retry_count"
        const val C_INCLUDE_METADATA = "c_include_metadata"
        const val C_MANUAL_UPLOAD = "c_manual_upload"
        const val C_SET = "c_set"

        // Instance columns
        const val C_INSTANCE_NAME = "c_instance_name"
        const val C_DOWNLOADED = "c_downloaded"
        const val C_FAVORITE = "c_favorite"
        const val C_DOWNLOAD_URL = "c_download_url"
        const val C_CHECKED = "c_checked"

        // Vault columns
        const val C_VAULT_FILE_ID = "c_vault_file_id"

        // Settings columns
        const val C_INT_VALUE = "c_int_value"
        const val C_TEXT_VALUE = "c_text_value"

        // Template columns
        const val C_TEMPLATE = "c_template"
        const val C_TEMPLATE_ENTITY = "c_template_entity"

        // Uwazi columns
        const val C_UWAZI_SERVER_ID = "c_uwazi_server_id"
        const val C_UWAZI_ENTITY_INSTANCE_ID = "c_uwazi_entity_instance_id"

        // Resources columns
        const val C_RESOURCES_ID = "c_resources_id"
        const val C_RESOURCES_TITLE = "c_resources_title"
        const val C_RESOURCES_FILE_NAME = "c_resources_file_name"
        const val C_RESOURCES_SIZE = "c_resources_size"
        const val C_RESOURCES_CREATED = "c_resources_created"
        const val C_RESOURCES_SAVED = "c_resources_saved"
        const val C_RESOURCES_PROJECT = "c_resources_project"
        const val C_RESOURCES_FILE_ID = "c_resources_file_id"

        // NextCloud columns
        const val C_NEXT_CLOUD_SERVER_NAME = "c_next_cloud_server_name"
        const val C_NEXT_CLOUD_FOLDER_ID = "c_next_cloud_folder_id"
        const val C_NEXT_CLOUD_FOLDER_NAME = "c_next_cloud_folder_name"
        const val C_NEXT_CLOUD_USER_ID = "c_next_cloud_user_id"

        // Google Drive columns
        const val C_GOOGLE_DRIVE_FOLDER_ID = "c_google_drive_folder_id"
        const val C_GOOGLE_DRIVE_FOLDER_NAME = "c_google_drive_folder_name"
        const val C_GOOGLE_DRIVE_SERVER_NAME = "c_google_drive_server_name"

        // DropBox columns
        const val C_DROPBOX_ACCESS_TOKEN = "c_dropbox_access_token"
        const val C_DROPBOX_SERVER_NAME = "c_dropbox_server_name"
    }

    /* ========== ALIASES ========== */
    object Aliases {
        const val A_TELLA_UPLOAD_INSTANCE_ID = "a_tella_upload_instance_id"
        const val A_UWAZI_ENTITY_INSTANCE_ID = "a_uwazi_entity_instance_id"
        const val A_SERVER_NAME = "a_server_name"
        const val A_COLLECT_BLANK_FORM_ID = "a_collect_blank_xform_id"
        const val A_COLLECT_FORM_INSTANCE_ID = "a_collect_form_instance_id"
        const val A_MEDIA_FILE_ID = "a_media_file_id"
        const val A_SERVER_USERNAME = "a_server_username"
        const val A_FORM_MEDIA_FILE_STATUS = "a_form_media_file_status"
    }

    // ========== BACKWARD COMPATIBILITY ==========
    // All constants exposed as public static fields for Java compatibility
    // They delegate to the new organized structure
    
    // Types
    @JvmField val INTEGER = Types.INTEGER
    @JvmField val TEXT = Types.TEXT
    @JvmField val BLOB = Types.BLOB

    // Database
    @JvmField val CIPHER3_DATABASE_NAME = Database.CIPHER3_DATABASE_NAME
    @JvmField val DATABASE_NAME = Database.DATABASE_NAME
    @JvmField val DATABASE_VERSION = Database.DATABASE_VERSION
    @JvmField val MIN_DATABASE_VERSION = Database.MIN_DATABASE_VERSION

    // Tables - All tables
    @JvmField val T_COLLECT_SERVER = Tables.T_COLLECT_SERVER
    @JvmField val T_COLLECT_BLANK_FORM = Tables.T_COLLECT_BLANK_FORM
    @JvmField val T_COLLECT_FORM_INSTANCE = Tables.T_COLLECT_FORM_INSTANCE
    @JvmField val T_COLLECT_FORM_INSTANCE_MEDIA_FILE = Tables.T_COLLECT_FORM_INSTANCE_MEDIA_FILE
    @JvmField val T_COLLECT_FORM_INSTANCE_VAULT_FILE = Tables.T_COLLECT_FORM_INSTANCE_VAULT_FILE
    @JvmField val T_REPORT_FORM_INSTANCE = Tables.T_REPORT_FORM_INSTANCE
    @JvmField val T_REPORT_FILES_UPLOAD = Tables.T_REPORT_FILES_UPLOAD
    @JvmField val T_REPORT_INSTANCE_VAULT_FILE = Tables.T_REPORT_INSTANCE_VAULT_FILE
    @JvmField val T_GOOGLE_DRIVE_FORM_INSTANCE = Tables.T_GOOGLE_DRIVE_FORM_INSTANCE
    @JvmField val T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE = Tables.T_GOOGLE_DRIVE_INSTANCE_VAULT_FILE
    @JvmField val T_DROPBOX_FORM_INSTANCE = Tables.T_DROPBOX_FORM_INSTANCE
    @JvmField val T_DROPBOX_INSTANCE_VAULT_FILE = Tables.T_DROPBOX_INSTANCE_VAULT_FILE
    @JvmField val T_NEXT_CLOUD_FORM_INSTANCE = Tables.T_NEXT_CLOUD_FORM_INSTANCE
    @JvmField val T_NEXT_CLOUD_INSTANCE_VAULT_FILE = Tables.T_NEXT_CLOUD_INSTANCE_VAULT_FILE
    @JvmField val T_MEDIA_FILE = Tables.T_MEDIA_FILE
    @JvmField val T_MEDIA_FILE_UPLOAD = Tables.T_MEDIA_FILE_UPLOAD
    @JvmField val T_SETTINGS = Tables.T_SETTINGS
    @JvmField val T_TELLA_UPLOAD_SERVER = Tables.T_TELLA_UPLOAD_SERVER
    @JvmField val T_UWAZI_SERVER = Tables.T_UWAZI_SERVER
    @JvmField val T_UWAZI_BLANK_TEMPLATES = Tables.T_UWAZI_BLANK_TEMPLATES
    @JvmField val T_UWAZI_ENTITY_INSTANCES = Tables.T_UWAZI_ENTITY_INSTANCES
    @JvmField val T_UWAZI_ENTITY_INSTANCE_VAULT_FILE = Tables.T_UWAZI_ENTITY_INSTANCE_VAULT_FILE
    @JvmField val T_RESOURCES = Tables.T_RESOURCES
    @JvmField val T_NEXT_CLOUD = Tables.T_NEXT_CLOUD
    @JvmField val T_GOOGLE_DRIVE = Tables.T_GOOGLE_DRIVE
    @JvmField val T_DROPBOX = Tables.T_DROPBOX
    @JvmField val T_FEEDBACK = Tables.T_FEEDBACK

    // Columns - All columns
    @JvmField val C_ID = Columns.C_ID
    @JvmField val C_NAME = Columns.C_NAME
    @JvmField val C_URL = Columns.C_URL
    @JvmField val C_USERNAME = Columns.C_USERNAME
    @JvmField val C_PASSWORD = Columns.C_PASSWORD
    @JvmField val C_UID = Columns.C_UID
    @JvmField val C_METADATA = Columns.C_METADATA
    @JvmField val C_PATH = Columns.C_PATH
    @JvmField val C_VERSION = Columns.C_VERSION
    @JvmField val C_HASH = Columns.C_HASH
    @JvmField val C_CREATED = Columns.C_CREATED
    @JvmField val C_UPDATED = Columns.C_UPDATED
    @JvmField val C_STATUS = Columns.C_STATUS
    @JvmField val C_SIZE = Columns.C_SIZE
    @JvmField val C_TYPE = Columns.C_TYPE
    @JvmField val C_TITLE = Columns.C_TITLE
    @JvmField val C_DESCRIPTION_TEXT = Columns.C_DESCRIPTION_TEXT
    @JvmField val C_MAIL = Columns.C_MAIL
    @JvmField val C_PHONE = Columns.C_PHONE
    @JvmField val C_USERID = Columns.C_USERID
    @JvmField val C_ACCESS_TOKEN = Columns.C_ACCESS_TOKEN
    @JvmField val C_CONNECT_COOKIES = Columns.C_CONNECT_COOKIES
    @JvmField val C_LOCALE_COOKIES = Columns.C_LOCALE_COOKIES
    @JvmField val C_SERVER_ID = Columns.C_SERVER_ID
    @JvmField val C_PROJECT_ID = Columns.C_PROJECT_ID
    @JvmField val C_PROJECT_NAME = Columns.C_PROJECT_NAME
    @JvmField val C_PROJECT_SLUG = Columns.C_PROJECT_SLUG
    @JvmField val C_ACTIVATED_METADATA = Columns.C_ACTIVATED_METADATA
    @JvmField val C_BACKGROUND_UPLOAD = Columns.C_BACKGROUND_UPLOAD
    @JvmField val C_AUTO_UPLOAD = Columns.C_AUTO_UPLOAD
    @JvmField val C_AUTO_DELETE = Columns.C_AUTO_DELETE
    @JvmField val C_FORM_ID = Columns.C_FORM_ID
    @JvmField val C_FORM_DEF = Columns.C_FORM_DEF
    @JvmField val C_FORM_NAME = Columns.C_FORM_NAME
    @JvmField val C_FORM_PART_STATUS = Columns.C_FORM_PART_STATUS
    @JvmField val C_COLLECT_SERVER_ID = Columns.C_COLLECT_SERVER_ID
    @JvmField val C_COLLECT_FORM_INSTANCE_ID = Columns.C_COLLECT_FORM_INSTANCE_ID
    @JvmField val C_REPORT_SERVER_ID = Columns.C_REPORT_SERVER_ID
    @JvmField val C_REPORT_API_ID = Columns.C_REPORT_API_ID
    @JvmField val C_REPORT_FILE_ID = Columns.C_REPORT_FILE_ID
    @JvmField val C_REPORT_INSTANCE_ID = Columns.C_REPORT_INSTANCE_ID
    @JvmField val C_CURRENT_UPLOAD = Columns.C_CURRENT_UPLOAD
    @JvmField val C_MEDIA_FILE_ID = Columns.C_MEDIA_FILE_ID
    @JvmField val C_FILE_NAME = Columns.C_FILE_NAME
    @JvmField val C_THUMBNAIL = Columns.C_THUMBNAIL
    @JvmField val C_DURATION = Columns.C_DURATION
    @JvmField val C_ANONYMOUS = Columns.C_ANONYMOUS
    @JvmField val C_UPLOADED = Columns.C_UPLOADED
    @JvmField val C_UPLOADED_SIZE = Columns.C_UPLOADED_SIZE
    @JvmField val C_RETRY_COUNT = Columns.C_RETRY_COUNT
    @JvmField val C_INCLUDE_METADATA = Columns.C_INCLUDE_METADATA
    @JvmField val C_MANUAL_UPLOAD = Columns.C_MANUAL_UPLOAD
    @JvmField val C_SET = Columns.C_SET
    @JvmField val C_INSTANCE_NAME = Columns.C_INSTANCE_NAME
    @JvmField val C_DOWNLOADED = Columns.C_DOWNLOADED
    @JvmField val C_FAVORITE = Columns.C_FAVORITE
    @JvmField val C_DOWNLOAD_URL = Columns.C_DOWNLOAD_URL
    @JvmField val C_CHECKED = Columns.C_CHECKED
    @JvmField val C_VAULT_FILE_ID = Columns.C_VAULT_FILE_ID
    @JvmField val C_INT_VALUE = Columns.C_INT_VALUE
    @JvmField val C_TEXT_VALUE = Columns.C_TEXT_VALUE
    @JvmField val C_TEMPLATE = Columns.C_TEMPLATE
    @JvmField val C_TEMPLATE_ENTITY = Columns.C_TEMPLATE_ENTITY
    @JvmField val C_UWAZI_SERVER_ID = Columns.C_UWAZI_SERVER_ID
    @JvmField val C_UWAZI_ENTITY_INSTANCE_ID = Columns.C_UWAZI_ENTITY_INSTANCE_ID
    @JvmField val C_RESOURCES_ID = Columns.C_RESOURCES_ID
    @JvmField val C_RESOURCES_TITLE = Columns.C_RESOURCES_TITLE
    @JvmField val C_RESOURCES_FILE_NAME = Columns.C_RESOURCES_FILE_NAME
    @JvmField val C_RESOURCES_SIZE = Columns.C_RESOURCES_SIZE
    @JvmField val C_RESOURCES_CREATED = Columns.C_RESOURCES_CREATED
    @JvmField val C_RESOURCES_SAVED = Columns.C_RESOURCES_SAVED
    @JvmField val C_RESOURCES_PROJECT = Columns.C_RESOURCES_PROJECT
    @JvmField val C_RESOURCES_FILE_ID = Columns.C_RESOURCES_FILE_ID
    @JvmField val C_NEXT_CLOUD_SERVER_NAME = Columns.C_NEXT_CLOUD_SERVER_NAME
    @JvmField val C_NEXT_CLOUD_FOLDER_ID = Columns.C_NEXT_CLOUD_FOLDER_ID
    @JvmField val C_NEXT_CLOUD_FOLDER_NAME = Columns.C_NEXT_CLOUD_FOLDER_NAME
    @JvmField val C_NEXT_CLOUD_USER_ID = Columns.C_NEXT_CLOUD_USER_ID
    @JvmField val C_GOOGLE_DRIVE_FOLDER_ID = Columns.C_GOOGLE_DRIVE_FOLDER_ID
    @JvmField val C_GOOGLE_DRIVE_FOLDER_NAME = Columns.C_GOOGLE_DRIVE_FOLDER_NAME
    @JvmField val C_GOOGLE_DRIVE_SERVER_NAME = Columns.C_GOOGLE_DRIVE_SERVER_NAME
    @JvmField val C_DROPBOX_ACCESS_TOKEN = Columns.C_DROPBOX_ACCESS_TOKEN
    @JvmField val C_DROPBOX_SERVER_NAME = Columns.C_DROPBOX_SERVER_NAME

    // Aliases - All aliases
    @JvmField val A_TELLA_UPLOAD_INSTANCE_ID = Aliases.A_TELLA_UPLOAD_INSTANCE_ID
    @JvmField val A_UWAZI_ENTITY_INSTANCE_ID = Aliases.A_UWAZI_ENTITY_INSTANCE_ID
    @JvmField val A_SERVER_NAME = Aliases.A_SERVER_NAME
    @JvmField val A_COLLECT_BLANK_FORM_ID = Aliases.A_COLLECT_BLANK_FORM_ID
    @JvmField val A_COLLECT_FORM_INSTANCE_ID = Aliases.A_COLLECT_FORM_INSTANCE_ID
    @JvmField val A_MEDIA_FILE_ID = Aliases.A_MEDIA_FILE_ID
    @JvmField val A_SERVER_USERNAME = Aliases.A_SERVER_USERNAME
    @JvmField val A_FORM_MEDIA_FILE_STATUS = Aliases.A_FORM_MEDIA_FILE_STATUS
}
