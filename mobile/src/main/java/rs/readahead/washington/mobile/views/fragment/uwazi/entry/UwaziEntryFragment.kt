package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.uwazi.UwaziConstants
import rs.readahead.washington.mobile.databinding.UwaziEntryFragmentBinding
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.views.activity.QuestionAttachmentActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener


const val COLLECT_TEMPLATE = "collect_template"
const val UWAZI_TITLE = "title"

class UwaziEntryFragment : BaseFragment(), OnNavBckListener {
    private val viewModel: SharedUwaziSubmissionViewModel by lazy {
        ViewModelProvider(activity).get(SharedUwaziSubmissionViewModel::class.java)
    }
    private lateinit var binding: UwaziEntryFragmentBinding
    private var template: CollectTemplate? = null
    private var entityInstance: UwaziEntityInstance = UwaziEntityInstance()
    private val bundle by lazy { Bundle() }
    private var screenView: ViewGroup? = null
    private var entryPrompts: ArrayList<UwaziEntryPrompt> = ArrayList()
    private lateinit var uwaziFormView: UwaziFormView

    private val uwaziTitlePrompt = UwaziEntryPrompt(
        UWAZI_TITLE,
        "10242048",
        UwaziConstants.UWAZI_DATATYPE_TEXT,
        "Title",
        true,
        "Enter the submission title"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UwaziEntryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == C.MEDIA_FILE_ID && resultCode == Activity.RESULT_OK) {
            val vaultFile =
                data!!.getSerializableExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY) as VaultFile
            putVaultFileInForm(vaultFile)
        }
    }

    override fun initView(view: View) {
        with(binding) {
            toolbar.backClickListener = { nav().popBackStack() }
            toolbar.onRightClickListener = {
                entityInstance.status = UwaziEntityStatus.DRAFT
                entityInstance.let { viewModel.saveEntityInstance(it) }
            }

            nextBtn.setOnClickListener { sendEntity() }

            screenView = binding.screenFormView
        }
    }

    private fun initView() {
        arguments?.let {
            template = Gson().fromJson(it.getString(COLLECT_TEMPLATE), CollectTemplate::class.java)
            entityInstance.collectTemplate = template
            entityInstance.title = template?.entityRow?.name.toString()
            entityInstance.template = template?.entityRow?.name.toString()
            parseUwaziForm()
        }
    }

    private fun initObservers() {
        with(viewModel) {

            progress.observe(viewLifecycleOwner, { status ->
                if (status == UwaziEntityStatus.SUBMITTED) {
                    nav().popBackStack()
                    progress.postValue(UwaziEntityStatus.UNKNOWN)
                }
            })

            instance.observe(viewLifecycleOwner, {
                entityInstance = it
                showSavedDialog()
            })
        }
    }

    override fun onBackPressed(): Boolean {
        return nav().popBackStack()
    }


    private fun sendEntity() {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER

        val hashmap = mutableMapOf<String, List<Any>>()
        val widgetMediaFiles = mutableListOf<FormMediaFile>()
        for (answer in uwaziFormView.answers) {
            if (answer.value != null) {
                if (answer.key == "title") {
                    entityInstance.title = answer.value.displayText
                } else {
                    hashmap[answer.key] = arrayListOf(UwaziValue(answer.value.displayText))

                }
            }
        }
        for (answer in uwaziFormView.files) {
            if (answer != null) {
                widgetMediaFiles.add(answer)
            }
        }

        entityInstance.metadata = hashmap
        entityInstance.widgetMediaFiles = widgetMediaFiles
        entityInstance.status = UwaziEntityStatus.FINALIZED
        entityInstance.collectTemplate = template
        entityInstance.template = template?.entityRow?.name.toString()

        val gsonTemplate = Gson().toJson(entityInstance)
        bundle.putString(SEND_ENTITY, gsonTemplate)
        NavHostFragment.findNavController(this@UwaziEntryFragment)
            .navigate(R.id.action_uwaziEntryScreen_to_uwaziSendScreen, bundle)
    }

    private fun parseUwaziForm() {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER
        entryPrompts.clear()

        entryPrompts.add(uwaziTitlePrompt)
        for (property in template?.entityRow?.properties!!) {
            val entryPrompt = UwaziEntryPrompt(
                property.name,
                property.id,
                property.type,
                property.label,
                property.required,
                property.label
            )
            entryPrompts.add(entryPrompt);
        }

        val arr: Array<UwaziEntryPrompt?> = arrayOfNulls(entryPrompts.size)
        arr.indices.forEach { i ->
            arr[i] = entryPrompts[i]
        }
        uwaziFormView = UwaziFormView(requireContext(), arr)
        screenView?.addView(uwaziFormView)
    }

    private fun putVaultFileInForm(vaultFile: VaultFile?) {
        val filename = vaultFile?.let { uwaziFormView.setBinaryData(it) }
    }

    private fun showSavedDialog() {
        DialogUtils.showBottomMessage(
            activity,
            getString(R.string.Uwazi_EntryInstance_SavedInfo),
            false
        )
    }
}