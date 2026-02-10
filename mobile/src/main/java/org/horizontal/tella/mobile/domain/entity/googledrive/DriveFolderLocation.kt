package org.horizontal.tella.mobile.domain.entity.googledrive

/**
 * Result of checking where a Google Drive folder lives (with DRIVE_FILE scope).
 * - MY_DRIVE: folder is in the user's My Drive (app-created or selected).
 * - SHARED_DRIVE: folder is on a Shared Drive (not supported with DRIVE_FILE).
 * - UNKNOWN: cannot verify (e.g. 403/404 with DRIVE_FILE = not app-accessible, wrong account).
 */
enum class DriveFolderLocation {
    MY_DRIVE,
    SHARED_DRIVE,
    UNKNOWN
}
