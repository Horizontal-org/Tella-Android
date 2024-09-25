package rs.readahead.washington.mobile.views.fragment.googledrive.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.googledrive.GoogleDriveViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportsUtils
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel

const val BUNDLE_IS_FROM_OUTBOX = "bundle_is_from_outbox"

@AndroidEntryPoint
class OutboxGoogleDriveFragment : BaseReportsFragment() {

    private val viewModel by viewModels<GoogleDriveViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun getViewModel(): BaseReportsViewModel {
        return viewModel
    }

    override fun getEmptyMessage(): Int {
        return R.string.Outbox_Reports_Empty_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_google_drive_logo
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_OUTBOX, true)
        navManager().navigateFromGoogleDriveScreenToGoogleDriveSendScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(viewModel) {
            outboxReportListFormInstance.observe(viewLifecycleOwner) { outboxes ->
                handleReportList(outboxes)
            }

            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showMenu(
                    instance = instance,
                    title = instance.title,
                    viewText = getString(R.string.View_Report),
                    deleteText = getString(R.string.Delete_Report),
                    deleteConfirmation = getString(R.string.action_delete) + " \"" + instance.title + "\"?",
                    deleteActionText = getString(R.string.Delete_Submitted_Report_Confirmation),
                )
            }
            instanceDeleted.observe(viewLifecycleOwner) {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        R.string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                viewModel.listOutbox()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.listOutbox()
    }
}