package org.horizontal.tella.mobile.views.fragment.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.database.UwaziDataSource
import org.horizontal.tella.mobile.data.repository.MediaFileRequestBody
import org.horizontal.tella.mobile.data.repository.ProgressListener
import org.horizontal.tella.mobile.data.repository.UwaziRepository
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus
import org.horizontal.tella.mobile.domain.entity.uwazi.EntityInstanceBundle
import org.horizontal.tella.mobile.domain.entity.uwazi.ListTemplateResult
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.domain.exception.NoConnectivityException
import org.horizontal.tella.mobile.presentation.uwazi.SendEntityRequest
import org.horizontal.tella.mobile.util.crash.CrashReporterProvider
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseEntityListViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportCounts
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import org.horizontal.tella.mobile.views.fragment.uwazi.mappers.toViewEntityInstanceItem
import org.horizontal.tella.mobile.views.fragment.uwazi.mappers.toViewUwaziTemplateItem
import timber.log.Timber
import java.net.URLEncoder
import javax.inject.Inject

private const val MULTIPART_FORM_DATA = "text/plain"

/**
 * Single ViewModel for the Uwazi connection, mirroring the per-connection ViewModels of Google
 * Drive, Nextcloud and Tella Web. On top of the shared draft/outbox/submitted behaviour it owns
 * the downloaded-template list and the multipart entity submission Uwazi requires.
 */
@HiltViewModel
class UwaziViewModel @Inject constructor() : BaseEntityListViewModel<UwaziEntityInstance>() {

    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val repository = UwaziRepository()

    private val _templates = MutableLiveData<List<Any>>()
    val templates: LiveData<List<Any>> get() = _templates

    private val _showSheetMore = SingleLiveEvent<UwaziTemplate>()
    val showSheetMore: LiveData<UwaziTemplate> get() = _showSheetMore

    private val _openEntity = SingleLiveEvent<UwaziTemplate>()
    val openEntity: LiveData<UwaziTemplate> get() = _openEntity

    private val _templateRemoved = SingleLiveEvent<String>()
    val templateRemoved: LiveData<String> get() = _templateRemoved

    /** Status of the last save from the entry screen, used to pick the tab to return to. */
    val saveStatus = MutableLiveData<EntityStatus>()

    private val _instanceProgress = MutableLiveData<UwaziEntityInstance>()
    val instanceProgress: LiveData<UwaziEntityInstance> get() = _instanceProgress

    private val _progressCallBack = SingleLiveEvent<Pair<String, Float>>()
    val progressCallBack: LiveData<Pair<String, Float>> get() = _progressCallBack

    private val _refreshedTemplates = MutableLiveData<ListTemplateResult>()
    val refreshedTemplates: LiveData<ListTemplateResult> get() = _refreshedTemplates

    private val _refreshProgress = MutableLiveData<Boolean>()
    val refreshProgress: LiveData<Boolean> get() = _refreshProgress

    private val _connectionAvailable = MutableLiveData<Boolean>()
    val connectionAvailable: LiveData<Boolean> get() = _connectionAvailable

    /** Entity currently being uploaded, so [clearDisposable] can park it in the outbox. */
    private var inFlightEntity: UwaziEntityInstance? = null

    // region Templates

