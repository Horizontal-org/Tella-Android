package org.horizontal.tella.mobile.views.fragment.dropbox.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.views.fragment.dropbox.DropBoxViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportsUtils
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateOutboxTitle
import org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments.BUNDLE_IS_FROM_OUTBOX

@AndroidEntryPoint
class OutboxDropBoxFragment : BaseReportsFragment<DropBoxViewModel>() {

    // Use the ViewModel provided by Hilt
    private val outboxDropBoxViewModel: DropBoxViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    // Provide the specific ViewModel to the base class
    override fun getViewModel(): DropBoxViewModel {
        return outboxDropBoxViewModel
    }

    override fun getEmptyMessage(): Int {
        return R.string.Outbox_Reports_Empty_Message
    }

    override fun getHeaderRecyclerViewMessage(): Int {
       return R.string.Outboxes_Header_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_dropbox
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_OUTBOX, true)
        navManager().navigateFromDropBoxMainScreenToDropBoxSendScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(outboxDropBoxViewModel) {
            outboxReportListFormInstance.observe(viewLifecycleOwner) { outboxes ->
                updateOutboxTitle.postValue(outboxes.size)
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
                outboxDropBoxViewModel.listOutbox()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        outboxDropBoxViewModel.listOutbox()
    }
}