package rs.readahead.washington.mobile.util.exception

import com.google.firebase.crashlytics.FirebaseCrashlytics
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException

object ExceptionHandler {

    fun handleException(throwable: Throwable?): ExceptionType {
        return when (throwable) {
            is NoConnectivityException -> ExceptionType.NO_CONNECTIVITY
            else -> {
                // Log the exception to Crashlytics or any other analytics tool
                FirebaseCrashlytics.getInstance().recordException(throwable!!)

                //TODO can implement logic to identify other exception types if needed
                // For example, check the class or message of the exception
                ExceptionType.OTHER
            }
        }
    }

}