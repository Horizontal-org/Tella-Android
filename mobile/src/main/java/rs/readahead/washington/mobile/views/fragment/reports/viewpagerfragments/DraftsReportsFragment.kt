package rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.adapter.EntityAdapter
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE

@AndroidEntryPoint
class DraftsReportsFragment : BaseReportsFragment() {

    private val viewModel: ReportsViewModel by viewModels()

    override fun getViewModel(): BaseReportsViewModel {
        return viewModel
    }

    override fun getEmptyMessage(): Int {
        return R.string.Uwazi_Draft_Entities_Empty_Description
    }

    override fun getAdapter(): EntityAdapter {
        return EntityAdapter()
    }

    override fun navigateToReportScreen(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        this.navManager().navigateFromReportsScreenToNewReportScreen()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    @SuppressLint("StringFormatInvalid")
    private fun initData() {
        with(viewModel) {
            draftListReportFormInstance.observe(viewLifecycleOwner) { drafts ->
                handleReportList(drafts)
            }
            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showDraftsMenu(instance)
            }

            reportInstance.observe(viewLifecycleOwner) { instance ->
                openEntityInstance(instance)
            }

            onOpenClickedInstance.observe(viewLifecycleOwner) { instance ->
                loadEntityInstance(instance)
            }

            instanceDeleted.observe(viewLifecycleOwner) {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        R.string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                viewModel.listDrafts()
            }
        }
    }

    private fun showDraftsMenu(instance: ReportInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.Uwazi_Action_EditDraft),
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
            baseActivity.resources.getString(R.string.Delete_Report_Confirmation),
            requireContext().getString(R.string.action_delete),
            requireContext().getString(R.string.action_cancel)
        )
    }


    override fun onResume() {
        super.onResume()
        viewModel.listDrafts()
    }

}