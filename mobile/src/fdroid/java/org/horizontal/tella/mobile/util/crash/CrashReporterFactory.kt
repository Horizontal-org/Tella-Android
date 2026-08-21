package org.horizontal.tella.mobile.util.crash

/**
 * F-Droid implementation: returns no-op reporter (no Firebase dependency).
 */
object CrashReporterFactory {
    fun create(): CrashReporter = NoOpCrashReporter()
}
