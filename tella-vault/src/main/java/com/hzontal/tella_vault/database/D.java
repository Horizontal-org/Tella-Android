package com.hzontal.tella_vault.database;


class D {
    /* DEFAULT TYPES FOR DATABASE */
    static final String INTEGER = " INTEGER ";
    static final String TEXT = " TEXT ";
    //static final String REAL = " REAL ";
    static final String BLOB = " BLOB ";
    //static final String DATE = " DATE ";

    /* DATABASE */
    static final String DATABASE_NAME = "tella-vault.db";
    static final int DATABASE_VERSION = 1;

    /* DATABASE TABLES */
    static final String T_VAULT_FILE = "t_vault_file";

    /* DATABASE COLUMNS */
    static final String C_ID = "c_id";
    static final String C_PARENT_ID = "c_parent_id";
    static final String C_UID = "c_uid";
    static final String C_TYPE = "c_type";
    static final String C_HASH = "c_hash";
    static final String C_METADATA = "c_metadata";
    static final String C_PATH = "c_path";
    static final String C_THUMBNAIL = "c_thumbnail";
    static final String C_NAME = "c_name";
    static final String C_CREATED = "c_created";
    static final String C_DURATION = "c_duration";
    static final String C_ANONYMOUS = "c_anonymous";
    static final String C_SIZE = "c_size";
    static final String C_MIME_TYPE = "c_mime_type";
}
