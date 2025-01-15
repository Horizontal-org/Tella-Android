package org.horizontal.tella.mobile.views.fragment.reports.entry

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_IS_FROM_DRAFT
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsEntryFragment
import org.horizontal.tella.mobile.views.fragment.reports.ReportsViewModel


@AndroidEntryPoint
class ReportsEntryFragment :
    BaseReportsEntryFragment() {
    override val viewModel: ReportsViewModel by viewModels()


    override fun submitReport(reportInstance: ReportInstance?) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_DRAFT, true)
        navManager().navigateFromNewReportsScreenToReportSendScreen()
    }
}

