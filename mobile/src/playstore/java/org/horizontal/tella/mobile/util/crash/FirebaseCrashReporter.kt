package org.horizontal.tella.mobile.util.crash

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.horizontal.tella.mobile.BuildConfig
import org.horizontal.tella.mobile.data.sharedpref.Preferences

/** Crashlytics reporter. Everything goes through [CrashReportSanitizer] first. */
class FirebaseCrashReporter : CrashReporter {

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    override fun init(context: Context) {
        val enabled = !BuildConfig.DEBUG && Preferences.isSubmittingCrashReports()
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
        if (!enabled) {
            crashlytics.deleteUnsentReports()
        }
        installFatalCrashSanitizer()
    }

    override fun recordException(throwable: Throwable) {
        CrashReportSanitizer.sanitizeNonFatal(throwable)?.let { crashlytics.recordException(it) }
    }

    // Crashlytics catches fatal crashes with its own handler, so we wrap it
    // to hand it the sanitized exception instead of the original.
    private fun installFatalCrashSanitizer() {
        val crashlyticsHandler = Thread.getDefaultUncaughtExceptionHandler() ?: return
        if (crashlyticsHandler is SanitizingUncaughtExceptionHandler) return
        Thread.setDefaultUncaughtExceptionHandler(SanitizingUncaughtExceptionHandler(crashlyticsHandler))
    }

    private class SanitizingUncaughtExceptionHandler(
        private val delegate: Thread.UncaughtExceptionHandler
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            val sanitized = try {
                CrashReportSanitizer.sanitize(throwable)
            } catch (ignored: Throwable) {
                // If sanitizing fails, send the class name and stack trace only.
                RedactedException(throwable.javaClass.name, null).also { it.stackTrace = throwable.stackTrace }
            }
            delegate.uncaughtException(thread, sanitized)
        }
    }
}
