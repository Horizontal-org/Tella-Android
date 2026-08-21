package org.horizontal.tella.mobile.util.crash

import android.content.Context

/**
 * No-op implementation of [CrashReporter] for F-Droid builds (no Firebase dependency).
 */
class NoOpCrashReporter : CrashReporter {
    override fun init(context: Context) {}
    override fun recordException(throwable: Throwable) {}
    override fun log(message: String) {}
}
