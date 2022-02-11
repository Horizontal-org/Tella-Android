package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSumbittedPreviewBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.UwaziDraftsAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.UwaziFormEndView

class SubmittedPreviewFragment : BaseFragment(){
    private val viewModel: SharedUwaziViewModel by viewModels()
    private var binding: FragmentSumbittedPreviewBinding? = null
    private lateinit var endView: UwaziFormEndView
    private var submittedInstance : UwaziEntityInstance? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSumbittedPreviewBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {
            instanceDeleteD.observe(viewLifecycleOwner,{ deleted->
                if (deleted) nav().popBackStack()
            })
        }
    }

    private fun showDeleteBottomSheet(entityInstance: UwaziEntityInstance){
        BottomSheetUtils.showConfirmDelete(activity.supportFragmentManager,
            entityInstance.title,
            getString(R.string.Uwazi_RemoveTemplate_SheetTitle)
        ) { viewModel.confirmDelete(entityInstance) }
    }
    override fun onResume() {
        super.onResume()
        viewModel.listSubmitted()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
    override fun initView(view: View) {
        endView = UwaziFormEndView(activity,R.string.collect_end_heading_confirmation_form_submitted)
        arguments?.get(SEND_ENTITY)?.let { entity ->
            submittedInstance = Gson().fromJson(entity as String ,UwaziEntityInstance::class.java)
            submittedInstance?.let {
                endView.setInstance(it,true, true)
                binding?.endViewContainer?.removeAllViews()
                binding?.endViewContainer?.addView(endView)
            }

        }
        binding?.toolbar?.onRightClickListener = { submittedInstance?.let { showDeleteBottomSheet(it) } }
        binding?.toolbar?.backClickListener = {nav().popBackStack()}
    }
}