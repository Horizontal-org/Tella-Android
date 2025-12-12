package org.horizontal.tella.mobile.data.database

import org.horizontal.tella.mobile.data.sharedpref.Preferences

/**
 * Adapter that bridges mobile's Preferences to tella-database's DatabasePreferences interface.
 * This allows the database module to work without depending on mobile's Preferences implementation.
 */
object PreferencesAdapter :  {
    override fun isAlreadyMigratedMainDB(): Boolean {
        return Preferences.isAlreadyMigratedMainDB()
    }

    override fun setAlreadyMigratedMainDB(value: Boolean) {
        Preferences.setAlreadyMigratedMainDB(value)
    }

    override fun setFreshInstall(value: Boolean) {
        Preferences.setFreshInstall(value)
    }
}
