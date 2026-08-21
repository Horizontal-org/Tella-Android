package com.hzontal.tella_locking_ui.common

import android.content.Context
import com.hzontal.tella_locking_ui.R
import com.hzontal.tella_locking_ui.TellaKeysUI

object ErrorMessageUtil {

    fun generateErrorMessage(
        context: Context,
        incorrectStringResource: Int,
        incorrectSetStringResource: Int,
        isShowRemainingAttempts: Boolean
    ): String {
        val failedAttemptsRemaining = updateAndReturnRemainingAttempts()

        if (failedAttemptsRemaining == 0L) {
            TellaKeysUI.getCredentialsCallback().onFailedAttempts(failedAttemptsRemaining)
            return context.getString(incorrectStringResource) +
                    context.getString(R.string.exceeded_max_attempts)
        }

        if (isShowRemainingAttempts) {
            when (failedAttemptsRemaining) {
                1L -> {
                    return context.getString(incorrectStringResource) +
                            context.getString(R.string.attempts_remaining_singular)
                }
                in 2..3 -> {
                    return context.getString(incorrectStringResource) +
                            context.getString(
                                R.string.attempts_remaining_plural,
                                failedAttemptsRemaining
                            )
                }
            }
        }

        return context.getString(incorrectSetStringResource)
    }


    private fun updateAndReturnRemainingAttempts(): Long {
        val remainingAttempts = if (TellaKeysUI.getRemainingAttempts() == 0L) {
            TellaKeysUI.getNumFailedAttempts() - 1
        } else {
            TellaKeysUI.getRemainingAttempts() - 1
        }

        TellaKeysUI.setRemainingAttempts(remainingAttempts)
        TellaKeysUI.getCredentialsCallback().saveRemainingAttempts(remainingAttempts)

        return remainingAttempts
    }

    fun resetUnlockAttempts() {
        TellaKeysUI.setRemainingAttempts(0L)
        TellaKeysUI.getCredentialsCallback().saveRemainingAttempts(0L)
    }
}
