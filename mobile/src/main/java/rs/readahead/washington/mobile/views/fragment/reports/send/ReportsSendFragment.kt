package rs.readahead.washington.mobile.views.fragment.reports.send

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsSendFragment
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel

@AndroidEntryPoint
class ReportsSendFragment : BaseReportsSendFragment() {

    override val viewModel by viewModels<ReportsViewModel>()

    override fun navigateBack() {
        if (isFromOutbox) {
            nav().popBackStack()
        } else {
            nav().popBackStack(R.id.newReportScreen, true)
        }
    }
}