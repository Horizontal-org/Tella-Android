package org.horizontal.tella.mobile.util.crash

import androidx.annotation.VisibleForTesting

/**
 * Provides the flavor-specific [CrashReporter] instance.
 * Call [init] from Application.onCreate(); then use [get] from call sites.
 */
object CrashReporterProvider {
    @Volatile
    private var instance: CrashReporter? = null

    fun init(context: android.content.Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    instance = CrashReporterFactory.create().also { it.init(context) }
                }
            }
        }
    }

    fun get(): CrashReporter =
        instance ?: throw IllegalStateException("CrashReporterProvider.init() must be called from Application.onCreate()")

    /**
     * For tests only. Sets the reporter instance so that [get] returns it without calling [init].
     * Call with null to reset.
     */
    @VisibleForTesting
    fun setInstanceForTesting(reporter: CrashReporter?) {
        instance = reporter
    }
}
