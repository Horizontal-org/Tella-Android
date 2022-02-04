package rs.readahead.washington.mobile.views.fragment.uwazi.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.UwaziSendFragmentBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.UwaziFormEndView
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener

const val SEND_ENTITY = "send_entity"

class UwaziSendFragment : BaseFragment(), OnNavBckListener {
    private val viewModel: UwaziSendViewModel by viewModels()
    private lateinit var binding: UwaziSendFragmentBinding
    private var entityInstance: UwaziEntityInstance? = null
    private var uwaziServer: UWaziUploadServer? = null
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

            progress.observe(viewLifecycleOwner, { isProgressing ->
                if (isProgressing){
                    entityInstance?.let { showFormSubmitLoading(instance = it) }
                }else{
                    endView.hideUploadProgress("UWAZI_RESPONSE")
                }
            })

            attachments.observe(viewLifecycleOwner,{
                attachmentsList = it
                entityInstance?.widgetMediaFiles = it
                entityInstance?.title = MockUwaziData.getEntityVictimRowMock().title
                entityInstance?.template = MockUwaziData.getEntityVictimRowMock().template
                uwaziServer = UWaziUploadServer().apply {
                    id = 0
                    name = "Tella Uwazi"
                    url = "https://horizontal.uwazi.io/api/"
                    username = "ahlem"
                    password = "episode-siamese-coma"
                    cookies = "connect.sid=s%3ABI_vPZYq9khYeNrPojdgwpsDbwFdCWb9.Oe15PBlcWVNqDkYEdDYPOl3OM7ZuC1Jx4Z9N7NqiYeY; Path=/; HttpOnly"
                }
                showFormEndView(false)
            })

            progressCallBack.observe(viewLifecycleOwner,{
                onShowProgress(it.first,it.second)
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
        entityInstance?.let {
            uwaziServer?.let { it1 ->
                viewModel.submitEntity(
                    server = it1,
                    sendEntityRequest = MockUwaziData.getEntityVictimRowMock(),
                    attachments = attachmentsList)
            }
        }
    }

    private fun onShowProgress(partName : String,total : Float){
        endView.setUploadProgress(partName,total)
    }

    private fun showFormSubmitLoading(instance : UwaziEntityInstance) {
        endView.clearPartsProgress(instance);
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