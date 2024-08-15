package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.UwaziDataSource
import rs.readahead.washington.mobile.data.repository.MediaFileRequestBody
import rs.readahead.washington.mobile.data.repository.ProgressListener
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException
import rs.readahead.washington.mobile.presentation.uwazi.SendEntityRequest
import timber.log.Timber
import java.net.URLEncoder

private const val MULTIPART_FORM_DATA = "text/plain"

class SharedUwaziSubmissionViewModel : ViewModel() {
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    private val _instance = SingleLiveEvent<UwaziEntityInstance>()
    val instance: LiveData<UwaziEntityInstance> get() = _instance
    private var _templates = MutableLiveData<ListTemplateResult>()
    val templates: MutableLiveData<ListTemplateResult> get() = _templates
    private val repository = UwaziRepository()
    val progress = MutableLiveData<EntityStatus>()
    private val _server = MutableLiveData<UWaziUploadServer>()
    val server: LiveData<UWaziUploadServer> get() = _server
    var error = MutableLiveData<Throwable>()
    private val _attachments = MutableLiveData<List<FormMediaFile>>()
    val attachments: LiveData<List<FormMediaFile>> get() = _attachments

    //TODO THIS IS UGLY WILL REPLACE IT FLOWABLE RX LATER
    private val _progressCallBack = SingleLiveEvent<Pair<String, Float>>()
    val progressCallBack: LiveData<Pair<String, Float>> get() = _progressCallBack

