package org.horizontal.tella.mobile.util.crash

import android.content.Context

/**
 * Abstraction for crash reporting. Play Store builds use Firebase Crashlytics;
 * F-Droid builds use a no-op implementation.
 */
interface CrashReporter {
    fun init(context: Context)
    fun recordException(throwable: Throwable)
    fun log(message: String)
}
