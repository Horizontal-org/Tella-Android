package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

object ReportsUtils {

     fun showReportDeletedSnackBar(message: String,activity: BaseActivity) {
        DialogUtils.showBottomMessage(
            activity,
            message,
            false
        )
    }

}