package com.hzontal.utils

import android.content.Context
import android.content.SharedPreferences

class Preferences(context: Context) {

    companion object {
        private const val PREF_NAME = "vault_preferences"
        private const val IS_MIGRATED_VAULT_DB = "is_migrated_vault_db"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    private fun setBoolean(key: String, value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    fun isAlreadyMigratedVaultDB(): Boolean {
        return getBoolean(IS_MIGRATED_VAULT_DB, false)
    }

    fun setAlreadyMigratedVaultDB(value: Boolean) {
        setBoolean(IS_MIGRATED_VAULT_DB, value)
    }
}
