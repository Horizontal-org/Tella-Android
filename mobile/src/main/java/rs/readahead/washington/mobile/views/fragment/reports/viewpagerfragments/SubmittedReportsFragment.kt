package rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R.string
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE

@AndroidEntryPoint
class SubmittedReportsFragment : BaseReportsFragment() {

    private val viewModel by viewModels<ReportsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun getViewModel(): BaseReportsViewModel {
        return viewModel
    }

    override fun getEmptyMessage(): Int {
        return string.Submitted_Reports_Empty_Message
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        navManager().navigateFromReportsScreenToReportSubmittedScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(viewModel) {
            submittedReportListFormInstance.observe(viewLifecycleOwner) { outboxes ->
                handleReportList(outboxes)
            }

            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showMenu(
                    instance = instance,
                    title = instance.title,
                    viewText = getString(string.View_Report),
                    deleteText = getString(string.Delete_Report),
                    deleteConfirmation = getString(string.action_delete) + " \"" + instance.title + "\"?",
                    deleteActionText = getString(string.Delete_Submitted_Report_Confirmation),
                )
            }

            instanceDeleted.observe(viewLifecycleOwner) {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                viewModel.listSubmitted()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        viewModel.listSubmitted()
    }
}