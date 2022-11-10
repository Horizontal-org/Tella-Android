package rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsListBinding
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.adapter.EntityAdapter
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.reports.entry.ReportsEntryViewModel

@AndroidEntryPoint
class DraftsReportsFragment : BaseBindingFragment<FragmentReportsListBinding>(
    FragmentReportsListBinding::inflate
) {

    private val viewModel by viewModels<ReportsEntryViewModel>()
    private val entityAdapter: EntityAdapter by lazy { EntityAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {
        binding?.textViewEmpty?.setText(getString(R.string.Drafts_Reports_Empty_Message))
        binding?.listReportsRecyclerView?.apply {
            adapter = entityAdapter
            layoutManager = LinearLayoutManager(baseActivity)
        }

    }

    private fun initData() {
        with(viewModel) {
            draftListReportFormInstance.observe(viewLifecycleOwner, { drafts ->
                if (drafts.isEmpty()) {
                    binding?.listReportsRecyclerView?.hide()
                    binding?.textViewEmpty?.show()
                } else {
                    entityAdapter.setEntities(drafts)
                    binding?.listReportsRecyclerView?.show()
                    binding?.textViewEmpty?.hide()
                }
            })
            onMoreClickedFormInstance.observe(viewLifecycleOwner, { instance ->
                showDraftsMenu(instance)
            })

            draftReportInstance.observe(viewLifecycleOwner, { instance ->
                openEntityInstance(instance)
            })

            onOpenClickedFormInstance.observe(viewLifecycleOwner, { instance ->
                loadEntityInstance(instance)
            })

            instanceDeleted.observe(viewLifecycleOwner, {
                viewModel.listDrafts()
            })
        }
    }

    private fun showDraftsMenu(instance: ReportFormInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.Uwazi_Action_EditDraft),
            getString(R.string.Uwazi_Action_RemoveDraft),
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
            requireContext().resources.getString(R.string.Uwazi_Subtitle_RemoveDraft),
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel)
        )
    }

    private fun loadEntityInstance(reportFormInstance: ReportFormInstance) {
        viewModel.getDraftBundle(reportFormInstance)
    }

    private fun openEntityInstance(reportFormInstance: ReportFormInstance) {
        val bundle = Bundle()
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportFormInstance)
        nav().navigate(R.id.action_reportsScreen_to_newReport_screen, bundle)
    }

    override fun onResume() {
        super.onResume()
        viewModel.listDrafts()
    }

}