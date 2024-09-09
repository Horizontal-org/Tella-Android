package rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsListBinding
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.adapter.EntityAdapter
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE

const val BUNDLE_IS_FROM_OUTBOX = "bundle_is_from_outbox"

@AndroidEntryPoint
class OutboxReportsFragment : BaseReportsFragment() {

    private val viewModel by viewModels<ReportsViewModel>()

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

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_OUTBOX, true)
        navManager().navigateFromReportsScreenToReportSendScreen()
    }

    override fun initData() {
        with(viewModel) {
            outboxReportListFormInstance.observe(viewLifecycleOwner) { outboxes ->
                handleReportList(outboxes)
            }

            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showMenu(
                    instance = instance,
                   title =  instance.title,
                   viewText =  getString(R.string.View_Report),
                    deleteText = getString(R.string.Delete_Report),
                    deleteConfirmation = getString(R.string.action_delete) + " \"" + instance.title + "\"?",
                    deleteActionText = getString(R.string.Delete_Submitted_Report_Confirmation),)
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

    private fun showOutboxMenu(instance: ReportInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.View_Report),
            getString(R.string.Delete_Report),
            object : BottomSheetUtils.ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        loadEntityInstance(instance)
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        viewModel.deleteReport(instance)
                    }
                }
            },
            getString(R.string.action_delete) + " \"" + instance.title + "\"?",
            requireContext().resources.getString(R.string.Delete_Submitted_Report_Confirmation),
            requireContext().getString(R.string.action_delete),
            requireContext().getString(R.string.action_cancel),
            R.drawable.ic_eye_white
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.listOutbox()
    }
}