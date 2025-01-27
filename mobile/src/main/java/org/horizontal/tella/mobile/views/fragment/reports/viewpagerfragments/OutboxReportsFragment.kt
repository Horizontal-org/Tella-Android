package org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportsUtils
import org.horizontal.tella.mobile.views.fragment.reports.ReportsViewModel

const val BUNDLE_IS_FROM_OUTBOX = "bundle_is_from_outbox"

@AndroidEntryPoint
class OutboxReportsFragment : BaseReportsFragment<ReportsViewModel>() {

    private val outboxReportsViewModel by viewModels<ReportsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun getViewModel(): ReportsViewModel {
        return outboxReportsViewModel
    }

    override fun getEmptyMessage(): Int {
        return R.string.Outbox_Reports_Empty_Message
    }

    override fun getHeaderRecyclerViewMessage(): Int {
       return R.string.Outboxes_Header_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_reports
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_OUTBOX, true)
        navManager().navigateFromReportsScreenToReportSendScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(outboxReportsViewModel) {
            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showMenu(
                    instance = instance,
                    title = instance.title,
                    viewText = getString(R.string.View_Report),
                    deleteText = getString(R.string.Delete_Report),
                    deleteConfirmation = getString(R.string.Delete_Report_Confirmation),
                    deleteActionText = getString(R.string.delete_report)                )
            }
            instanceDeleted.observe(viewLifecycleOwner) {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        R.string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                outboxReportsViewModel.listOutbox()
            }
        }
    }
}