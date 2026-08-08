package org.horizontal.tella.mobile.views.fragment.uwazi.viewpagerfragments

import androidx.fragment.app.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateDraftTitle
import org.horizontal.tella.mobile.views.fragment.uwazi.UwaziViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UWAZI_INSTANCE

@AndroidEntryPoint
class DraftsUwaziFragment : BaseReportsFragment<UwaziEntityInstance>() {

    private val draftUwaziViewModel: UwaziViewModel by viewModels()

    override fun getViewModel(): UwaziViewModel {
        return draftUwaziViewModel
    }

    override fun getEmptyMessage(): Int {
        return R.string.Uwazi_Draft_Entities_Empty_Description
    }

    override fun getHeaderRecyclerViewMessage(): Int {
        return R.string.Uwazi_Drafts_Header_Text
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_uwazi
    }

    override fun navigateToReportScreen(reportInstance: UwaziEntityInstance) {
        bundle.putString(UWAZI_INSTANCE, Gson().toJson(reportInstance))
        this.navManager().navigateFromUwaziScreenToUwaziEntryScreen()
    }

    override fun initData() {
        with(draftUwaziViewModel) {
            draftListReportFormInstance.observe(viewLifecycleOwner) { drafts ->
                handleReportList(drafts)
                updateDraftTitle.postValue(drafts.size)
            }

            onMoreClickedInstance.observe(viewLifecycleOwner) { instance ->
                showMenu(
                    instance = instance,
                    title = instance.title,
                    viewText = getString(R.string.Uwazi_Action_EditDraft),
                    deleteText = getString(R.string.action_delete),
                    deleteConfirmation = getString(R.string.Uwazi_Subtitle_RemoveDraft),
                    deleteActionText = getString(R.string.action_delete) + " \"" + instance.title + "\"?",
                    confirmButtonLabel = getString(R.string.action_yes),
                    cancelButtonLabel = getString(R.string.action_no),
                )
            }

            instanceDeleted.observe(viewLifecycleOwner) { deletedTitle ->
                DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(R.string.Uwazi_Entity_Deleted_Toast, deletedTitle),
                    false
                )
                listDrafts()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        draftUwaziViewModel.listDrafts()
    }
}
