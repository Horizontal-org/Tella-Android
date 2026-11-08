package org.horizontal.tella.mobile.util.crash

import android.content.Context

/**
 * Crash reporting: Crashlytics on Play Store builds, no-op on F-Droid.
 * No free-text log on purpose, so user data can't end up in reports.
 */
interface CrashReporter {
    fun init(context: Context)
    fun recordException(throwable: Throwable)
}
