package rs.readahead.washington.mobile.views.fragment.uwazi.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import rs.readahead.washington.mobile.databinding.UwaziSendFragmentBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener
import timber.log.Timber

const val SEND_ENTITY = "send_entity"

class UwaziSendFragment : BaseFragment(), OnNavBckListener {
    private val viewModel : UwaziSendViewModel by viewModels()
    private lateinit var binding: UwaziSendFragmentBinding
    private var entityInstance: UwaziEntityInstance? = null
    private var uwaziServer : UWaziUploadServer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UwaziSendFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       // initObservers()
        initView()
    }

    override fun initView(view: View) {
        with(binding){
            toolbar.backClickListener = {nav().popBackStack()}

            nextBtn.setOnClickListener {
               if (entityInstance != null) submitEntity()
            }
        }
    }

    private fun initView() {
        arguments?.let {
            entityInstance = Gson().fromJson(it.getString(SEND_ENTITY), UwaziEntityInstance::class.java)
            Timber.d("+++++ entity instance for sending %s, %s", entityInstance?.id, entityInstance?.collectTemplate?.entityRow?.name)
        }
    }

    override fun onBackPressed(): Boolean {
        return  nav().popBackStack()
    }

    private fun submitEntity(){
        entityInstance?.collectTemplate?.let { uwaziServer?.let { it1 -> viewModel.submitEntity(server = it1, uwaziEntityRow = it.entityRow) } }
    }
}