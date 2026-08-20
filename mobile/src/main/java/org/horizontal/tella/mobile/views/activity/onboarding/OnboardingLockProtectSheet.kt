package org.horizontal.tella.mobile.views.activity.onboarding

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.hzontal.tella_locking_ui.ONBOARDING_LOCK_PROTECT_SHEET_TAG
import org.horizontal.tella.mobile.R
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils

object OnboardingLockProtectSheet {

    @JvmStatic
    fun show(activity: AppCompatActivity, onContinue: Runnable) {
        val fragmentManager = activity.supportFragmentManager
        val existing = fragmentManager.findFragmentByTag(ONBOARDING_LOCK_PROTECT_SHEET_TAG) as? DialogFragment
        if (existing?.isAdded == true) {
            return
        }
        if (existing != null) {
            fragmentManager.beginTransaction().remove(existing).commitAllowingStateLoss()
        }

        BottomSheetUtils.showStandardSheet(
            fragmentManager = fragmentManager,
            titleText = activity.getString(R.string.onboard_lock_protect_sheet_title),
            descriptionText = activity.getString(R.string.onboard_lock_protect_sheet_message),
            actionButtonLabel = activity.getString(R.string.onboard_lock_protect_sheet_continue),
            cancelButtonLabel = null,
            onConfirmClick = { onContinue.run() },
            onCancelClick = null,
            screenTag = ONBOARDING_LOCK_PROTECT_SHEET_TAG,
            useOverlay = false
        )
    }
}
