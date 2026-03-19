package org.horizontal.tella.mobile.views.fragment.googledrive.di

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Stub implementation of DriveServiceProvider for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but throws
 * UnsupportedOperationException since Google Drive is not available in F-Droid builds.
 */
class DriveServiceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDriveService(email: String): Nothing {
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }
}




