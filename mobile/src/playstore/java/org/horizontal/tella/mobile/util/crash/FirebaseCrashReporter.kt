package org.horizontal.tella.mobile.util.crash

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.horizontal.tella.mobile.BuildConfig
import org.horizontal.tella.mobile.data.sharedpref.Preferences

/**
 * Play Store implementation of [CrashReporter] using Firebase Crashlytics.
 */
class FirebaseCrashReporter : CrashReporter {

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    override fun init(context: Context) {
        val enabled = !BuildConfig.DEBUG && Preferences.isSubmittingCrashReports()
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
        if (!enabled) {
            crashlytics.deleteUnsentReports()
        }
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }
}
