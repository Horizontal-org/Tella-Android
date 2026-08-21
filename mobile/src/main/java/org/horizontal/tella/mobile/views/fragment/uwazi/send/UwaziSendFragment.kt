package org.horizontal.tella.mobile.views.fragment.uwazi.send

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.UwaziSendFragmentBinding
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.uwazi.SharedLiveData
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.BUNDLE_IS_FROM_UWAZI_ENTRY
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.SharedUwaziSubmissionViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.OUTBOX_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.UwaziFormEndView
import org.horizontal.tella.mobile.views.fragment.vault.attachements.OnNavBckListener

const val SEND_ENTITY = "send_entity"

class UwaziSendFragment :
    BaseBindingFragment<UwaziSendFragmentBinding>(UwaziSendFragmentBinding::inflate),
    OnNavBckListener {
    private val viewModel : SharedUwaziSubmissionViewModel by viewModels()

    private var entityInstance: UwaziEntityInstance? = null
    private var uwaziServer: UWaziUploadServer? = null
    private lateinit var endView: UwaziFormEndView
    private var isFromEntryScreen = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initView()
    }

    private fun initView() {
        with(binding) {
            toolbar.backClickListener = { nav().popBackStack() }

            nextBtn.setOnClickListener {
                entityInstance?.let {
                    submitEntity()
                }
            }

            cancelBtn.setOnClickListener {
                entityInstance?.let { entity ->
                    if (entity.status != EntityStatus.SUBMISSION_PENDING) {
                        entity.status = EntityStatus.SUBMISSION_PENDING
                        viewModel.saveEntityInstance(entity)
                    } else {
                        handleBackButton()
                    }
                }
            }
        }
        arguments?.let { bundle ->
            entityInstance =
                Gson().fromJson(bundle.getString(SEND_ENTITY), UwaziEntityInstance::class.java)
            isFromEntryScreen = bundle.getBoolean(BUNDLE_IS_FROM_UWAZI_ENTRY)
            showFormEndView()
        }
    }

    private fun initObservers() {
        with(viewModel) {
            server.observe(viewLifecycleOwner) {
                uwaziServer = it
            }

            progressCallBack.observe(viewLifecycleOwner) {
                onShowProgress(it.first, it.second)
            }

            progress.observe(viewLifecycleOwner) { status ->
                when (status) {
                    EntityStatus.SUBMITTED -> {
                        baseActivity.divviupUtils.runUwaziSentEvent()
                        handleBackButton()
                        DialogUtils.showBottomMessage(
                            baseActivity,
                            getString(R.string.form_successfully_submitted, entityInstance?.title),
                            false
                        )
                    }

                    EntityStatus.SUBMISSION_ERROR -> {
                        DialogUtils.showBottomMessage(
                            baseActivity,
                            getString(R.string.collect_toast_fail_sending_form),
                            true
                        )
                        entityInstance?.status = EntityStatus.SUBMISSION_ERROR
                        entityInstance?.let { viewModel.saveEntityInstance(it) }
                        handleBackButton()
                        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
                    }

                    EntityStatus.SUBMISSION_PENDING -> {
                        handleBackButton()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun handleBackButton(): Boolean {
        return if (isFromEntryScreen) {
            nav().popBackStack(R.id.uwaziEntryScreen, true)
        } else {
            nav().popBackStack()
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

    private fun onShowProgress(partName: String, total: Float) {
        endView.showUploadProgress(partName)
        endView.setUploadProgress(partName, total)
    }

    private fun showFormEndView() {
        if (entityInstance == null) {
            return
        }

        endView = UwaziFormEndView(baseActivity, getFormattedFormTitle(entityInstance!!))
        endView.setInstance(entityInstance!!, false, false)
        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
        //  updateFormSubmitButton(false)
    }

    private fun getFormattedFormTitle(entityInstance: UwaziEntityInstance): String {
        return getString(R.string.Uwazi_Server_Title) + " " + entityInstance.collectTemplate?.serverName + "\n" + getString(
            R.string.Uwazi_Template_Title
        ) + " " + entityInstance.collectTemplate?.entityRow?.translatedName
    }
}