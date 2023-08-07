package rs.readahead.washington.mobile.util

import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences

class FailedUnlockManager {

    companion object {
        const val OPTION_OFF = 0L
        const val OPTION_5_ATTEMPTS = 5L
        const val OPTION_10_ATTEMPTS = 10L
        const val OPTION_20_ATTEMPTS = 20L
    }
    private var options: LinkedHashMap<Long, Int> = LinkedHashMap()

    init {
        options = LinkedHashMap<Long, Int>().apply {
            put(OPTION_OFF, R.string.Settings_Off_Do_Not_Delete)
            put(OPTION_5_ATTEMPTS, R.string.Settings_Five_Attempts)
            put(OPTION_10_ATTEMPTS, R.string.Settings_Ten_Attempts)
            put(OPTION_20_ATTEMPTS, R.string.Settings_Twenty_Attempts)
        }
    }

    fun getOptionsList(): LinkedHashMap<Long, Int> = options

    fun getFailedUnlockOption(): Int = when (Preferences.getFailedUnlockOption()) {
        OPTION_OFF -> R.string.Settings_Off
        OPTION_5_ATTEMPTS -> R.string.Settings_Five_Attempts
        OPTION_10_ATTEMPTS -> R.string.Settings_Ten_Attempts
        OPTION_20_ATTEMPTS -> R.string.Settings_Twenty_Attempts
        else -> R.string.Settings_Off
    }

    fun setFailedUnlockOption(option : Long) {
        Preferences.setFailedUnlockOption(option)
    }

    fun getLockTimeout(): Long {
        return Preferences.getFailedUnlockOption()
    }
}
