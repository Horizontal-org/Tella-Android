package org.horizontal.tella.mobile.data.database

import org.horizontal.tella.mobile.data.database.modules.DatabaseModule
import org.horizontal.tella.mobile.data.database.modules.dropbox.DropboxDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.feedback.FeedbackDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.forms.FormsDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.googledrive.GoogleDriveDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.media.MediaDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.nextcloud.NextCloudDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.reports.ReportsDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.resources.ResourcesDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.settings.SettingsDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.uwazi.UwaziDatabaseModule

/**
 * Provides database modules based on build configuration.
 * 
 * For F-Droid builds, cloud modules can be excluded by:
 * 1. Not including the module classes in the build
 * 2. Using build variants with different source sets
 * 3. Using dependency injection to conditionally provide modules
 * 
 * Usage:
 * - Core modules: Always included
 * - Cloud modules: Optional, can be excluded for F-Droid
 */
interface DatabaseModuleProvider {
    fun getModules(): List<DatabaseModule>
}

/**
 * Default implementation that includes all modules.
 * For F-Droid, pass false for Google Drive and Dropbox.
 * 
 * Usage:
 * - Play Store: DefaultDatabaseModuleProvider() or DefaultDatabaseModuleProvider(includeGoogleDrive = true, includeDropbox = true)
 * - F-Droid: DefaultDatabaseModuleProvider(includeGoogleDrive = false, includeDropbox = false)
 */
class DefaultDatabaseModuleProvider(
    private val includeGoogleDrive: Boolean = true,
    private val includeDropbox: Boolean = true,
    private val includeNextCloud: Boolean = true
) : DatabaseModuleProvider {
    
    override fun getModules(): List<DatabaseModule> {
        val modules = mutableListOf<DatabaseModule>()
        
        // Core modules (always included)
        modules.addAll(getCoreModules())
        
        // Optional cloud modules
        if (includeGoogleDrive) {
            modules.addAll(getGoogleDriveModules())
        }
        if (includeDropbox) {
            modules.addAll(getDropboxModules())
        }
        // NextCloud is always included (F-Droid compatible)
        if (includeNextCloud) {
            modules.addAll(getNextCloudModules())
        }
        
        return modules
    }
    
    private fun getCoreModules(): List<DatabaseModule> {
        return listOf(
            SettingsDatabaseModule(),
            FormsDatabaseModule(),
            MediaDatabaseModule(),
            ReportsDatabaseModule(),
            UwaziDatabaseModule(),
            FeedbackDatabaseModule(),
            ResourcesDatabaseModule()
        )
    }
    
    private fun getGoogleDriveModules(): List<DatabaseModule> {
        return listOf(
            GoogleDriveDatabaseModule()
        )
    }
    
    private fun getDropboxModules(): List<DatabaseModule> {
        return listOf(
            DropboxDatabaseModule()
        )
    }
    
    private fun getNextCloudModules(): List<DatabaseModule> {
        return listOf(
            NextCloudDatabaseModule()
        )
    }
}
