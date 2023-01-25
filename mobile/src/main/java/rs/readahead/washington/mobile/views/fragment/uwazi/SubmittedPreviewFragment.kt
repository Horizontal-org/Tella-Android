package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSumbittedPreviewBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.UwaziFormEndView

class SubmittedPreviewFragment : BaseFragment() {
    private val viewModel: SharedUwaziViewModel by viewModels()
    private var binding: FragmentSumbittedPreviewBinding? = null
    private lateinit var endView: UwaziFormEndView
    private var submittedInstance: UwaziEntityInstance? = null

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
            instanceDeleteD.observe(viewLifecycleOwner) { deleted ->
                if (deleted) nav().popBackStack()
            }
        }
    }

    private fun showDeleteBottomSheet(entityInstance: UwaziEntityInstance) {
        BottomSheetUtils.showConfirmDelete(
            activity.supportFragmentManager,
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
        arguments?.get(SEND_ENTITY)?.let { entity ->
            submittedInstance = Gson().fromJson(entity as String, UwaziEntityInstance::class.java)
            submittedInstance?.let {
                endView = UwaziFormEndView(activity, getFormattedFormTitle(submittedInstance!!))

                endView.setInstance(it, true, true)
                binding?.endViewContainer?.removeAllViews()
                binding?.endViewContainer?.addView(endView)
            }

        }
        binding?.toolbar?.onRightClickListener =
            { submittedInstance?.let { showDeleteBottomSheet(it) } }
        binding?.toolbar?.backClickListener = { nav().popBackStack() }
    }

    private fun getFormattedFormTitle(entityInstance: UwaziEntityInstance): String {
        return getString(R.string.Uwazi_Server_Title) + " " + entityInstance.collectTemplate?.serverName + "\n" + getString(
            R.string.Uwazi_Template_Title
        ) + " " + entityInstance.collectTemplate?.entityRow?.translatedName
    }
}