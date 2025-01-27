package org.horizontal.tella.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showEditDeleteMenuSheet
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentTemplatesUwaziBinding
import org.horizontal.tella.mobile.domain.entity.uwazi.CollectTemplate
import org.horizontal.tella.mobile.views.adapters.uwazi.UwaziTemplatesAdapter
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.COLLECT_TEMPLATE


class TemplatesUwaziFragment : BaseBindingFragment<FragmentTemplatesUwaziBinding>(
    FragmentTemplatesUwaziBinding::inflate
) {
    private val viewModel: SharedUwaziViewModel by viewModels()
    private val uwaziTemplatesAdapter: UwaziTemplatesAdapter by lazy { UwaziTemplatesAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hasInitializedRootView) {
            hasInitializedRootView = true
            initView()
        }
        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {

            templates.observe(viewLifecycleOwner) {
                if (it.size == 1) {
                    binding.textViewEmpty.isVisible = true
                    binding.templatesRecyclerView.isVisible = false
                } else {
                    binding.textViewEmpty.isVisible = false
                    binding.templatesRecyclerView.isVisible = true
                    uwaziTemplatesAdapter.setEntityTemplates(it)
                }
            }

            showSheetMore.observe(viewLifecycleOwner) {
                showDownloadedMenu(it)
            }

            openEntity.observe(viewLifecycleOwner) {
                openEntity(it)
            }
        }
    }

    private fun initView() {
        binding.templatesRecyclerView.apply {
            layoutManager = LinearLayoutManager(baseActivity)
            adapter = uwaziTemplatesAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.listTemplates()
    }

    private fun showDownloadedMenu(template: CollectTemplate) {
        showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            template.entityRow.name,
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
            getString(R.string.action_delete) + " \"" + template.entityRow.name + "\"?",
            requireContext().resources.getString(R.string.Uwazi_Subtitle_RemoveTemplate),
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel)
        )
    }

    private fun openEntity(template: CollectTemplate) {
        val gsonTemplate = Gson().toJson(template)
        bundle.putString(COLLECT_TEMPLATE, gsonTemplate)
        navManager().navigateFromUwaziScreenToUwaziEntryScreen()
    }

}