package org.horizontal.tella.mobile.data.database

import org.horizontal.tella.mobile.data.database.modules.DatabaseModule
import org.horizontal.tella.mobile.data.database.modules.feedback.FeedbackDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.forms.FormsDatabaseModule
import org.horizontal.tella.mobile.data.database.modules.media.MediaDatabaseModule
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
 * For F-Droid, create a variant that excludes cloud modules.
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
    
    // These will be implemented after splitting CloudDatabaseModule
    private fun getGoogleDriveModules(): List<DatabaseModule> {
        // TODO: Return GoogleDriveDatabaseModule() after refactoring
        return emptyList()
    }
    
    private fun getDropboxModules(): List<DatabaseModule> {
        // TODO: Return DropboxDatabaseModule() after refactoring
        return emptyList()
    }
    
    private fun getNextCloudModules(): List<DatabaseModule> {
        // TODO: Return NextCloudDatabaseModule() after refactoring
        return emptyList()
    }
}
