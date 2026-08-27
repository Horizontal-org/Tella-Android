package org.horizontal.tella.mobile.views.fragment.uwazi.viewpagerfragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showEditDeleteMenuSheet
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentReportsListBinding
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.views.adapters.uwazi.UwaziTemplatesAdapter
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.EmptyMessageVisibilityHandler
import org.horizontal.tella.mobile.views.fragment.uwazi.UwaziViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.COLLECT_TEMPLATE

/**
 * Uwazi's extra first tab. It lists downloaded templates rather than instances, so it does not
 * extend [org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsFragment],
 * but it reuses the same list layout and empty-state handling as the other tabs.
 */
@AndroidEntryPoint
class TemplatesUwaziFragment : BaseBindingFragment<FragmentReportsListBinding>(
    FragmentReportsListBinding::inflate
) {
    private val viewModel: UwaziViewModel by viewModels()
    private val uwaziTemplatesAdapter: UwaziTemplatesAdapter by lazy { UwaziTemplatesAdapter() }
    private var visibilityHandler: EmptyMessageVisibilityHandler? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        visibilityHandler = parentFragment as? EmptyMessageVisibilityHandler
    }

    override fun onDetach() {
        super.onDetach()
        visibilityHandler = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {
            progress.observe(viewLifecycleOwner) { isLoading ->
                binding.progress.isVisible = isLoading
            }

            templates.observe(viewLifecycleOwner) { items ->
                val isEmpty = items.size <= 1
                binding.draftsRecyclerView.isVisible = !isEmpty
                visibilityHandler?.setEmptyTextViewMessageVisibility(isEmpty)
                if (!isEmpty) {
                    uwaziTemplatesAdapter.setEntityTemplates(items)
                }
            }

            showSheetMore.observe(viewLifecycleOwner) { showDownloadedMenu(it) }

            openEntity.observe(viewLifecycleOwner) { openEntity(it) }

            templateRemoved.observe(viewLifecycleOwner) { templateName ->
                DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(R.string.Uwazi_Template_Removed_Toast, templateName),
                    false
                )
            }
        }
    }

    private fun initView() {
        binding.draftsRecyclerView.apply {
            layoutManager = LinearLayoutManager(baseActivity)
            adapter = uwaziTemplatesAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.listTemplates()
    }

    private fun showDownloadedMenu(template: UwaziTemplate) {
        val templateName = template.entityRow.name
        showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            templateName,
            getString(R.string.Uwazi_Action_FillEntity),
            getString(R.string.Uwazi_Action_RemoveTemplate),
            object : ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        openEntity(template)
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        viewModel.confirmDelete(template)
                    }
                }
            },
            getString(R.string.Uwazi_RemoveTemplate_ConfirmTitle, templateName),
            getString(R.string.Uwazi_Subtitle_RemoveTemplate),
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel)
        )
    }

    private fun openEntity(template: UwaziTemplate) {
        bundle.putString(COLLECT_TEMPLATE, Gson().toJson(template))
        navManager().navigateFromUwaziScreenToUwaziEntryScreen()
    }
}
