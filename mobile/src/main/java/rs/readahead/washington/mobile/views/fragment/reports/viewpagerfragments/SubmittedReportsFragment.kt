package rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.R.string
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportsUtils
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.SharedLiveData.updateSubmittedTitle
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel

@AndroidEntryPoint
class SubmittedReportsFragment : BaseReportsFragment<ReportsViewModel>() {

    private val submittedReportsViewModel by viewModels<ReportsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun getViewModel(): ReportsViewModel {
        return submittedReportsViewModel
    }

    override fun getEmptyMessage(): Int {
        return string.Submitted_Reports_Empty_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_reports
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        navManager().navigateFromReportsScreenToReportSubmittedScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(submittedReportsViewModel) {

            submittedReportListFormInstance.observe(viewLifecycleOwner) { submitted ->
                handleReportList(submitted)
                updateSubmittedTitle.postValue(submitted.size)
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
                submittedReportsViewModel.listSubmitted()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        submittedReportsViewModel.listSubmitted()
    }
}