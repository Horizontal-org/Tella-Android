package org.horizontal.tella.mobile.views.fragment.uwazi.viewpagerfragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateSubmittedTitle
import org.horizontal.tella.mobile.views.fragment.uwazi.UwaziViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.send.SEND_ENTITY

@AndroidEntryPoint
class SubmittedUwaziFragment : BaseReportsFragment<UwaziEntityInstance>() {

    private val submittedUwaziViewModel: UwaziViewModel by viewModels()

    override fun getViewModel(): UwaziViewModel {
        return submittedUwaziViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun getEmptyMessage(): Int {
        return R.string.Uwazi_Submitted_Entities_Empty_Description
    }

    override fun getHeaderRecyclerViewMessage(): Int {
        return R.string.Uwazi_Submitted_Header_Text
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_uwazi
    }

    override fun navigateToReportScreen(reportInstance: UwaziEntityInstance) {
        bundle.putString(SEND_ENTITY, Gson().toJson(reportInstance))
        this.navManager().navigateFromUwaziScreenToUwaziSubmitedPreview()
    }

    override fun initData() {
        with(submittedUwaziViewModel) {
            submittedReportListFormInstance.observe(viewLifecycleOwner) { submitted ->
                updateSubmittedTitle.postValue(submitted.size)
            }

            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showMenu(
                    instance = instance,
                    title = instance.title,
                    viewText = getString(R.string.Uwazi_Action_ViewEntity),
                    deleteText = getString(R.string.Uwazi_Action_DeleteEntity),
                    deleteConfirmation = getString(R.string.Uwazi_Subtitle_RemoveSubmittedEntity),
                    deleteActionText = getString(R.string.Uwazi_DeleteEntity_SheetTitle)
                )
            }

            instanceDeleted.observe(viewLifecycleOwner) { deletedTitle ->
                DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(R.string.Uwazi_Entity_Deleted_Toast, deletedTitle),
                    false
                )
                listSubmitted()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        submittedUwaziViewModel.listSubmitted()
    }
}
