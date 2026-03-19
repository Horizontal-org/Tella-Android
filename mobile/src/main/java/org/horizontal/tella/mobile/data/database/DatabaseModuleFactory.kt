package org.horizontal.tella.mobile.data.database

import org.horizontal.tella.mobile.BuildConfig
import org.horizontal.tella.mobile.BuildConfig.ENABLE_DROPBOX
import org.horizontal.tella.mobile.BuildConfig.ENABLE_GOOGLE_DRIVE

/**
 * Factory for creating DatabaseModuleProvider based on build configuration.
 * 
 * This allows the database layer to conditionally include Google Drive and Dropbox
 * modules based on the build variant (playstore vs fdroid).
 */
object DatabaseModuleFactory {
    /**
     * Creates a DatabaseModuleProvider based on BuildConfig flags.
     * 
     * For F-Droid builds, Google Drive and Dropbox modules are excluded.
     * For Play Store builds, all modules are included.
     */
    @JvmStatic
    fun createProvider(): DatabaseModuleProvider {
        return DefaultDatabaseModuleProvider(
            includeGoogleDrive = ENABLE_GOOGLE_DRIVE,
            includeDropbox = ENABLE_DROPBOX,
            includeNextCloud = true // NextCloud is always included (F-Droid compatible)
        )
    }
}




