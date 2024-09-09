package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsListBinding
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.adapter.EntityAdapter
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments.CustomAdapter
import timber.log.Timber

abstract class BaseReportsFragment :
    BaseBindingFragment<FragmentReportsListBinding>(FragmentReportsListBinding::inflate) {

    protected abstract fun getViewModel(): BaseReportsViewModel // Child classes provide the specific ViewModel
    protected abstract fun getEmptyMessage(): Int // Child classes define specific empty messages
    protected abstract fun navigateToReportScreen(reportInstance: ReportInstance) // Navigation method to be implemented by subclasses
    protected abstract fun initData()
    private val entityAdapter: EntityAdapter by lazy { EntityAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.draftsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.draftsRecyclerView.adapter = entityAdapter

        // Initialize RecyclerView and Adapter
        initData()
        observeViewModel()
    }

    // Observe ViewModel data changes (e.g., report lists, progress, errors)
    private fun observeViewModel() {

        getViewModel().progress.observe(viewLifecycleOwner) { isLoading ->
            showProgress(isLoading)
        }

        getViewModel().error.observe(viewLifecycleOwner) { throwable ->
            showError(throwable)
        }

        getViewModel().reportInstance.observe(viewLifecycleOwner) { instance ->
            openEntityInstance(instance)
        }

        getViewModel().onOpenClickedInstance.observe(viewLifecycleOwner) { instance ->
            loadEntityInstance(instance)
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    protected fun handleReportList(reports: List<ViewEntityTemplateItem>) {
        if (reports.isEmpty()) {
            showEmptyMessage()
        } else {
            entityAdapter.setEntities(reports)
            binding.draftsRecyclerView.show()
            binding.textViewEmpty.hide()
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
        binding.draftsRecyclerView.visibility = View.GONE
    }

    protected fun showMenu(
        instance: ReportInstance,
        title: String,
        viewText: String,
        deleteText: String,
        deleteConfirmation: String,
        deleteActionText: String,
    ) {

        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            title,
            viewText,
            deleteText,
            object : BottomSheetUtils.ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    when (action) {
                        BottomSheetUtils.Action.EDIT -> loadEntityInstance(instance)
                        BottomSheetUtils.Action.DELETE -> getViewModel().deleteReport(instance)
                        BottomSheetUtils.Action.SHARE -> {}
                        BottomSheetUtils.Action.VIEW -> {}
                    }
                }
            },
            deleteActionText,
            deleteConfirmation,
            getString(R.string.action_delete) ,
            getString(R.string.action_cancel),
            R.drawable.ic_eye_white
        )
    }

    // Method to be used by subclasses to define how the navigation is handled
    protected fun openEntityInstance(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        navigateToReportScreen(reportInstance)
    }

    protected fun loadEntityInstance(reportInstance: ReportInstance) {
        getViewModel().getReportBundle(reportInstance)
    }

}
