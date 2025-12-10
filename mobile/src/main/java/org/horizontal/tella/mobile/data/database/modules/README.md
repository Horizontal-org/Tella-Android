# Database Modular Architecture

## Overview

The database has been refactored into a modular architecture to improve maintainability, testability, and separation of concerns. The architecture uses a **single main database** (`tella-v4.db`) with **modular table definitions** organized by feature.

## Architecture Decision

**Decision: Single Main Database with Modular Organization**

We chose a single main database approach rather than separate databases per module because:

1. **Referential Integrity**: Many tables have foreign key relationships across modules (e.g., media files referenced by forms, reports, and cloud services)
2. **Shared Entities**: Core entities like media files and vault files are used across multiple features
3. **Transaction Support**: Single database allows for atomic operations across modules
4. **Simplified Management**: One database file is easier to backup, migrate, and manage
5. **Performance**: Single database reduces overhead from multiple database connections

## Module Structure

The database is organized into the following modules:

### 1. **Forms Module** (`FormsDatabaseModule`)
- **Purpose**: Manages Collect forms functionality
- **Tables**:
  - `t_collect_server` - Collect servers
  - `t_collect_blank_xform` - Blank forms
  - `t_collect_xform_instance` - Form instances
  - `t_collect_xform_instance_media_file` - Form instance media files
  - `t_collect_xform_instance_vault_file` - Form instance vault files
- **Min DB Version**: 1

### 2. **Media Module** (`MediaDatabaseModule`)
- **Purpose**: Manages media files
- **Tables**:
  - `t_media_file` - Media files
  - `t_media_file_upload` - Media file uploads
- **Min DB Version**: 1

### 3. **Reports Module** (`ReportsDatabaseModule`)
- **Purpose**: Manages reports and Tella upload servers
- **Tables**:
  - `t_tella_upload_server` - Tella upload servers
  - `t_report_form_instance` - Report form instances
  - `t_report_instance_vault_file` - Report instance vault files
  - `t_report_files_upload` - Report file uploads
- **Min DB Version**: 6

### 4. **Uwazi Module** (`UwaziDatabaseModule`)
- **Purpose**: Manages Uwazi integration
- **Tables**:
  - `t_uwazi_server` - Uwazi servers
  - `t_uwazi_collect_blank_templates` - Uwazi blank templates
  - `t_uwazi_entity_instances` - Uwazi entity instances
  - `t_uwazi_entity_instance_vault_file` - Uwazi entity instance vault files
- **Min DB Version**: 8

### 5. **Cloud Module** (`CloudDatabaseModule`)
- **Purpose**: Manages cloud service integrations
- **Tables**:
  - Google Drive: `t_google_drive`, `t_google_drive_form_instance`, `t_google_drive_instance_vault_file`
  - Dropbox: `t_dropbox`, `t_dropbox_form_instance`, `t_dropbox_instance_vault_file`
  - NextCloud: `t_next_cloud`, `t_next_cloud_form_instance`, `t_next_cloud_instance_vault_file`
- **Min DB Version**: 13

### 6. **Settings Module** (`SettingsDatabaseModule`)
- **Purpose**: Manages application settings
- **Tables**:
  - `t_settings` - Application settings
- **Min DB Version**: 1

### 7. **Resources Module** (`ResourcesDatabaseModule`)
- **Purpose**: Manages resources
- **Tables**:
  - `t_resources` - Resources
- **Min DB Version**: 12

### 8. **Feedback Module** (`FeedbackDatabaseModule`)
- **Purpose**: Manages feedback
- **Tables**:
  - `t_feedback` - Feedback
- **Min DB Version**: 11

## Module Interface

All modules implement the `DatabaseModule` interface:

```java
public interface DatabaseModule {
    String getModuleName();
    void onCreate(SQLiteDatabase db);
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    int getMinDatabaseVersion();
}
```

## Database Helper

The `HorizontalSQLiteOpenHelper` now:

1. **Initializes modules** in the constructor
2. **Delegates table creation** to modules in `onCreate()`
3. **Delegates upgrades** to modules in `onUpgrade()`

This ensures each module is responsible for its own schema management.

## Benefits

1. **Separation of Concerns**: Each module manages its own tables and schema
2. **Maintainability**: Changes to one module don't affect others
3. **Testability**: Modules can be tested independently
4. **Scalability**: New features can be added as new modules
5. **Clear Boundaries**: Module boundaries are explicit and well-defined
6. **Backward Compatibility**: Existing code continues to work

## Migration Notes

- The vault database (`tella-vault-v4.db`) remains separate as it was already modular
- All existing tables and relationships are preserved
- Database version remains at 16
- No data migration is required

## Future Enhancements

Potential improvements:
1. Create separate DataSource classes for each module
2. Add module-level transaction support
3. Implement module-level backup/restore
4. Add module-level migration utilities

