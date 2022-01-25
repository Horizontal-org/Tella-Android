package rs.readahead.washington.mobile.views.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.uwazi.Language
import rs.readahead.washington.mobile.databinding.DialogUwaziServerLanguageBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.views.adapters.uwazi.LanguageSelectorAdapter

class UwaziServerLanguageDialogFragment : AppCompatDialogFragment() {
    private val languageSelectorAdapter by lazy { LanguageSelectorAdapter() }
    private lateinit var binding : DialogUwaziServerLanguageBinding
    private val viewModel : UwaziServerLanguageViewModel by viewModels()
    private var uwaziServer : UWaziUploadServer? = null
    private var language : Language? = null

    companion object{
        val TAG = UwaziServerLanguageDialogFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(server: UWaziUploadServer?): UwaziServerLanguageDialogFragment {
            val frag = UwaziServerLanguageDialogFragment()
            val args = Bundle()
            if (server == null) {
                args.putInt(TITLE_KEY, R.string.settings_servers_add_server_dialog_title)
            } else {
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

    private fun initData(){
        requireArguments().getSerializable(OBJECT_KEY)?.apply {
            if (this is UWaziUploadServer){
                uwaziServer = this
                viewModel.getServerLanguage(this)
            }
        }
    }

    private fun initView(){
        with(binding){
            languageRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = languageSelectorAdapter
            }

            cancel.setOnClickListener { dismiss() }

            next.setOnClickListener { uwaziServer?.let { it1 -> language?.let { it2 -> viewModel.updateLanguageSettings(server = it1, language = it2) } } }
        }
    }

  private fun initObservers(){
        with(viewModel){
            listLanguage.observe(viewLifecycleOwner,{ languageList->
                languageSelectorAdapter.setLanguages(languageList)
            })

            languageClicked.observe(viewLifecycleOwner,{
                language = it
            })

            languageUpdated.observe(viewLifecycleOwner,{ updated ->
                if (updated){
                    dismiss()
                }
            })
        }
    }




}