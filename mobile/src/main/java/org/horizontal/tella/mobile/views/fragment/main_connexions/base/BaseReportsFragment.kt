package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentReportsListBinding
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.util.show
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateOutboxTitle
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateSubmittedTitle
import org.horizontal.tella.mobile.views.fragment.reports.adapter.EntityAdapter
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem

abstract class BaseReportsFragment<VM : BaseReportsViewModel> :
    BaseBindingFragment<FragmentReportsListBinding>(FragmentReportsListBinding::inflate) {

    // Child classes provide the specific ViewModel through this method
    protected abstract fun getViewModel(): VM
    protected abstract fun getEmptyMessage(): Int // Child classes define specific empty messages
    protected abstract fun getHeaderRecyclerViewMessage(): Int
    protected abstract fun getEmptyMessageIcon(): Int
    protected abstract fun navigateToReportScreen(reportInstance: ReportInstance) // Navigation method to be implemented by subclasses
    protected abstract fun initData()
    private val entityAdapter: EntityAdapter by lazy { EntityAdapter() }

    private var visibilityHandler: EmptyMessageVisibilityHandler? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView and Adapter
        initData()
        observeViewModel()
        setUpRecyclerView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the parent fragment implements the EmptyMessageVisibilityHandler interface
        visibilityHandler = parentFragment as? EmptyMessageVisibilityHandler
    }

    override fun onDetach() {
        super.onDetach()
        visibilityHandler = null
    }


    private fun setUpRecyclerView() {
        binding.draftsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.draftsRecyclerView.adapter = entityAdapter
    }

    // Observe ViewModel data changes (e.g., report lists, progress, errors)
    @SuppressLint("StringFormatInvalid")
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

        getViewModel().outboxReportListFormInstance.observe(viewLifecycleOwner) { outboxes ->
            handleReportList(outboxes)
            updateOutboxTitle.postValue(outboxes.size)
        }


        getViewModel().submittedReportListFormInstance.observe(viewLifecycleOwner) { submitted ->
            handleReportList(submitted)
            updateSubmittedTitle.postValue(submitted.size)
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    protected fun handleReportList(reports: List<ViewEntityTemplateItem>) {
        if (reports.isEmpty()) {
            showEmptyMessage()
        } else {
            entityAdapter.setEntities(
                reports, if (getHeaderRecyclerViewMessage() != -1) {
                    getString(getHeaderRecyclerViewMessage())
                } else ""
            )
            binding.draftsRecyclerView.show()
            visibilityHandler?.setEmptyTextViewMessageVisibility(false)
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
        visibilityHandler?.setEmptyTextViewMessageVisibility(true)
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
            getString(R.string.action_delete),
            getString(R.string.action_cancel),
            R.drawable.ic_eye_white
        )
    }

    // Method to be used by subclasses to define how the navigation is handled
    private fun openEntityInstance(reportInstance: ReportInstance) {
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        navigateToReportScreen(reportInstance)
    }

    protected fun loadEntityInstance(reportInstance: ReportInstance) {
        getViewModel().getReportBundle(reportInstance)
    }

}
