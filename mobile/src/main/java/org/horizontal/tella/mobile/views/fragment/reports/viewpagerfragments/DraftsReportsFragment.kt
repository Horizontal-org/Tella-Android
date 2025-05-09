package org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportsUtils
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateDraftTitle
import org.horizontal.tella.mobile.views.fragment.reports.ReportsViewModel

@AndroidEntryPoint
class DraftsReportsFragment : BaseReportsFragment<ReportsViewModel>() {

    private val draftReportsDriveViewModel: ReportsViewModel by viewModels()

    override fun getViewModel(): ReportsViewModel {
        return draftReportsDriveViewModel
    }

    override fun getEmptyMessage(): Int {
        return R.string.Uwazi_Draft_Entities_Empty_Description
    }

    override fun getHeaderRecyclerViewMessage(): Int {
        return R.string.Drafts_Header_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_reports
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        this.navManager().navigateFromReportsScreenToNewReportScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(draftReportsDriveViewModel) {
            draftListReportFormInstance.observe(viewLifecycleOwner) { drafts ->
                handleReportList(drafts)
                updateDraftTitle.postValue(drafts.size)
            }

            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showMenu(
                    instance = instance,
                    title = instance.title,
                    viewText = getString(R.string.Uwazi_Action_EditDraft),
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
                draftReportsDriveViewModel.listDrafts()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        draftReportsDriveViewModel.listDrafts()
    }

}