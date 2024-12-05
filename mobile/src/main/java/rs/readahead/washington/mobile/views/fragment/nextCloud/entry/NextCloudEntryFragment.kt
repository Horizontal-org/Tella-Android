package rs.readahead.washington.mobile.views.fragment.nextCloud.entry

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BUNDLE_IS_FROM_DRAFT
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsEntryFragment
import rs.readahead.washington.mobile.views.fragment.nextCloud.NextCloudViewModel

@AndroidEntryPoint
class NextCloudEntryFragment :
    BaseReportsEntryFragment() {
    override val viewModel: NextCloudViewModel by viewModels()

    override fun submitReport(reportInstance: ReportInstance?) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_DRAFT, true)
        navManager().navigateFromNextCloudEntryScreenToNextCloudSendScreen()
    }
}
