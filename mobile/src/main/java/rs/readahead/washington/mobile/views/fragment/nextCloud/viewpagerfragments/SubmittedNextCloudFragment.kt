package rs.readahead.washington.mobile.views.fragment.nextCloud.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.R.string
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.googledrive.GoogleDriveViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportsUtils
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.SharedLiveData.updateSubmittedTitle
import rs.readahead.washington.mobile.views.fragment.nextCloud.NextCloudViewModel

@AndroidEntryPoint
class SubmittedNextCloudFragment : BaseReportsFragment<NextCloudViewModel>() {

    // Use the ViewModel provided by Hilt
    private val submittedNextCloudViewModel: NextCloudViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun getViewModel(): NextCloudViewModel {
        return submittedNextCloudViewModel
    }

    override fun getEmptyMessage(): Int {
        return string.Submitted_Reports_Empty_Message
    }

    override fun getHeaderRecyclerViewMessage(): Int {
        return string.Submitted_Header_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_nextcloud
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        navManager().navigateFromNextCloudScreenToNextCloudSubmittedScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(submittedNextCloudViewModel) {
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
                    deleteActionText = getString(string.delete_report)
                )
            }

            instanceDeleted.observe(viewLifecycleOwner) {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                submittedNextCloudViewModel.listSubmitted()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        submittedNextCloudViewModel.listSubmitted()
    }
}