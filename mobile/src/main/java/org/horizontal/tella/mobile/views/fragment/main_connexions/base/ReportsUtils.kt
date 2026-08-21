package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.views.base_ui.BaseActivity

object ReportsUtils {

    fun showReportDeletedSnackBar(message: String, activity: BaseActivity) {
        DialogUtils.showBottomMessage(
            activity,
            message,
            false
        )
    }
}