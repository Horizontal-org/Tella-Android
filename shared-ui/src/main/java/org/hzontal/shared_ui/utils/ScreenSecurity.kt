package org.hzontal.shared_ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.view.Window
import android.view.WindowManager

object ScreenSecurity {
    const val PREFS_NAME = "washington_shared_prefs"
    const val PREF_KEY = "set_security_screen"
    const val DEFAULT_ENABLED = true

    @JvmStatic
    fun isEnabled(context: Context): Boolean =
        isEnabled(
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        )

    @JvmStatic
    fun isEnabled(preferences: SharedPreferences): Boolean =
        preferences.getBoolean(PREF_KEY, DEFAULT_ENABLED)

    @JvmStatic
    fun applyToWindow(window: Window, context: Context) {
        applyToWindow(window, isEnabled(context))
    }

    @JvmStatic
    fun applyToWindow(window: Window, enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
