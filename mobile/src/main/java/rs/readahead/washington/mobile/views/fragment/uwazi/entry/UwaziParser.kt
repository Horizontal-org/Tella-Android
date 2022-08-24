package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.content.Context
import com.google.gson.internal.LinkedTreeMap
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.uwazi.UwaziConstants
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValueAttachment

class UwaziParser(private val context: Context?) {

    private var entryPrompts = mutableListOf<UwaziEntryPrompt>()

    private val uwaziTitlePrompt by lazy {
        UwaziEntryPrompt(
            UWAZI_TITLE,
            "10242048",
            UwaziConstants.UWAZI_DATATYPE_TEXT,
            "Title",
            true,
            "Enter the submission title"
        )
    }

    private val uwaziFilesPrompt by lazy {
        UwaziEntryPrompt(
            UWAZI_SUPPORTING_FILES,
            "10242049",
            UwaziConstants.UWAZI_DATATYPE_MULTIFILES,
            context?.getString(R.string.Uwazi_MiltiFileWidget_SupportingFiles)
                ?: "Supporting files",
            false,
            context?.getString(R.string.Uwazi_MiltiFileWidget_Help) ?: "Select files"
        )
    }

    private val uwaziPdfsPrompt by lazy {
        UwaziEntryPrompt(
            UWAZI_PRIMARY_DOCUMENTS,
            "10242050",
            UwaziConstants.UWAZI_DATATYPE_MULTIPDFFILES,
            context?.getString(R.string.Uwazi_MiltiFileWidget_PrimaryDocuments)
                ?: "Primary documents",
            false,
            context?.getString(R.string.Uwazi_MiltiFileWidget_AttachMenyPdfFiles)
                ?: "Attach PDF files"
        )
    }

    fun fillAnswersToForm(instance: UwaziEntityInstance, formView: UwaziFormView) {
        val files = mutableMapOf<String, FormMediaFile>()
        for (file in instance.widgetMediaFiles) {
            files[file.name] = file
        }

        formView.setBinaryData(UWAZI_TITLE, instance.title)

        for (answer in instance.metadata) {
            if ((answer.value as List<*>).size > 1) {
                formView.setBinaryData(answer.key, answer.value)
            } else {
                val uwaziValue: UwaziValue = answer.value[0] as UwaziValue
                val stringVal = uwaziValue.value
                if (files.containsKey(stringVal)) {
                    formView.setBinaryData(answer.key, files[stringVal] as VaultFile)
                } else {
                    formView.setBinaryData(answer.key, stringVal)
                }
            }
        }
    }

    fun putAnswersToForm(instance: UwaziEntityInstance, formView: UwaziFormView) {
        val files = mutableMapOf<String, FormMediaFile>()
        for (file in instance.widgetMediaFiles) {
            files[file.name] = file
        }

        formView.setBinaryData(UWAZI_TITLE, instance.title)
        for (answer in instance.metadata) {

            val stringVal = if ((instance.metadata[answer.key] as ArrayList).size == 1) {
                (instance.metadata[answer.key]?.get(0) as LinkedTreeMap<String, Any>)["value"]
            } else {
                (instance.metadata[answer.key])
            }

            if (files.containsKey(stringVal)) {
                formView.setBinaryData(answer.key, files[stringVal] as VaultFile)
            } else {
                if (stringVal != null) {
                    formView.setBinaryData(answer.key, stringVal)
                }
            }
        }
    }

    fun getAnswersFromForm(
        isSend: Boolean,
        uwaziFormView: UwaziFormView,
        template: CollectTemplate?,
        entityInstance: UwaziEntityInstance
    ): Boolean {
        uwaziFormView.clearValidationConstraints()
        val hashmap = mutableMapOf<String, List<Any>>()
        val widgetMediaFiles = mutableListOf<FormMediaFile>()
        val answers = uwaziFormView.answers
        var validationRequired = false
        var validationError = false

        // check required fields
        if (answers[UWAZI_TITLE] == null) {
            uwaziFormView.setValidationConstraintText(
                UWAZI_TITLE,
                context?.getString(R.string.Uwazi_Entity_Error_Response_Mandatory)
                    ?: "Mandatory field"
            )
            validationRequired = true
        }

        if (isSend) {
            for (property in template?.entityRow?.properties!!) {
                //check url validation errors
                if (uwaziFormView.checkValidationConstraints()) {
                    validationError = true
                }

                //check mandatory errors
                if (property.required && (answers[property.name] == null)) {
                    uwaziFormView.setValidationConstraintText(
                        property.name,
                        context?.getString(R.string.Uwazi_Entity_Error_Response_Mandatory)
                            ?: "Mandatory field"
                    )
                    validationRequired = true
                }
            }
        }
        if (validationRequired || validationError) return false

        // put answers to entity
        for (answer in answers) {
            if (answer.value != null) {
                if (answer.key == UWAZI_TITLE) {
                    entityInstance.title = (answer.value as UwaziValue).value as String
                } else {
                    when (answer.value) {
                        is List<*> -> {
                            hashmap[answer.key] = (answer.value) as List<UwaziValue>
                        }
                        is UwaziValueAttachment -> {
                            hashmap[answer.key] = arrayListOf(
                                UwaziValueAttachment(
                                    value = (answer.value as UwaziValueAttachment).value,
                                    attachment = uwaziFormView.filesNames.indexOf((answer.value as UwaziValueAttachment).value)
                                )
                            )
                        }
                        else -> {
                            hashmap[answer.key] =
                                arrayListOf(UwaziValue((answer.value as UwaziValue).value))
                        }
                    }
                }
            }
        }

        //put files in entity
        for (answer in uwaziFormView.files) {
            if (answer != null) {
                widgetMediaFiles.add(answer)
            }
        }
        entityInstance.metadata = hashmap
        entityInstance.widgetMediaFiles = widgetMediaFiles
        entityInstance.collectTemplate = template
        entityInstance.template = template?.entityRow?.name.toString()
        return true
    }

    fun prepareFormView(
        template: CollectTemplate?
    ): UwaziFormView {
        entryPrompts.clear()

        //TODO Handle this special common props smarter
        entryPrompts.add(uwaziPdfsPrompt)
        entryPrompts.add(uwaziFilesPrompt)

        if (template?.entityRow?.commonProperties?.get(0)?.translatedLabel?.length!! > 0) {
            uwaziTitlePrompt.question =
                template.entityRow.commonProperties.get(0).translatedLabel
        }
        entryPrompts.add(uwaziTitlePrompt)

        for (property in template.entityRow.properties) {
            val entryPrompt = UwaziEntryPrompt(
                property.name,
                property.id,
                property.type,
                property.translatedLabel,
                property.required,
                property.translatedLabel
            )
            if (property.values != null) {
                entryPrompt.selectValues = property.values
            }
            entryPrompts.add(entryPrompt)
        }

        val arr: Array<UwaziEntryPrompt?> = arrayOfNulls(entryPrompts.size)
        arr.indices.forEach { i ->
            arr[i] = entryPrompts[i]
        }

        return UwaziFormView(context, arr)
    }
}