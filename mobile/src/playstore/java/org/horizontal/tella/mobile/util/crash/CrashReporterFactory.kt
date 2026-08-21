package org.horizontal.tella.mobile.util.crash

/**
 * Play Store implementation: returns Firebase Crashlytics-backed reporter.
 */
object CrashReporterFactory {
    fun create(): CrashReporter = FirebaseCrashReporter()
}
