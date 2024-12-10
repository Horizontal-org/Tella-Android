package rs.readahead.washington.mobile.views.fragment.nextCloud.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportsUtils
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.SharedLiveData.updateOutboxTitle
import rs.readahead.washington.mobile.views.fragment.nextCloud.NextCloudViewModel
import rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments.BUNDLE_IS_FROM_OUTBOX

@AndroidEntryPoint
class OutboxNextCloudFragment : BaseReportsFragment<NextCloudViewModel>() {

    // Use the ViewModel provided by Hilt
    private val outboxNextCloudViewModel: NextCloudViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    // Provide the specific ViewModel to the base class
    override fun getViewModel(): NextCloudViewModel {
        return outboxNextCloudViewModel
    }

    override fun getEmptyMessage(): Int {
        return R.string.Outbox_Reports_Empty_Message
    }

    override fun getHeaderRecyclerViewMessage(): Int {
        return R.string.Outboxes_Header_Message
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_nextcloud
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_OUTBOX, true)
        navManager().navigateFromNextCloudMainScreenToNextCloudSendScreen()
    }

    @SuppressLint("StringFormatInvalid")
    override fun initData() {
        with(outboxNextCloudViewModel) {
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
                    deleteConfirmation = getString(R.string.Delete_Report_Confirmation),
                    deleteActionText = getString(R.string.delete_report)
                )
            }
            instanceDeleted.observe(viewLifecycleOwner) {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        R.string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                outboxNextCloudViewModel.listOutbox()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        outboxNextCloudViewModel.listOutbox()
    }
}