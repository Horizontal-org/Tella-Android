package org.horizontal.tella.mobile.views.fragment.googledrive.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.R.string
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.views.fragment.googledrive.GoogleDriveViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportsUtils
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateSubmittedTitle

@AndroidEntryPoint
class SubmittedGoogleDriveFragment : BaseReportsFragment<GoogleDriveViewModel>() {

    // Use the ViewModel provided by Hilt
    private val submittedGoogleDriveViewModel: GoogleDriveViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun getViewModel(): GoogleDriveViewModel {
        return submittedGoogleDriveViewModel
    }

    override fun getEmptyMessage(): Int {
        return string.Submitted_Reports_Empty_Message
    }

    override fun getHeaderRecyclerViewMessage(): Int {
        return string.Submitted_Header_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_google_drive
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        navManager().navigateFromGoogleDriveScreenToGoogleDriveSubmittedScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(submittedGoogleDriveViewModel) {
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
                    deleteConfirmation = getString(string.Delete_Report_Confirmation),
                    deleteActionText = getString(string.delete_report)                )
            }

            instanceDeleted.observe(viewLifecycleOwner) {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                submittedGoogleDriveViewModel.listSubmitted()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        submittedGoogleDriveViewModel.listSubmitted()
    }
}