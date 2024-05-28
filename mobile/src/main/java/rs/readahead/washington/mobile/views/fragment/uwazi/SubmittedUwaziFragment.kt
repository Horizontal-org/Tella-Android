package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSubmittedUwaziBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziSubmittedAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY

class SubmittedUwaziFragment : BaseBindingFragment<FragmentSubmittedUwaziBinding>(
    FragmentSubmittedUwaziBinding::inflate
) {

    private val viewModel: SharedUwaziViewModel by viewModels()
    private val adapterSubmitted: UwaziSubmittedAdapter by lazy { UwaziSubmittedAdapter() }

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

            submittedInstances.observe(viewLifecycleOwner) {
                if (it.isEmpty()) {
                    binding?.textViewEmptyOutbox?.isVisible = true
                    binding?.submittedRecyclerView?.isVisible = false
                } else {
                    (it as ArrayList).add(0, getString(R.string.Uwazi_Submitted_Header_Text))
                    binding?.textViewEmptyOutbox?.isVisible = false
                    adapterSubmitted.setEntities(it)
                }

            }

            showInstanceSheetMore.observe(viewLifecycleOwner) {
                showDraftsMenu(it)
            }

            onInstanceSuccess.observe(viewLifecycleOwner) {
                openEntityInstance(it)
            }

            openEntityInstance.observe(viewLifecycleOwner) {
                openEntityInstance(it)
            }

            instanceDeleteD.observe(viewLifecycleOwner) {
                listSubmitted()
            }
        }
    }

    private fun showDraftsMenu(instance: UwaziEntityInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.View_Report),
            getString(R.string.Uwazi_Action_DeleteEntity),
            object : BottomSheetUtils.ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        viewModel.getInstanceUwaziEntity(instance.id)
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        viewModel.confirmDelete(instance)
                    }
                }
            },
            getString(R.string.action_delete) + " \"" + instance.title + "\"?",
            requireContext().resources.getString(R.string.Uwazi_Subtitle_RemoveDraft),
            requireContext().getString(R.string.action_delete),
            requireContext().getString(R.string.action_cancel),
            iconView = R.drawable.ic_eye_white
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.listSubmitted()
    }

    private fun initView() {
        binding?.submittedRecyclerView?.apply {
            layoutManager = LinearLayoutManager(baseActivity)
            adapter = adapterSubmitted
        }
    }

    private fun openEntityInstance(entityInstance: UwaziEntityInstance) {
        val bundle = Bundle()
        bundle.putString(SEND_ENTITY, Gson().toJson(entityInstance))
        navManager().navigateFromUwaziScreenToUwaziSubmitedPreview()
//        NavHostFragment.findNavController(this)
//            .navigate(R.id.action_uwaziScreen_to_uwaziSubmittedPreview, bundle)
    }

}