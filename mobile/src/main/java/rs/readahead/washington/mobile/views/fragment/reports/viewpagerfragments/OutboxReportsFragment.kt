package rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments

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
import rs.readahead.washington.mobile.views.fragment.reports.adapter.EntityAdapter
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel

const val BUNDLE_IS_FROM_OUTBOX = "bundle_is_from_outbox"

@AndroidEntryPoint
class OutboxReportsFragment : BaseBindingFragment<FragmentReportsListBinding>(
    FragmentReportsListBinding::inflate
) {
    private val viewModel by viewModels<ReportsViewModel>()
    private val entityAdapter: EntityAdapter by lazy { EntityAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {
        binding?.textViewEmpty?.setText(getString(R.string.Outbox_Reports_Empty_Message))
        binding?.listReportsRecyclerView?.apply {
            adapter = entityAdapter
            layoutManager = LinearLayoutManager(baseActivity)
        }
    }

    private fun initData() {
        with(viewModel) {
            outboxReportListFormInstance.observe(viewLifecycleOwner) { outboxes ->
                if (outboxes.isEmpty()) {
                    binding?.listReportsRecyclerView?.hide()
                    binding?.textViewEmpty?.show()
                } else {
                    entityAdapter.setEntities(outboxes)
                    binding?.listReportsRecyclerView?.show()
                    binding?.textViewEmpty?.hide()
                }
            }
            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showOutboxMenu(instance)
            }

            reportInstance.observe(viewLifecycleOwner) { instance ->
                openEntityInstance(instance)
            }

            onOpenClickedInstance.observe(viewLifecycleOwner) { instance ->
                loadEntityInstance(instance)
            }

            instanceDeleted.observe(viewLifecycleOwner) {
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
            requireContext().resources.getString(R.string.Delete_Report_Confirmation),
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel),
            R.drawable.ic_eye_white
        )
    }

    private fun loadEntityInstance(reportInstance: ReportInstance) {
        viewModel.getReportBundle(reportInstance)
    }

    private fun openEntityInstance(reportInstance: ReportInstance) {
        val bundle = Bundle()
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_OUTBOX, true)

        nav().navigate(R.id.action_reportsScreen_to_reportSendScreen, bundle)
    }

    override fun onResume() {
        super.onResume()
        viewModel.listOutbox()
    }
}