    private val _progress = MutableLiveData<Boolean>()
    val progressRefresh: LiveData<Boolean> get() = _progress
    private var _connectionAvailable = MutableLiveData<Boolean>()
    fun saveEntityInstance(instance: UwaziEntityInstance) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.saveEntityInstance(instance).toObservable()
            }
            .doFinally { progress.postValue(instance.status) }
            .subscribe({
                progress.postValue(instance.status)
                // _instance.postValue(savedInstance)
            }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
                error.postValue(throwable)
            })
    }

    fun getUwaziServerAndSaveEntity(serverID: Long, entity: UwaziEntityInstance) {
        keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.getUwaziServerById(serverID).toObservable()
            }
            ?.subscribe(
                { server: UWaziUploadServer? ->
                    if (server != null) {
                        if (server.password.isNullOrEmpty() or server.username.isNullOrEmpty()) {
                            submitWhiteListedEntity(server, entity)

                        } else {
                            submitEntity(server, entity)
                        }

                    }
                },
                { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    error.postValue(throwable)
                }
            )?.let {
                disposables.add(
                    it
                )
            }
    }

    private fun submitEntity(server: UWaziUploadServer, entity: UwaziEntityInstance) {
        disposables.add(
            repository.submitEntity(
                server = server,
                entity = createRequestBody(Gson().toJson(entity.createEntityRequest())),
                attachments = createListOfAttachments(
                    removeDocumentsList(entity),
                    _progressCallBack
                ),
                attachmentsOriginalName = createListOfAttachmentsOriginalName(
                    removeDocumentsList(
                        entity
                    )
                ),
                documents = createListOfDocuments(getDocumentsList(entity), _progressCallBack)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { progress.postValue(EntityStatus.SUBMISSION_ERROR) }
                .flatMap {
                    entity.status = EntityStatus.SUBMITTED
                    entity.formPartStatus = FormMediaFileStatus.SUBMITTED
                    keyDataSource.uwaziDataSource.blockingFirst().saveEntityInstance(entity)
                }
                .subscribe({
                    progress.postValue(EntityStatus.SUBMITTED)
                }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(
                        throwable
                            ?: throw NullPointerException("Expression 'throwable' must not be null")
                    )
                    error.postValue(throwable)
                    progress.postValue(EntityStatus.SUBMISSION_ERROR)
                })
    }

    private fun submitWhiteListedEntity(server: UWaziUploadServer, entity: UwaziEntityInstance) {
        disposables.add(
            repository.submitWhiteListedEntity(
                server = server,
                entity = createRequestBody(Gson().toJson(entity.createEntityRequest())),
                attachments = createListOfAttachments(
                    removeDocumentsList(entity),
                    _progressCallBack
                ),
                attachmentsOriginalName = createListOfAttachmentsOriginalName(
                    removeDocumentsList(
                        entity
                    )
                ),
                documents = createListOfDocuments(getDocumentsList(entity), _progressCallBack)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { progress.postValue(EntityStatus.SUBMISSION_ERROR) }
                .flatMap {
                    entity.status = EntityStatus.SUBMITTED
                    entity.formPartStatus = FormMediaFileStatus.SUBMITTED
                    keyDataSource.uwaziDataSource.blockingFirst().saveEntityInstance(entity)
                }
                .subscribe({
                    progress.postValue(EntityStatus.SUBMITTED)
                }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(
                        throwable
                            ?: throw NullPointerException("Expression 'throwable' must not be null")
                    )
                    error.postValue(throwable)
                    progress.postValue(EntityStatus.SUBMISSION_ERROR)
                })
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
        return RequestBody.create(
            MULTIPART_FORM_DATA.toMediaTypeOrNull(), s
        )
    }

    private fun getDocumentsList(uwaziEntityInstance: UwaziEntityInstance): List<VaultFile?> {
        val primaryDocumentsNode =
            uwaziEntityInstance.metadata["primary_documents"] ?: return emptyList()
        val newAttachments = mutableListOf<VaultFile>()
        ((primaryDocumentsNode[0] as LinkedTreeMap<*, *>).get("value") as ArrayList<String>).forEach { fileId ->
            uwaziEntityInstance.widgetMediaFiles.forEach { vaultFile ->
                if (vaultFile.id?.equals(fileId) == true) {
                    newAttachments.add(vaultFile)
                }
            }
        }
        return newAttachments
    }

    private fun removeDocumentsList(uwaziEntityInstance: UwaziEntityInstance): List<VaultFile?> {
        val primaryDocumentsNode =
            uwaziEntityInstance.metadata["primary_documents"]
                ?: return uwaziEntityInstance.widgetMediaFiles
        val newAttachments = arrayListOf<VaultFile>()
        newAttachments.addAll(uwaziEntityInstance.widgetMediaFiles)
        (((primaryDocumentsNode[0] as ArrayList<*>)[0] as LinkedTreeMap<*, *>)["value"] as ArrayList<*>).forEach {
            fileName ->
            uwaziEntityInstance.widgetMediaFiles.forEach { vaultFile ->
                if (vaultFile.id?.equals(fileName) == true) {
                    newAttachments.remove(vaultFile)
                }
            }
        }
        return newAttachments
    }

    private fun createListOfDocuments(
        attachments: List<VaultFile?>?,
        progressCallBack: SingleLiveEvent<Pair<String, Float>>,
    ): List<MultipartBody.Part?> {
        val listAttachments: MutableList<MultipartBody.Part?> = mutableListOf()
        var fileToUpload: MultipartBody.Part?
        try {
            if (attachments != null) {
                for (i in attachments.indices) {
                    attachments[i]?.let { vaultFile ->

                        val requestBody = MediaFileRequestBody(
                            vaultFile,
                            ProgressListener(vaultFile.id, progressCallBack)
                        )
                        fileToUpload =
                            requestBody.let { file ->
                                MultipartBody.Part.createFormData(
                                    "documents[$i]",
                                    URLEncoder.encode(vaultFile.name, "utf-8"),
                                    file
                                )
                            }
                        listAttachments.add(fileToUpload)
                    }
                }
            }

        } catch (e: Exception) {
            Timber.d(e.message ?: "Error attaching files")
        }
        return listAttachments.toList()
    }

    private fun createListOfAttachments(
        attachments: List<VaultFile?>?,
        progressCallBack: SingleLiveEvent<Pair<String, Float>>,
    ): List<MultipartBody.Part?> {
        val listAttachments: MutableList<MultipartBody.Part?> = mutableListOf()
        var fileToUpload: MultipartBody.Part?
        try {
            if (attachments != null) {
                for (i in attachments.indices) {
                    attachments[i]?.let { vaultFile ->

                        val requestBody = MediaFileRequestBody(
                            vaultFile,
                            ProgressListener(vaultFile.id, progressCallBack)
                        )
                        fileToUpload =
                            requestBody.let { file ->
                                MultipartBody.Part.createFormData(
                                    "attachments[$i]",
                                    URLEncoder.encode(vaultFile.name, "utf-8"),
                                    file
                                )
                            }
                        listAttachments.add(fileToUpload)
                    }
                }
            }

        } catch (e: Exception) {
            Timber.d(e.message ?: "Error attaching files")
        }
        return listAttachments.toList()
    }

    private fun createListOfAttachmentsOriginalName(
        attachments: List<VaultFile?>?
    ): List<String> {
        val listAttachments: MutableList<String> = mutableListOf()
        try {
            if (attachments != null) {
                for (i in attachments.indices) {
                    attachments[i]?.let { vaultFile ->
                        listAttachments.add(vaultFile.name)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.d(e.message ?: "Error attaching files")
        }
        return listAttachments.toList()
    }

    fun refreshEntitiesList() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listUwaziServers().toObservable()
            }
            .flatMap { servers ->
                val singles: MutableList<Single<ListTemplateResult>> = java.util.ArrayList()
                for (server in servers) {
                    singles.add(repository.getTemplatesResult(server))
                }
                Single.zip(
                    singles
                ) { objects: Array<Any?> ->
                    val allResults = ListTemplateResult()
                    for (obj in objects) {
                        if (obj is ListTemplateResult) {
                            val templates =
                                obj.templates
                            val errors =
                                obj.errors
                            allResults.templates.addAll(templates)
                            allResults.errors.addAll(errors)
                        }
                    }
                    allResults
                }.toObservable()
            }.flatMap { result ->
                keyDataSource.uwaziDataSource.flatMap { dataSource ->
                    dataSource.updateBlankTemplatesIfNeeded(result).toObservable()
                }

            }
            .doFinally { _progress.postValue(false) }
            .subscribe({ result ->
                _templates.postValue(result)
            }) { throwable: Throwable? ->
                if (throwable is NoConnectivityException) {
                    _connectionAvailable.postValue(true)
                } else {
                    FirebaseCrashlytics.getInstance().recordException(
                        throwable
                            ?: throw NullPointerException("Expression 'throwable' must not be null")
                    )
                    error.postValue(throwable)
                }
            }
        )
    }
    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}