package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.UwaziDataSource
import rs.readahead.washington.mobile.data.repository.MediaFileRequestBody
import rs.readahead.washington.mobile.data.repository.ProgressListener
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.presentation.uwazi.SendEntityRequest
import rs.readahead.washington.mobile.views.fragment.uwazi.send.MULTIPART_FORM_DATA


class SharedUwaziSubmissionViewModel : ViewModel(){
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    private val _instance = SingleLiveEvent<UwaziEntityInstance>()
    val instance: LiveData<UwaziEntityInstance> get() = _instance
    private val _template = MutableLiveData<CollectTemplate>()
    val template: LiveData<CollectTemplate> get() = _template
    private val _attachedFiles = MutableLiveData<List<VaultFile>>()
    val attachedFiles: LiveData<List<VaultFile>> get() = _attachedFiles
    private val repository = UwaziRepository()
    val progress = MutableLiveData<UwaziEntityStatus>()
    private val _server = MutableLiveData<UWaziUploadServer>()
    val server: LiveData<UWaziUploadServer> get() = _server
    var error = MutableLiveData<Throwable>()
    private val _attachments = MutableLiveData<List<FormMediaFile>>()
    val attachments: LiveData<List<FormMediaFile>> get() = _attachments
    //TODO THIS IS UGLY WILL REPLACE IT FLOWABLE RX LATER
    private val _progressCallBack = SingleLiveEvent<Pair<String, Float>>()
    val progressCallBack: LiveData<Pair<String, Float>> get() = _progressCallBack

    fun saveEntityInstance(instance : UwaziEntityInstance) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: UwaziDataSource -> dataSource.saveEntityInstance(instance).toObservable() }
            .doFinally { progress.postValue(instance.status)   }
            .subscribe ({
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
                        submitEntity(server, entity)
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

    fun submitEntity(server: UWaziUploadServer, entity: UwaziEntityInstance) {
        disposables.add(
            repository.submitEntity(
                server = server,
                entity = createRequestBody(Gson().toJson(entity.createEntityRequest())),
                attachments = createListOfAttachments(entity.widgetMediaFiles, _progressCallBack)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError {  progress.postValue(UwaziEntityStatus.SUBMISSION_ERROR) }
                .flatMap {
                    entity.status = UwaziEntityStatus.SUBMITTED
                    entity.formPartStatus = FormMediaFileStatus.SUBMITTED
                    keyDataSource.uwaziDataSource.blockingFirst().saveEntityInstance(entity)
                }
                .subscribe({
                    progress.postValue(UwaziEntityStatus.SUBMITTED)
                }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(
                        throwable
                            ?: throw NullPointerException("Expression 'throwable' must not be null")
                    )
                    error.postValue(throwable)
                    progress.postValue(UwaziEntityStatus.SUBMISSION_ERROR)
                })
    }

    private fun UwaziEntityInstance.createEntityRequest() = SendEntityRequest(
        attachments = emptyList(),
        metadata = metadata,
        template = collectTemplate?.entityRow?._id ?: "",
        title = title,
        type = type
    )

    private fun createRequestBody(s: String): RequestBody {
        return RequestBody.create(
            MediaType.parse(MULTIPART_FORM_DATA), s
        )
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
                    attachments[i]?.let {

                        val requestBody = MediaFileRequestBody(
                            it,
                            ProgressListener(it.id, progressCallBack)
                        )
                        fileToUpload =
                            requestBody.let { it1 ->
                                MultipartBody.Part.createFormData(
                                    "attachments",
                                    it.name,
                                    it1
                                )
                            }
                        listAttachments.add(fileToUpload)
                    }
                }
            }

        } catch (e: Exception) {
        }
        return listAttachments.toList()
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}