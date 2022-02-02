package rs.readahead.washington.mobile.views.fragment.uwazi.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.UwaziSendFragmentBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.collect.CollectFormEndView
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.UwaziFormEndView
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener

const val SEND_ENTITY = "send_entity"

class UwaziSendFragment : BaseFragment(), OnNavBckListener {
    private val viewModel: UwaziSendViewModel by viewModels()
    private lateinit var binding: UwaziSendFragmentBinding
    private var entityInstance: UwaziEntityInstance? = null
    private var uwaziServer: UWaziUploadServer? = null
    //TODO WE WILL NEED TO USE FormMediaFile
    private var attachmentsList : List<FormMediaFile>? = null
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
                if (entityInstance != null) submitEntity()
            }

            cancelBtn.setOnClickListener {

            }
        }
    }

    private fun initObservers() {
        with(viewModel) {
            server.observe(viewLifecycleOwner, {
                uwaziServer = it
            })

            progress.observe(viewLifecycleOwner, {
                binding.progressCircular.isVisible = it
            })

            attachments.observe(viewLifecycleOwner,{
                attachmentsList = it
                entityInstance?.widgetMediaFiles = it
                entityInstance?.title = MockUwaziData.getEntityVictimRowMock().title
                entityInstance?.template = MockUwaziData.getEntityVictimRowMock().template
                showFormEndView(false)
            })
        }
    }

    private fun initView() {
        arguments?.let {
            entityInstance =
                Gson().fromJson(it.getString(SEND_ENTITY), UwaziEntityInstance::class.java)
            entityInstance?.collectTemplate?.let { it1 -> viewModel.getUwaziServer(it1.serverId) }
        }
    }

    override fun onBackPressed(): Boolean {
        return nav().popBackStack()
    }

    private fun submitEntity() {
        entityInstance?.collectTemplate?.let {
            uwaziServer?.let { it1 ->
                viewModel.submitEntity(
                    server = it1,
                    sendEntityRequest = MockUwaziData.getEntityVictimRowMock(),
                    attachments = attachmentsList
                )
            }
        }
    }

    private fun showFormEndView(offline: Boolean) {
        if (entityInstance == null){
            return
        }

        endView = UwaziFormEndView(activity,
            if (entityInstance!!.status == UwaziEntityStatus.SUBMITTED) R.string.collect_end_heading_confirmation_form_submitted else R.string.collect_end_action_submit
        )
        entityInstance?.let { endView.setInstance(it, offline) }
        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
      //  updateFormSubmitButton(false)
    }
}