package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSubmittedUwaziBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziDraftsAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziSubmittedAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY

class SubmittedUwaziFragment : UwaziListFragment() {

    override fun getFormListType(): Type {
        return Type.SUBMITTED
    }

    private val viewModel: SharedUwaziViewModel by viewModels()
    private val adapterSubmitted: UwaziSubmittedAdapter by lazy { UwaziSubmittedAdapter() }
    private var binding: FragmentSubmittedUwaziBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubmittedUwaziBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {

            submittedInstances.observe(viewLifecycleOwner, {
                if (it.isEmpty()) {
                    binding?.textViewEmptyOutbox?.isVisible = true
                } else {
                    binding?.textViewEmptyOutbox?.isVisible = false
                    adapterSubmitted.setEntities(it)
                }

            })

            showInstanceSheetMore.observe(viewLifecycleOwner, {
                showDraftsMenu(it)
            })

            onInstanceSuccess.observe(viewLifecycleOwner,{
                openEntityInstance(it)
            })
        }
    }

    private fun showDraftsMenu(instance: UwaziEntityInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.Uwazi_Action_ViewEntity),
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
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel)
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.listSubmitted()
    }

    private fun initView() {
        binding?.submittedRecyclerView?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = adapterSubmitted
        }
    }

    private fun openEntityInstance(entityInstance: UwaziEntityInstance) {
        val bundle = Bundle()
        bundle.putString(SEND_ENTITY, Gson().toJson(entityInstance))
        NavHostFragment.findNavController(this)
            .navigate(R.id.action_uwaziScreen_to_uwaziSubmittedPreview, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.submittedRecyclerView?.adapter = null
        binding = null
    }
}