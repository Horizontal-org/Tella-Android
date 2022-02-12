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
import rs.readahead.washington.mobile.databinding.FragmentOutboxUwaziBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziDraftsAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziSubmittedAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UWAZI_INSTANCE

class OutboxUwaziFragment : UwaziListFragment() {

    private val viewModel: SharedUwaziViewModel by viewModels()
    private val outboxAdapter: UwaziSubmittedAdapter by lazy { UwaziSubmittedAdapter() }
    private var binding: FragmentOutboxUwaziBinding? = null
    private val bundle by lazy { Bundle() }

    override fun getFormListType(): Type {
        return Type.OUTBOX
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOutboxUwaziBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {
            outboxInstances.observe(viewLifecycleOwner, {
                if (it.isEmpty()) {
                    binding?.textViewEmpty?.isVisible = true
                    binding?.outboxRecyclerView?.isVisible = false
                    outboxAdapter.setEntities(emptyList())
                } else {
                    binding?.textViewEmpty?.isVisible = false
                    binding?.outboxRecyclerView?.isVisible = true
                    outboxAdapter.setEntities(it)
                }
            })

            instanceDeleteD.observe(viewLifecycleOwner,{
                listOutBox()
            })

            showInstanceSheetMore.observe(viewLifecycleOwner, {
                showDraftsMenu(it)
            })

            openEntityInstance.observe(viewLifecycleOwner,{
                openEntity(it)
            })

            onInstanceSuccess.observe(viewLifecycleOwner,{
                editEntity(it)
            })
        }
    }

    private fun showDraftsMenu(instance: UwaziEntityInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.Uwazi_Action_EditDraft),
            getString(R.string.Uwazi_Action_RemoveTemplate),
            object : BottomSheetUtils.ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        /*   val gsonTemplate = Gson().toJson(template)
                           bundle.putString(COLLECT_TEMPLATE, gsonTemplate)
                           NavHostFragment.findNavController(this@TemplatesUwaziFragment)
                               .navigate(R.id.action_uwaziScreen_to_uwaziEntryScreen, bundle)*/
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

    private fun openEntity(entityInstance: UwaziEntityInstance){
        viewModel.getInstanceUwaziEntity(entityInstance.id)
        //DialogUtils.showBottomMessage(activity,"This functionality is not yet implemented",true)
    }

    private fun editEntity(entityInstance: UwaziEntityInstance){
        val gsonTemplate = Gson().toJson(entityInstance)
        bundle.putString(UWAZI_INSTANCE, gsonTemplate)
        NavHostFragment.findNavController(this)
            .navigate(R.id.action_uwaziScreen_to_uwaziEntryScreen, bundle)
    }

    override fun onResume() {
        super.onResume()
        viewModel.listOutBox()
    }

    private fun initView() {
        binding?.outboxRecyclerView?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = outboxAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.outboxRecyclerView?.adapter = null
        binding = null
    }
}