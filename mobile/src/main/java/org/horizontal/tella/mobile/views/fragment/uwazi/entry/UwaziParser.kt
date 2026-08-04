package org.horizontal.tella.mobile.views.fragment.uwazi.entry

import android.content.Context
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.uwazi.UwaziConstants
import org.horizontal.tella.mobile.data.uwazi.UwaziConstants.UWAZI_DATATYPE_RELATIONSHIP
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.presentation.uwazi.UwaziRelationShipEntity
import org.horizontal.tella.mobile.presentation.uwazi.UwaziValue
import org.horizontal.tella.mobile.presentation.uwazi.UwaziValueAttachment

class UwaziParser(private val context: Context?) {

    private var template: UwaziTemplate? = null
    private var entityInstance: UwaziEntityInstance = UwaziEntityInstance()
    private var entryPrompts = mutableListOf<UwaziEntryPrompt>()
    var hashCode: Int? = null //used to check is the answers has changed

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

    fun getInstance(): UwaziEntityInstance {
        return entityInstance
    }

    fun getTemplate(): UwaziTemplate? {
        return template
    }

    fun setTemplate(collectTemplate: UwaziTemplate) {
        template = collectTemplate
    }

    fun parseInstance(instance: String): UwaziFormView {
        entityInstance = Gson().fromJson(instance, UwaziEntityInstance::class.java)
        template = entityInstance.collectTemplate
        return prepareFormView()
    }

    fun parseTemplateForRelationShipEntities(templateString: String) {
        template = Gson().fromJson(templateString, UwaziTemplate::class.java)
    }

    fun parseTemplate(templateString: String): UwaziFormView {
        template = Gson().fromJson(templateString, UwaziTemplate::class.java)
        entityInstance.collectTemplate = template
        entityInstance.template = template?.entityRow?.name.toString()
        return prepareFormView()
    }

    fun fillAnswersToForm(formView: UwaziFormView) {
        putAnswersToForm(formView)
    }

    fun putAnswersToForm(formView: UwaziFormView) {
        val filesByName = entityInstance.widgetMediaFiles.associateBy { it.name }

        formView.setBinaryData(UWAZI_TITLE, entityInstance.title)

        for ((key, values) in entityInstance.metadata) {
            restoreAnswerToForm(formView, key, values, filesByName)
        }

        hashCode = formView.answers.hashCode()
    }

    /**
     * Restores a saved metadata answer onto the matching widget.
     * After Gson round-trips, attachment answers become LinkedTreeMaps with both
     * "value" (filename) and "attachment" (index) — those must be unwrapped so
     * image/media widgets receive a FormMediaFile (or a plain value), not the raw list.
     */
    private fun restoreAnswerToForm(
        formView: UwaziFormView,
        key: String,
        values: List<Any>,
        filesByName: Map<String, FormMediaFile>
    ) {
        if (values.isEmpty()) {
            formView.setBinaryData(key, "")
            return
        }

        try {
            if (values.size > 1) {
                formView.setBinaryData(key, values)
                return
            }

            val extracted = extractAnswerValue(values[0]) ?: return

            when {
                extracted is String && filesByName.containsKey(extracted) -> {
                    formView.setBinaryData(key, filesByName.getValue(extracted))
                }
                extracted is Array<*> -> {
                    val ids = extracted.filterIsInstance<String>()
                    if (ids.isNotEmpty()) {
                        formView.setBinaryData(key, ArrayList(ids))
                    }
                }
                extracted is List<*> -> {
                    val ids = extracted.filterIsInstance<String>()
                    if (ids.isNotEmpty()) {
                        formView.setBinaryData(key, ArrayList(ids))
                    } else {
                        formView.setBinaryData(key, extracted)
                    }
                }
                else -> formView.setBinaryData(key, extracted)
            }
        } catch (e: Exception) {
            // Keep restoring other answers if one widget fails.
            android.util.Log.e("UwaziParser", "Failed to restore answer for $key", e)
        }
    }

    private fun extractAnswerValue(raw: Any?): Any? {
        return when (raw) {
            is UwaziValueAttachment -> raw.value
            is UwaziValue -> raw.value
            is LinkedTreeMap<*, *> -> raw["value"] ?: raw
            is Map<*, *> -> raw["value"] ?: raw
            else -> raw
        }
    }

    fun getAnswersFromForm(
        isSend: Boolean,
        uwaziFormView: UwaziFormView
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
                            hashmap[answer.key] =  (answer.value as List<UwaziValue>).toCollection(ArrayList())
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
        template?.entityRow?.properties?.filter { property -> property.type == UWAZI_DATATYPE_RELATIONSHIP }
            ?.forEach { property ->
                val selectedEntities: ArrayList<Any> = ArrayList()
                uwaziFormView.entities.forEach { selectedEntity ->
                    property.entities?.filter { it.id == selectedEntity.value && property.label == selectedEntity.type}?.forEach {
                        selectedEntities.add(UwaziRelationShipEntity(it.id, it.label,selectedEntity.type))
                    }
                }
                property.selectedEntities = selectedEntities as List<UwaziRelationShipEntity>
                answers.filter { answer -> answer.key == property.name }.forEach { answer ->
                    hashmap[answer.key] = selectedEntities as List<UwaziRelationShipEntity>
                }
            }
        val entities = ArrayList<UwaziRelationShipEntity>()
        uwaziFormView.entities.filterNotNull().let {
            entities.addAll(it)
        }
        entityInstance.metadata = hashmap
        entityInstance.widgetMediaFiles = widgetMediaFiles
        entityInstance.collectTemplate = template
        entityInstance.template = template?.entityRow?.name.toString()
        return true
    }

    private fun prepareFormView(): UwaziFormView {
        entryPrompts.clear()

        //TODO Handle this special common props smarter
        entryPrompts.add(uwaziPdfsPrompt)
        entryPrompts.add(uwaziFilesPrompt)
        if (template?.entityRow?.commonProperties?.get(0)?.translatedLabel?.length!! > 0) {
            uwaziTitlePrompt.question =
                template!!.entityRow.commonProperties.get(0).translatedLabel
        }
        entryPrompts.add(uwaziTitlePrompt)

        for (property in template!!.entityRow.properties) {
            val entryPrompt = UwaziEntryPrompt(
                property.name,
                property.selectedEntities,
                property._id,
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
    fun getGsonTemplate(): String {
        return Gson().toJson(entityInstance)
    }
    fun getToGsonTemplate(): String {
        return Gson().toJson(template)
    }
    fun setInstanceStatus(entityStatus: EntityStatus) {
        entityInstance.status = entityStatus
    }
}