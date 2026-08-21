package org.horizontal.tella.mobile.util

import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.sharedpref.Preferences

enum class FailedUnlockOption(val value: Long, val stringResId: Int) {
    OFF(0L, R.string.Settings_Off_Do_Not_Delete),
    ATTEMPTS_5(5L, R.string.Settings_Five_Attempts),
    ATTEMPTS_10(10L, R.string.Settings_Ten_Attempts),
    ATTEMPTS_20(20L, R.string.Settings_Twenty_Attempts)
}

class FailedUnlockManager {
    private val options: LinkedHashMap<Long, Int> =
        FailedUnlockOption.entries.associate { it.value to it.stringResId } as LinkedHashMap

    fun getOptionsList(): LinkedHashMap<Long, Int> = options

    fun getFailedUnlockOptionText(): Int {
        val optionMap = mapOf(
            20L to R.string.Settings_Twenty_Attempts,
            5L  to R.string.Settings_Five_Attempts,
            10L to R.string.Settings_Ten_Attempts
        )

        return optionMap[Preferences.getFailedUnlockOption()] ?: R.string.Settings_Off
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

    fun getUnlockRemainingAttempts(): Long {
        return Preferences.getUnlockRemainingAttempts()
    }

    fun setUnlockRemainingAttempts(option: Long) {
        Preferences.setUnlockRemainingAttempts(option)
    }

}
