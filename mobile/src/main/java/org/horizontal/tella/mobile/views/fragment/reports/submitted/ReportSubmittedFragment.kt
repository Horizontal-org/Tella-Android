package org.horizontal.tella.mobile.views.fragment.reports.submitted

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportSubmittedFragment
import org.horizontal.tella.mobile.views.fragment.reports.ReportsViewModel

@AndroidEntryPoint
class ReportSubmittedFragment : BaseReportSubmittedFragment() {

    override val viewModel by viewModels<ReportsViewModel>()
}

