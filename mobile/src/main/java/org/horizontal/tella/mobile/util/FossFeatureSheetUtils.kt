package org.horizontal.tella.mobile.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentManager
import org.horizontal.tella.mobile.R
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils

/**
 * Shared UI for the "Feature unavailable" bottom sheet shown when Google Drive or Dropbox
 * are not available (e.g. in F-Droid builds). Learn More opens the open-source info URL;
 * optional callbacks allow callers to finish or navigate back after dismiss.
 */
object FossFeatureSheetUtils {

    private const val OPEN_SOURCE_URL = "https://tella-app.org/open-source"

    /**
     * Shows the FOSS feature unavailable bottom sheet.
     *
     * @param fragmentManager FragmentManager to show the sheet with
     * @param context Context for strings and opening the Learn More URL
     * @param onAfterLearnMoreClick Optional; called after opening the Learn More URL (e.g. finish())
     * @param onUnderstandClick Optional; called when user taps "I Understand" (e.g. finish() or onBackPressed())
     */
    @JvmStatic
    fun showFossFeatureUnavailableSheet(
        fragmentManager: FragmentManager,
        context: Context,
        onAfterLearnMoreClick: (() -> Unit)? = null,
        onUnderstandClick: (() -> Unit)? = null
    ) {
        BottomSheetUtils.showStandardSheet(
            fragmentManager = fragmentManager,
            titleText = context.getString(R.string.Foss_Feature_Unavailable_Title),
            descriptionText = context.getString(R.string.Foss_Feature_Unavailable_Description),
            actionButtonLabel = context.getString(R.string.Learn_More),
            cancelButtonLabel = context.getString(R.string.onboard_lock_warning_cta),
            onConfirmClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OPEN_SOURCE_URL)))
                onAfterLearnMoreClick?.invoke()
            },
            onCancelClick = onUnderstandClick
        )
    }
}
