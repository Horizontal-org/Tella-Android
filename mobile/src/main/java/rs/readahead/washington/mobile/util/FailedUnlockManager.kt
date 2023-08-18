package rs.readahead.washington.mobile.util

import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences

enum class FailedUnlockOption(val value: Long, val stringResId: Int) {
    OFF(0L, R.string.Settings_Off_Do_Not_Delete),
    ATTEMPTS_5(5L, R.string.Settings_Five_Attempts),
    ATTEMPTS_10(10L, R.string.Settings_Ten_Attempts),
    ATTEMPTS_20(20L, R.string.Settings_Twenty_Attempts)
}

class FailedUnlockManager {
    private val options: LinkedHashMap<Long, Int> =
        FailedUnlockOption.values().associate { it.value to it.stringResId } as LinkedHashMap

    fun getOptionsList(): LinkedHashMap<Long, Int> = options

    fun getFailedUnlockOption(): Int {
        val selectedOption = Preferences.getFailedUnlockOption()
        return if (selectedOption == 0L) {
            R.string.Settings_Off
        } else {
            R.string.Settings_On
        }
    }

    fun setFailedUnlockOption(option: Long) {
        Preferences.setFailedUnlockOption(option)
    }

    fun getOption(): Long {
        return Preferences.getFailedUnlockOption()
    }

    fun isShowRemainingAttempts(): Boolean {
        return Preferences.isShowUnlockRemainingAttempts()
    }

    fun setShowUnlockRemainingAttempts(option: Boolean) {
        Preferences.setShowUnlockRemainingAttempts(option)
    }

}
