package rs.readahead.washington.mobile.views.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.uwazi.LanguageEntity
import rs.readahead.washington.mobile.databinding.DialogUwaziServerLanguageBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.views.adapters.uwazi.LanguageSelectorAdapter

class UwaziServerLanguageDialogFragment : AppCompatDialogFragment() {
    private val languageSelectorAdapter by lazy { LanguageSelectorAdapter() }
    private lateinit var binding: DialogUwaziServerLanguageBinding
    private val viewModel: UwaziServerLanguageViewModel by viewModels()
    private var uwaziServer: UWaziUploadServer? = null
    private var language: LanguageEntity? = null
    private var isUpdate: Boolean = false

    interface UwaziServerLanguageDialogHandler {
        fun onUwaziServerLanguageDialog(server: UWaziUploadServer)
        fun onUpdateServerLanguageDialog(server: UWaziUploadServer)
        fun onDialogServerLanguageDismiss(server: UWaziUploadServer)
    }

    companion object {
        val TAG = UwaziServerLanguageDialogFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            server: UWaziUploadServer?,
            isUpdateServer: Boolean
        ): UwaziServerLanguageDialogFragment {
            val frag = UwaziServerLanguageDialogFragment()
            val args = Bundle()
            if (server == null) {
                args.putInt(TITLE_KEY, R.string.settings_servers_add_server_dialog_title)
            } else {
                args.putBoolean(IS_UPDATE_SERVER, isUpdateServer)
                args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
                args.putSerializable(ID_KEY, server.id)
                args.putSerializable(OBJECT_KEY, server)

            }
            frag.arguments = args
            return frag
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initView()
        initObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogUwaziServerLanguageBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onStart() {
        if (dialog == null) {
            return
        }
        dialog?.window?.setWindowAnimations(
            R.style.CollectDialogAnimation
        )
        super.onStart()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // the content
        val root = RelativeLayout(activity)
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (activity == null) {
            return super.onCreateDialog(savedInstanceState)
        }
        val context = context ?: return super.onCreateDialog(savedInstanceState)

        // creating the fullscreen dialog
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(root)
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(context.resources.getDrawable(R.drawable.collect_server_dialog_layout_background))
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return dialog
    }

    private fun initData() {
        requireArguments().getSerializable(OBJECT_KEY)?.apply {
            if (this is UWaziUploadServer) {
                uwaziServer = this
                viewModel.getServerLanguage(this)
            }
        }
        requireArguments().getBoolean(IS_UPDATE_SERVER).let {
            isUpdate = it
        }
    }

    private fun initView() {
        with(binding) {
            languageRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = languageSelectorAdapter
            }

           /* cancel.setOnClickListener {
                dismissDialog()
            }

            next.setOnClickListener {
                uwaziServer?.let { server ->
                    language?.let { language ->
                        viewModel.updateLanguageSettings(
                            server = server,
                            language = language
                        )
                    }
                }
            }*/
        }
    }

    private fun initObservers() {
        with(viewModel) {
            listLanguage.observe(viewLifecycleOwner, { languageList ->
                languageSelectorAdapter.setLanguages(languageList)
            })

            languageClicked.observe(viewLifecycleOwner, {
                language = it
            })

            languageUpdated.observe(viewLifecycleOwner, { updated ->
                if (updated) {
                    updateLanguage()
                }
            })
            progress.observe(viewLifecycleOwner, {
              binding.progressCircular.isVisible = it
            })
        }
    }

    private fun updateLanguage() {
        dismiss()
        val activity = activity as UwaziServerLanguageDialogHandler? ?: return
        uwaziServer?.let { server ->
            if (isUpdate) {
                activity.onUpdateServerLanguageDialog(server)
            } else {
                activity.onUwaziServerLanguageDialog(server)
            }
        }
    }


    private fun dismissDialog() {
        dismiss()
        val activity = activity as UwaziServerLanguageDialogHandler? ?: return
        uwaziServer?.let { server -> activity.onDialogServerLanguageDismiss(server) }
    }

}