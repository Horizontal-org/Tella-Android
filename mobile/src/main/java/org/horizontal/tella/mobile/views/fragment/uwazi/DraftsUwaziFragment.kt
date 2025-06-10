package org.horizontal.tella.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentDraftsUwaziBinding
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.uwazi.adapters.UwaziDraftsAdapter
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UWAZI_INSTANCE

@AndroidEntryPoint
class DraftsUwaziFragment :
    BaseBindingFragment<FragmentDraftsUwaziBinding>(FragmentDraftsUwaziBinding::inflate) {
    private val viewModel: SharedUwaziViewModel by viewModels()
    private val uwaziDraftsAdapter: UwaziDraftsAdapter by lazy { UwaziDraftsAdapter() }

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
            draftInstances.observe(viewLifecycleOwner) {
                if (it.isEmpty()) {
                    binding.textViewEmpty.isVisible = true
                    binding.draftsRecyclerView.isVisible = false
                    uwaziDraftsAdapter.setEntityDrafts(emptyList())
                } else {
                    (it as ArrayList).add(0, getString(R.string.Uwazi_Drafts_Header_Text))
                    binding.textViewEmpty.isVisible = false
                    binding.draftsRecyclerView.isVisible = true
                    uwaziDraftsAdapter.setEntityDrafts(it)
                }
            }

            showInstanceSheetMore.observe(viewLifecycleOwner) {
                showDraftsMenu(it)
            }

            openEntityInstance.observe(viewLifecycleOwner) {
                openDraft(it)
            }

            onInstanceSuccess.observe(viewLifecycleOwner) {
                editDraft(it)
            }

            instanceDeleteD.observe(viewLifecycleOwner) {
                listDrafts()
            }
        }
    }

    private fun showDraftsMenu(instance: UwaziEntityInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.Uwazi_Action_EditDraft),
            getString(R.string.action_delete),
            object : BottomSheetUtils.ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        openDraft(instance)
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        viewModel.confirmDelete(instance)
                    }
                }
            },
            getString(R.string.action_delete) + " \"" + instance.title + "\"?",
            requireContext().resources.getString(R.string.Uwazi_Subtitle_RemoveDraft),
            requireContext().getString(R.string.action_delete),
            requireContext().getString(R.string.action_cancel)
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.listDrafts()
    }

    private fun initView() {
        binding.draftsRecyclerView.apply {
            layoutManager = LinearLayoutManager(baseActivity)
            adapter = uwaziDraftsAdapter
        }
    }

    private fun openDraft(entityInstance: UwaziEntityInstance) {
        viewModel.getInstanceUwaziEntity(entityInstance.id)
    }

    private fun editDraft(entityInstance: UwaziEntityInstance) {
        val gsonTemplate = Gson().toJson(entityInstance)
        bundle.putString(UWAZI_INSTANCE, gsonTemplate)
        navManager().navigateFromUwaziScreenToUwaziEntryScreen()
    }

}