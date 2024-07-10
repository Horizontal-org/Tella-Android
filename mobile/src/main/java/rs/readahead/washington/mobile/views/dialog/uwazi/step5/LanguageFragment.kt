package rs.readahead.washington.mobile.views.dialog.uwazi.step5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.DialogUwaziServerLanguageBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.Language
import rs.readahead.washington.mobile.views.adapters.uwazi.LanguageSelectorAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.TITLE_KEY
import rs.readahead.washington.mobile.views.dialog.uwazi.UwaziConnectFlowViewModel
import rs.readahead.washington.mobile.views.dialog.uwazi.step6.SuccessConnectFragment

class LanguageFragment : BaseFragment() {
    private lateinit var binding: DialogUwaziServerLanguageBinding
    private val viewModel: UwaziConnectFlowViewModel by viewModels()
    private lateinit var serverUwazi: UWaziUploadServer
    private var language: Language? = null
    private val languageSelectorAdapter by lazy { LanguageSelectorAdapter() }
    private var isUpdate = false

    companion object {
        val TAG = LanguageFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(server: UWaziUploadServer, isUpdate: Boolean): LanguageFragment {
            val frag = LanguageFragment()
            val args = Bundle()

            args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
            args.putSerializable(ID_KEY, server.id)
            args.putString(OBJECT_KEY, Gson().toJson(server))
            args.putBoolean(IS_UPDATE_SERVER, isUpdate)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogUwaziServerLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        viewModel.getSettings(serverUwazi)
    }

    private fun initObservers() {
        with(viewModel) {
            settings.observe(viewLifecycleOwner) { result ->
                languageSelectorAdapter.setLanguages(result.second)
                serverUwazi.name = result.first
            }

            languageClicked.observe(viewLifecycleOwner) { languages->
                language = languages
                serverUwazi.localeCookie = languages.key
            }

            languageUpdated.observe(viewLifecycleOwner) { updated ->
                if (updated) {
                    baseActivity.addFragment(
                        SuccessConnectFragment.newInstance(serverUwazi, isUpdate),
                        R.id.container
                    )
                }
            }
            progress.observe(viewLifecycleOwner) {
                binding.progressCircular.isVisible = it
            }
        }
    }


    override fun initView(view: View) {
        arguments?.getString(OBJECT_KEY)?.let {
            serverUwazi = Gson().fromJson(it, UWaziUploadServer::class.java)
        }

        arguments?.getBoolean(IS_UPDATE_SERVER)?.let {
            isUpdate = it
        }
        with(binding) {
            languageRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = languageSelectorAdapter
            }
            nextBtn.setOnClickListener {
                baseActivity.addFragment(
                    SuccessConnectFragment.newInstance(serverUwazi, isUpdate),
                    R.id.container
                )
            }
            backBtn.setOnClickListener {
                baseActivity.supportFragmentManager.popBackStack()
            }
        }

    }

}