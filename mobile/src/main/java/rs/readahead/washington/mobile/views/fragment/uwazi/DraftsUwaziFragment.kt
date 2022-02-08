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
import rs.readahead.washington.mobile.databinding.FragmentDraftsUwaziBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziDraftsAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UWAZI_INSTANCE

class DraftsUwaziFragment : UwaziListFragment() {
    private val viewModel: SharedUwaziViewModel by viewModels()
    private val uwaziDraftsAdapter: UwaziDraftsAdapter by lazy { UwaziDraftsAdapter() }
    private var binding: FragmentDraftsUwaziBinding? = null
    private val bundle by lazy { Bundle() }

    override fun getFormListType(): Type {
        return Type.DRAFT
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDraftsUwaziBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {
            draftInstances.observe(viewLifecycleOwner, {
                if (it.isEmpty()) {
                    binding?.textViewEmpty?.isVisible = true
                    binding?.draftsRecyclerView?.isVisible = false
                    uwaziDraftsAdapter.setEntityDrafts(emptyList())
                } else {
                    binding?.textViewEmpty?.isVisible = false
                    binding?.draftsRecyclerView?.isVisible = true
                    uwaziDraftsAdapter.setEntityDrafts(it)
                }
            })

            showInstanceSheetMore.observe(viewLifecycleOwner, {
                showDraftsMenu(it)
            })

            openEntityInstance.observe(viewLifecycleOwner,{
                openDraft(it)
            })

            onInstanceSuccess.observe(viewLifecycleOwner,{
                editDraft(it)
            })
        }
    }

    private fun showDraftsMenu(instance: UwaziEntityInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.Uwazi_Action_EditDraft),
            getString(R.string.Uwazi_Action_RemoveDraft),
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
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel)
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.listDrafts()
    }

    private fun initView() {
        binding?.draftsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = uwaziDraftsAdapter
        }
    }

    private fun openDraft(entityInstance: UwaziEntityInstance){
        viewModel.getInstanceUwaziEntity(entityInstance.id)
        //DialogUtils.showBottomMessage(activity,"This functionality is not yet implemented",true)
    }

    private fun editDraft(entityInstance: UwaziEntityInstance){
        val gsonTemplate = Gson().toJson(entityInstance)
        bundle.putString(UWAZI_INSTANCE, gsonTemplate)
        NavHostFragment.findNavController(this@DraftsUwaziFragment)
            .navigate(R.id.action_uwaziScreen_to_uwaziEntryScreen, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.draftsRecyclerView?.adapter = null
        binding = null
    }
}