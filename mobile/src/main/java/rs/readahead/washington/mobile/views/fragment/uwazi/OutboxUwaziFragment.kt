package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziSubmittedAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY

class OutboxUwaziFragment : UwaziListFragment() {

    private val viewModel: SharedUwaziViewModel by viewModels()
    private val outboxAdapter: UwaziSubmittedAdapter by lazy { UwaziSubmittedAdapter() }

    override fun getFormListType(): Type {
        return Type.OUTBOX
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {
            outboxInstances.observe(viewLifecycleOwner) {
                if (it.isEmpty()) {
                    binding.textViewEmpty.isVisible = true
                    binding.outboxRecyclerView.isVisible = false
                    outboxAdapter.setEntities(emptyList())
                } else {
                    (it as ArrayList).add(0, getString(R.string.Uwazi_Outbox_Header_Text))
                    binding.textViewEmpty.isVisible = false
                    binding.outboxRecyclerView.isVisible = true
                    outboxAdapter.setEntities(it)
                }
            }

            instanceDeleteD.observe(viewLifecycleOwner) {
                listOutBox()
            }

            showInstanceSheetMore.observe(viewLifecycleOwner) {
                showDraftsMenu(it)
            }

            openEntityInstance.observe(viewLifecycleOwner) {
                openEntity(it)
            }

            onInstanceSuccess.observe(viewLifecycleOwner) {
                editEntity(it)
            }
        }
    }

    private fun showDraftsMenu(instance: UwaziEntityInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.Uwazi_Action_ViewEntity),
            getString(R.string.Uwazi_Action_RemoveTemplate),
            object : BottomSheetUtils.ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        openEntity(instance)
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

    private fun openEntity(entityInstance: UwaziEntityInstance) {
        viewModel.getInstanceUwaziEntity(entityInstance.id)
    }

    private fun editEntity(entityInstance: UwaziEntityInstance) {
        val gsonTemplate = Gson().toJson(entityInstance)
        bundle.putString(SEND_ENTITY, gsonTemplate)
        navManager().navigateFromUwaziEntryToSendScreen()
    }

    override fun onResume() {
        super.onResume()
        viewModel.listOutBox()
    }

    private fun initView() {
        binding.outboxRecyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = outboxAdapter
        }
    }
}