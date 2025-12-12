package org.horizontal.tella.mobile.data.database

/**
 * Abstraction for database-related preferences.
 * This interface allows the database module to work without depending on mobile's Preferences class.
 * 
 * The mobile module should provide an implementation of this interface.
 */
interface DatabasePreferences {
    fun isAlreadyMigratedMainDB(): Boolean
    fun setAlreadyMigratedMainDB(value: Boolean)
    fun setFreshInstall(value: Boolean)
}
