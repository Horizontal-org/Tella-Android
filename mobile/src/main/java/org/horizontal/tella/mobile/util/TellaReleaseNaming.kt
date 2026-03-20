package org.horizontal.tella.mobile.util

import android.content.Context
import org.horizontal.tella.mobile.BuildConfig
import org.horizontal.tella.mobile.R

/**
 * Central place for Tella release naming: "Tella X.X.X (build)" vs "Tella FOSS X.X.X (build)".
 * FOSS is detected from [BuildConfig.APPLICATION_ID] (fdroid flavor uses `…tellaFOSS`).
 *
 * Use this for About & Help, feedback payloads, and anywhere else the full release label is needed.
 */
object TellaReleaseNaming {

    /** True when this APK is the F-Droid / FOSS distribution (distinct package id from Play Store). */
    @JvmStatic
    fun isFossDistribution(): Boolean = BuildConfig.APPLICATION_ID.endsWith("FOSS")

    /** Localized app product name: "Tella" or "Tella FOSS". */
    @JvmStatic
    fun appDisplayName(context: Context): String =
        context.getString(
            if (isFossDistribution()) R.string.tella_foss else R.string.tella
        )

    /**
     * Full release string for UI and support metadata, e.g. `Tella 2.19.0 (224)` or `Tella FOSS 2.19.0 (224)`.
     */
    @JvmStatic
    fun fullReleaseLabel(context: Context): String {
        val name = appDisplayName(context)
        return "$name ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }
}
