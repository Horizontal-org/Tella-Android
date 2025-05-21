package org.horizontal.tella.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentSumbittedPreviewBinding
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.uwazi.send.SEND_ENTITY
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.UwaziFormEndView

@AndroidEntryPoint
class SubmittedPreviewFragment : BaseBindingFragment<FragmentSumbittedPreviewBinding>(
    FragmentSumbittedPreviewBinding::inflate
) {
    private val viewModel: SharedUwaziViewModel by viewModels()
    private lateinit var endView: UwaziFormEndView
    private var submittedInstance: UwaziEntityInstance? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
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
            baseActivity.supportFragmentManager,
            entityInstance.title,
            getString(R.string.Uwazi_RemoveTemplate_SheetTitle)
        ) { viewModel.confirmDelete(entityInstance) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.listSubmitted()
    }


    fun initView() {
        arguments?.get(SEND_ENTITY)?.let { entity ->
            submittedInstance = Gson().fromJson(entity as String, UwaziEntityInstance::class.java)
            submittedInstance?.let {
                endView = UwaziFormEndView(baseActivity, getFormattedFormTitle(submittedInstance!!))

                endView.setInstance(it, true, true)
                binding.endViewContainer.removeAllViews()
                binding.endViewContainer.addView(endView)
            }

        }
        binding.toolbar.onRightClickListener =
            { submittedInstance?.let { showDeleteBottomSheet(it) } }
        binding.toolbar.backClickListener = {
            nav().popBackStack()
        }
    }

    private fun getFormattedFormTitle(entityInstance: UwaziEntityInstance): String {
        return getString(R.string.Uwazi_Server_Title) + " " + entityInstance.collectTemplate?.serverName + "\n" + getString(
            R.string.Uwazi_Template_Title
        ) + " " + entityInstance.collectTemplate?.entityRow?.translatedName
    }
}