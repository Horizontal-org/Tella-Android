package rs.readahead.washington.mobile.views.fragment.googledrive.viewpagerfragments

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.googledrive.GoogleDriveViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportsUtils

@AndroidEntryPoint
class DraftsGoogleDriveFragment : BaseReportsFragment<GoogleDriveViewModel>() {

    // Use the ViewModel provided by Hilt
    private val draftGoogleDriveViewModel: GoogleDriveViewModel by viewModels()

    override fun getViewModel(): GoogleDriveViewModel {
        return draftGoogleDriveViewModel
    }

    override fun getEmptyMessage(): Int {
        return R.string.Uwazi_Draft_Entities_Empty_Description
    }

    override fun getHeaderRecyclerViewMessage(): Int {
        return R.string.Drafts_Header_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_google_drive
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        this.navManager().navigateFromGoogleDriveScreenToNewGoogleDriveScreen()

    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(draftGoogleDriveViewModel) {
            draftListReportFormInstance.observe(viewLifecycleOwner) { drafts ->
                handleReportList(drafts)
            }

            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showMenu(
                    instance = instance,
                    title = instance.title,
                    viewText = getString(R.string.Uwazi_Action_EditDraft),
                    deleteText = getString(R.string.Delete_Report),
                    deleteConfirmation = getString(R.string.action_delete) + " \"" + instance.title + "\"?",
                    deleteActionText = getString(R.string.Delete_Report_Confirmation)
                )
            }

            instanceDeleted.observe(viewLifecycleOwner) {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        R.string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                draftGoogleDriveViewModel.listOutbox()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        draftGoogleDriveViewModel.listDrafts()
    }

}