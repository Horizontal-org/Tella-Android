package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentDraftsUwaziBinding
import rs.readahead.washington.mobile.databinding.FragmentSubmittedUwaziBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziDraftsAdapter

class SubmittedUwaziFragment : UwaziListFragment() {

    override fun getFormListType(): Type {
        return Type.SUBMITTED
    }

    private val viewModel: SharedUwaziViewModel by viewModels()
    private val uwaziDraftsAdapter: UwaziDraftsAdapter by lazy { UwaziDraftsAdapter() }
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
                    uwaziDraftsAdapter.setEntityDrafts(it)
                }

            })

            progress.observe(viewLifecycleOwner, {
                binding?.progressCircular?.isVisible = it
            })

            showInstanceSheetMore.observe(viewLifecycleOwner, {
                showDraftsMenu(it)
            })
        }
    }

    private fun showDraftsMenu(instance: UwaziEntityInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.title,
            getString(R.string.Uwazi_Action_FillEntity),
            getString(R.string.Uwazi_Action_RemoveTemplate),
            object : BottomSheetUtils.ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {

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
            adapter = uwaziDraftsAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.submittedRecyclerView?.adapter = null
        binding = null
    }
}