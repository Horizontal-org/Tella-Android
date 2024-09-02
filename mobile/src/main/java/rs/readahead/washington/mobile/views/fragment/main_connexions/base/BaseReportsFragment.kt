package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.databinding.FragmentReportsListBinding
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.adapter.EntityAdapter
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE

@AndroidEntryPoint
abstract class BaseReportsFragment : BaseBindingFragment<FragmentReportsListBinding>(FragmentReportsListBinding::inflate) {

    protected abstract val viewModel: ReportsViewModel // Child classes provide the specific ViewModel
    protected abstract fun getEmptyMessage(): Int // Child classes define specific empty messages
    protected abstract fun getAdapter(): EntityAdapter // Child classes provide the specific adapter
    protected abstract fun navigateToReportScreen(reportInstance: ReportInstance) // Navigation method to be implemented by subclasses

    private lateinit var adapter: EntityAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView and Adapter
        adapter = getAdapter()
        binding.listReportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@BaseReportsFragment.adapter
        }

        observeViewModel()
    }

    // Observe ViewModel data changes (e.g., report lists, progress, errors)
    private fun observeViewModel() {
        viewModel.draftListReportFormInstance.observe(viewLifecycleOwner) { reports ->
            handleReportList(reports)
        }

        viewModel.progress.observe(viewLifecycleOwner) { isLoading ->
            showProgress(isLoading)
        }

        viewModel.error.observe(viewLifecycleOwner) { throwable ->
            showError(throwable)
        }
    }

    private fun handleReportList(reports: List<ViewEntityTemplateItem>) {
        if (reports.isEmpty()) {
            showEmptyMessage()
        } else {
            adapter.setEntities(reports)
            binding.listReportsRecyclerView.visibility = View.VISIBLE
            binding.textViewEmpty.visibility = View.GONE
        }
    }

    private fun showProgress(isLoading: Boolean) {
        // Implement logic to show/hide progress bar
        if (isLoading) {
            binding.progress.visibility = View.VISIBLE
        } else {
            binding.progress.visibility = View.GONE
        }
    }

    private fun showError(throwable: Throwable) {
        Snackbar.make(binding.root, throwable.message.toString(), Snackbar.LENGTH_LONG).show()
    }

    private fun showEmptyMessage() {
        binding.textViewEmpty.setText(getString(getEmptyMessage()))
        binding.textViewEmpty.visibility = View.VISIBLE
        binding.listReportsRecyclerView.visibility = View.GONE
    }

    // Method to be used by subclasses to define how the navigation is handled
    protected fun openEntityInstance(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        navigateToReportScreen(reportInstance)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearDisposable()
    }
}
