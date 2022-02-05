package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.UwaziEntryFragmentBinding
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.views.activity.QuestionAttachmentActivity
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener


const val COLLECT_TEMPLATE = "collect_template"

class UwaziEntryFragment : BaseFragment(), OnNavBckListener {
    private val viewModel: UwaziEntryViewModel by viewModels()
    private lateinit var binding: UwaziEntryFragmentBinding
    private var metadata = JsonObject()
    private var template: CollectTemplate? = null
    private var entityInstance: UwaziEntityInstance = UwaziEntityInstance()
    private val bundle by lazy { Bundle() }
    private var screenView: ViewGroup? = null
    private var entryPrompts: ArrayList<UwaziEntryPrompt> = ArrayList()
    private lateinit var uwaziFormView: UwaziFormView

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
            template.observe(viewLifecycleOwner, {
                it
            })

            progress.observe(viewLifecycleOwner, {
                binding.progressCircular.isVisible = it
            })

            instance.observe(viewLifecycleOwner, {
                entityInstance = it
                setUpdated(it.updated)
            })
        }
    }

    override fun onBackPressed(): Boolean {
        return nav().popBackStack()
    }

    private fun setUpdated(updatedTime: Long) {
        // binding.updated.text = updatedTime.toString()
    }

    private fun sendEntity() {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER
        val widgetMediaFiles =  mutableListOf<FormMediaFile>()

        for (answer in uwaziFormView.answers) {
            if (answer.value != null) {
                metadata.addProperty(
                    answer.key,
                    Gson().toJson(listOf(UwaziValue(answer.value.displayText)))
                )
            }
        }
        for (answer in uwaziFormView.files) {
            if (answer != null) {
                widgetMediaFiles.add(answer)
            }
        }

        entityInstance.metadata = metadata
        entityInstance.widgetMediaFiles = widgetMediaFiles
        entityInstance.status = UwaziEntityStatus.FINALIZED
        entityInstance.collectTemplate = template

        val gsonTemplate = Gson().toJson(entityInstance)
        bundle.putString(SEND_ENTITY, gsonTemplate)
        NavHostFragment.findNavController(this@UwaziEntryFragment)
            .navigate(R.id.action_uwaziEntryScreen_to_uwaziSendScreen, bundle)
    }

    private fun parseUwaziForm() {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER

        metadata = JsonObject()

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
        entityInstance.metadata = metadata

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
}