    fun listTemplates() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listBlankTemplates().toObservable()
            }
            .doFinally { _progress.postValue(false) }
            .subscribe({ templates: List<UwaziTemplate> ->
                val resultList = mutableListOf<Any>()
                resultList.add(0, R.string.Uwazi_Templates_HeaderMessage)
                templates.map { template ->
                    resultList.add(template.toViewUwaziTemplateItem(
                        onMoreClicked = { _showSheetMore.postValue(template) },
                        onFavoriteClicked = { toggleFavorite(template) },
                        onOpenEntityClicked = { _openEntity.postValue(template) }
                    ))
                }
                _templates.postValue(resultList)
            }) { throwable -> handleError(throwable) }
        )
    }

    private fun toggleFavorite(template: UwaziTemplate) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle { dataSource: UwaziDataSource -> dataSource.toggleFavorite(template) }
            .subscribe({ listTemplates() }) { throwable ->
                CrashReporterProvider.get().recordException(throwable)
                handleError(throwable)
            }
        )
    }

    fun confirmDelete(template: UwaziTemplate) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMapCompletable { dataSource: UwaziDataSource ->
                dataSource.deleteTemplate(template.id)
            }
            .doFinally { _progress.postValue(false) }
            .subscribe({
                _templateRemoved.postValue(template.entityRow.name)
                listTemplates()
            }) { throwable ->
                CrashReporterProvider.get().recordException(throwable)
                handleError(throwable)
            }
        )
    }

    fun refreshEntitiesList() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _refreshProgress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listUwaziServers().toObservable()
            }
            .flatMap { servers ->
                val singles = servers.map { server -> repository.getTemplatesResult(server) }
                Single.zip(singles) { results: Array<Any?> ->
                    val allResults = ListTemplateResult()
                    results.filterIsInstance<ListTemplateResult>().forEach { result ->
                        allResults.templates.addAll(result.templates)
                        allResults.errors.addAll(result.errors)
                    }
                    allResults
                }.toObservable()
            }
            .flatMap { result ->
                keyDataSource.uwaziDataSource.flatMap { dataSource ->
                    dataSource.updateBlankTemplatesIfNeeded(result).toObservable()
                }
            }
            .doFinally { _refreshProgress.postValue(false) }
            .subscribe({ result -> _refreshedTemplates.postValue(result) }) { throwable ->
                if (throwable is NoConnectivityException) {
                    _connectionAvailable.postValue(true)
                } else {
                    CrashReporterProvider.get().recordException(throwable)
                    handleError(throwable)
                }
            }
        )
    }

    // endregion

    // region Draft / outbox / submitted lists

    override fun listDrafts() {
        listInstances({ it.listDraftInstances() }, _draftListReportFormInstance)
    }

    override fun listOutbox() {
        listInstances({ it.listOutboxInstances() }, _outboxReportListFormInstance)
    }

    override fun listSubmitted() {
        listInstances({ it.listSubmittedInstances() }, _submittedReportListFormInstance)
    }

    private fun listInstances(
        query: (UwaziDataSource) -> Single<List<UwaziEntityInstance>>,
        target: SingleLiveEvent<List<ViewEntityTemplateItem>>
    ) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource -> query(dataSource).toObservable() }
            .doFinally { _progress.postValue(false) }
            .subscribe({ instances ->
                target.postValue(instances.map { instance ->
                    instance.toViewEntityInstanceItem(
                        onMoreClicked = { onMoreClicked(instance) },
                        onOpenClicked = { openInstance(instance) }
                    )
                })
            }) { throwable -> handleError(throwable) }
        )
    }

    override fun listDraftsOutboxAndSubmitted() {
        disposables.add(keyDataSource.uwaziDataSource
            .firstOrError()
            .flatMap { dataSource ->
                Single.zip(
                    dataSource.listDraftInstances(),
                    dataSource.listOutboxInstances(),
                    dataSource.listSubmittedInstances()
                ) { drafts, outbox, submitted ->
                    ReportCounts(outbox.size, submitted.size, drafts.size)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ counts -> _reportCounts.postValue(counts) }) { throwable ->
                handleError(throwable)
            }
        )
    }

    override fun deleteReport(instance: UwaziEntityInstance) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMapCompletable { dataSource: UwaziDataSource ->
                dataSource.deleteEntityInstance(instance.id)
            }
            .doFinally { _progress.postValue(false) }
            .subscribe({ _instanceDeleted.postValue(instance.title) }) { throwable ->
                CrashReporterProvider.get().recordException(throwable)
                handleError(throwable)
            }
        )
    }

    /**
     * Loads the entity together with its vault attachments and publishes it, which is what drives
     * the shared list fragments' navigation to the entry or send screen.
     */
    override fun getReportBundle(instance: UwaziEntityInstance) {
        disposables.add(
            keyDataSource.uwaziDataSource
                .firstOrError()
                .flatMap { dataSource: UwaziDataSource -> dataSource.getBundle(instance.id) }
                .flatMap { bundle: EntityInstanceBundle ->
                    MyApplication.keyRxVault.rxVault
                        .firstOrError()
                        .flatMap { rxVault ->
                            rxVault.get(bundle.fileIds).map { vaultFiles ->
                                val existing = vaultFiles.filterNotNull().associateBy { it.id }
                                bundle.instance.apply {
                                    widgetMediaFiles = bundle.fileIds.mapNotNull { fileId ->
                                        existing[fileId]?.let { file ->
                                            FormMediaFile.fromMediaFile(file).apply {
                                                status = FormMediaFileStatus.NOT_SUBMITTED
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ loaded -> _reportInstance.postValue(loaded) }) { throwable ->
                    openInstanceAfterUnexpectedFailure(
                        instance,
                        throwable,
                        "Failed to get Uwazi entity instance ${instance.id}"
                    )
                }
        )
    }

    override fun listServers() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource -> dataSource.listUwaziServers().toObservable() }
            .doFinally { _progress.postValue(false) }
            .subscribe({ servers -> _serversList.postValue(servers) }) { throwable ->
                handleError(throwable)
            }
        )
    }

    // endregion

    // region Saving

    override fun saveDraft(reportInstance: UwaziEntityInstance, exitAfterSave: Boolean) {
        reportInstance.status = EntityStatus.DRAFT
        saveEntityInstance(reportInstance)
        _exitAfterSave.postValue(exitAfterSave)
    }

    override fun saveOutbox(reportInstance: UwaziEntityInstance) {
        reportInstance.status = EntityStatus.SUBMISSION_PENDING
        saveEntityInstance(reportInstance)
    }

    override fun saveSubmitted(reportInstance: UwaziEntityInstance) {
        reportInstance.status = EntityStatus.SUBMITTED
        saveEntityInstance(reportInstance)
    }

    fun saveEntityInstance(instance: UwaziEntityInstance) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.saveEntityInstance(instance).toObservable()
            }
            .doFinally { saveStatus.postValue(instance.status) }
            .subscribe({ saveStatus.postValue(instance.status) }) { throwable ->
                CrashReporterProvider.get().recordException(throwable)
                handleError(throwable)
            })
    }

    // endregion

    // region Submission

    override fun submitReport(instance: UwaziEntityInstance, backButtonPressed: Boolean) {
        if (instance.status == EntityStatus.SUBMITTED) return

        // Leaving the send screen before the upload finishes parks the entity in the outbox,
        // which is what the shared send screen's back handling expects.
        if (backButtonPressed) {
            instance.status = EntityStatus.SUBMISSION_PENDING
            saveEntityInstance(instance)
            return
        }

        val serverId = instance.collectTemplate?.serverId ?: return

        // Kept in memory only: Uwazi's outbox query does not select SUBMISSION_IN_PROGRESS, so
        // persisting it here would hide the entity from every tab. It exists so the shared send
        // screen can offer Pause while the upload runs.
        instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
        inFlightEntity = instance
        _instanceProgress.postValue(instance)

        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.getUwaziServerById(serverId).toObservable()
            }
            .subscribe({ server ->
                if (server.password.isNullOrEmpty() || server.username.isNullOrEmpty()) {
                    submitWhiteListedEntity(server, instance)
                } else {
                    submitEntity(server, instance)
                }
            }) { throwable ->
                CrashReporterProvider.get().recordException(throwable)
                handleError(throwable)
            }
        )
    }

    private fun submitEntity(server: UWaziUploadServer, entity: UwaziEntityInstance) {
        disposables.add(
            repository.submitEntity(
                server = server,
                entity = createRequestBody(Gson().toJson(entity.createEntityRequest())),
                attachments = createParts(
                    "attachments",
                    removeDocumentsList(entity),
                    _progressCallBack
                ),
                attachmentsOriginalName = originalNames(removeDocumentsList(entity)),
                documents = createParts("documents", getDocumentsList(entity), _progressCallBack)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { markSubmissionFailed(entity) }
                .flatMap {
                    entity.status = EntityStatus.SUBMITTED
                    entity.formPartStatus = FormMediaFileStatus.SUBMITTED
                    keyDataSource.uwaziDataSource.blockingFirst().saveEntityInstance(entity)
                }
                .subscribe({ markSubmissionSucceeded(entity) }) { throwable ->
                    CrashReporterProvider.get().recordException(throwable)
                    handleError(throwable)
                    markSubmissionFailed(entity)
                })
    }

    private fun submitWhiteListedEntity(
        server: UWaziUploadServer,
        entity: UwaziEntityInstance
    ) {
        disposables.add(
            repository.submitWhiteListedEntity(
                server = server,
                entity = createRequestBody(Gson().toJson(entity.createEntityRequest())),
                attachments = createParts(
                    "attachments",
                    removeDocumentsList(entity),
                    _progressCallBack
                ),
                attachmentsOriginalName = originalNames(removeDocumentsList(entity)),
                documents = createParts("documents", getDocumentsList(entity), _progressCallBack)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { response ->
                    if (response.isSuccessful) {
                        entity.status = EntityStatus.SUBMITTED
                        entity.formPartStatus = FormMediaFileStatus.SUBMITTED
                        keyDataSource.uwaziDataSource.blockingFirst().saveEntityInstance(entity)
                    } else {
                        Single.error(Throwable("Server error: ${response.code()}"))
                    }
                }
                .subscribe({ markSubmissionSucceeded(entity) }) { throwable ->
                    CrashReporterProvider.get().recordException(throwable)
                    handleError(throwable)
                    markSubmissionFailed(entity)
                }
        )
    }

    private fun markSubmissionSucceeded(entity: UwaziEntityInstance) {
        inFlightEntity = null
        entity.status = EntityStatus.SUBMITTED
        _instanceProgress.postValue(entity)
        _reportInstance.postValue(entity)
    }

    private fun markSubmissionFailed(entity: UwaziEntityInstance) {
        inFlightEntity = null
        entity.status = EntityStatus.SUBMISSION_ERROR
        saveEntityInstance(entity)
        _instanceProgress.postValue(entity)
    }

    private fun UwaziEntityInstance.createEntityRequest() = SendEntityRequest(
        metadata = removeAttachments(metadata.toMutableMap()),
        template = collectTemplate?.entityRow?._id ?: "",
        title = title,
        type = type
    )

    private fun removeAttachments(metadata: MutableMap<String, List<Any>>): Map<String, List<Any>> {
        metadata.remove("supporting_files")
        metadata.remove("primary_documents")
        return metadata
    }

    private fun createRequestBody(s: String): RequestBody {
        return RequestBody.create(MULTIPART_FORM_DATA.toMediaTypeOrNull(), s)
    }

    private fun getDocumentsList(instance: UwaziEntityInstance): List<VaultFile?> {
        val primaryDocumentsNode = instance.metadata["primary_documents"] ?: return emptyList()
        val newAttachments = mutableListOf<VaultFile>()
        ((primaryDocumentsNode[0] as LinkedTreeMap<*, *>)["value"] as ArrayList<String>).forEach { fileId ->
            instance.widgetMediaFiles.forEach { vaultFile ->
                if (vaultFile.id?.equals(fileId) == true) {
                    newAttachments.add(vaultFile)
                }
            }
        }
        return newAttachments
    }

    private fun removeDocumentsList(instance: UwaziEntityInstance): List<VaultFile?> {
        val primaryDocumentsNode =
            instance.metadata["primary_documents"] ?: return instance.widgetMediaFiles
        val newAttachments = arrayListOf<VaultFile>()
        newAttachments.addAll(instance.widgetMediaFiles)
        ((primaryDocumentsNode[0] as LinkedTreeMap<*, *>)["value"] as ArrayList<String>).forEach { fileName ->
            instance.widgetMediaFiles.forEach { vaultFile ->
                if (vaultFile.id?.equals(fileName) == true) {
                    newAttachments.remove(vaultFile)
                }
            }
        }
        return newAttachments
    }

    private fun createParts(
        formField: String,
        attachments: List<VaultFile?>?,
        progressCallBack: SingleLiveEvent<Pair<String, Float>>,
    ): List<MultipartBody.Part?> {
        val parts: MutableList<MultipartBody.Part?> = mutableListOf()
        try {
            attachments?.forEachIndexed { index, vaultFile ->
                vaultFile?.let { file ->
                    val requestBody =
                        MediaFileRequestBody(file, ProgressListener(file.id, progressCallBack))
                    parts.add(
                        MultipartBody.Part.createFormData(
                            "$formField[$index]",
                            URLEncoder.encode(file.name, "utf-8"),
                            requestBody
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.d(e.message ?: "Error attaching files")
        }
        return parts.toList()
    }

    private fun originalNames(attachments: List<VaultFile?>?): List<String> =
        attachments?.mapNotNull { it?.name } ?: emptyList()

    // endregion

    /**
     * Also serves as the send screen's Pause action, so an interrupted upload is parked in the
     * outbox the way Uwazi's former dedicated cancel button did.
     */
    override fun clearDisposable() {
        disposables.clear()
        inFlightEntity?.takeIf { it.status == EntityStatus.SUBMISSION_IN_PROGRESS }?.let { entity ->
            entity.status = EntityStatus.SUBMISSION_PENDING
            saveEntityInstance(entity)
            _instanceProgress.postValue(entity)
        }
        inFlightEntity = null
    }
}
