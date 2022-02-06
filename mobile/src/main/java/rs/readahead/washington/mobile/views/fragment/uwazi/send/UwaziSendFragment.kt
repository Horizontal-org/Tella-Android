package rs.readahead.washington.mobile.views.fragment.uwazi.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.UwaziSendFragmentBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.SharedUwaziSubmissionViewModel
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.DRAFT_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.OUTBOX_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.UwaziFormEndView
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener

const val SEND_ENTITY = "send_entity"

class UwaziSendFragment : BaseFragment(), OnNavBckListener {
    private val viewModel: SharedUwaziSubmissionViewModel by lazy {
        ViewModelProvider(activity).get(SharedUwaziSubmissionViewModel::class.java)
    }
    private lateinit var binding: UwaziSendFragmentBinding
    private var entityInstance: UwaziEntityInstance? = null
    private var uwaziServer: UWaziUploadServer? = null
    private lateinit var endView: UwaziFormEndView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UwaziSendFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initView()
    }

    override fun initView(view: View) {
        with(binding) {
            toolbar.backClickListener = { nav().popBackStack() }

            nextBtn.setOnClickListener {
                entityInstance?.let {
                    submitEntity()
                }
            }

            cancelBtn.setOnClickListener {
                SharedLiveData.updateViewPagerPosition.postValue(DRAFT_LIST_PAGE_INDEX)
            }
        }
    }

    private fun initObservers() {
        with(viewModel) {
            server.observe(viewLifecycleOwner, {
                uwaziServer = it
            })

            progressCallBack.observe(viewLifecycleOwner,{
                onShowProgress(it.first,it.second)
            })

            progress.observe(viewLifecycleOwner,{ status ->
             when(status){

                 UwaziEntityStatus.SUBMITTED -> {
                     nav().popBackStack()
                     SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
                 }

                 UwaziEntityStatus.SUBMISSION_ERROR -> {
                     binding.cancelBtn.isVisible = true
                     DialogUtils.showBottomMessage(activity,getString(R.string.collect_toast_fail_sending_form),true)
                     SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
                 }

                 UwaziEntityStatus.SUBMISSION_PENDING -> {
                     binding.cancelBtn.isVisible = true
                     SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
                 }
             }
            })
        }
    }

    private fun initView() {
        arguments?.let {
            entityInstance = Gson().fromJson(it.getString(SEND_ENTITY), UwaziEntityInstance::class.java)
            showFormEndView(false)
        }
    }

    override fun onBackPressed(): Boolean {
        return nav().popBackStack()
    }

    private fun submitEntity() {
        entityInstance?.let { entity ->
            entity.collectTemplate?.serverId?.let { serverID ->
                viewModel.getUwaziServerAndSaveEntity(
                    serverID,
                    entity
                )
            }
        }
    }

    private fun onShowProgress(partName : String,total : Float){
        endView.showUploadProgress(partName)
        endView.setUploadProgress(partName,total)
    }

    private fun showFormSubmitLoading(instance : UwaziEntityInstance) {
        endView.clearPartsProgress(instance)
    }

    private fun showFormEndView(offline: Boolean) {
        if (entityInstance == null){
            return
        }

        endView = UwaziFormEndView(activity,
            if (entityInstance!!.status == UwaziEntityStatus.SUBMITTED) R.string.collect_end_heading_confirmation_form_submitted else R.string.collect_end_action_submit
        )
        endView.setInstance(entityInstance!!, offline)
        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
      //  updateFormSubmitButton(false)
    }
}