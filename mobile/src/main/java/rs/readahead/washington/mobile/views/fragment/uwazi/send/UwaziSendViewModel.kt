package rs.readahead.washington.mobile.views.fragment.uwazi.send

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.UwaziDataSource
import rs.readahead.washington.mobile.data.repository.MediaFileRequestBody
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.presentation.uwazi.SendEntityRequest
const val MULTIPART_FORM_DATA = "text/plain"

class UwaziSendViewModel : ViewModel() {

    private val repository = UwaziRepository()
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val _entitySubmitted = MutableLiveData<Boolean>()
    val entitySubmitted: LiveData<Boolean> get() = _entitySubmitted
    private val _server = MutableLiveData<UWaziUploadServer>()
    val server: LiveData<UWaziUploadServer> get() = _server
    var error = MutableLiveData<Throwable>()
    private val _attachments = MutableLiveData<List<FormMediaFile>>()
    val attachments: LiveData<List<FormMediaFile>> get() = _attachments

    init {
        getRootId()
    }

    fun getUwaziServer(serverID : Long) {
        keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.getUwaziServerById(serverID).toObservable()
            }
            ?.subscribe(
                { server: UWaziUploadServer? ->
                    if (server  != null) {
                        _server.postValue(server)
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

    fun getSelectedVaultFiles(){

    }

    fun submitEntity(server: UWaziUploadServer, sendEntityRequest: SendEntityRequest,attachments : List<VaultFile>?) {
        disposables.add(
            repository.submitEntity(
                server = server,
                title = createRequestBody(sendEntityRequest.title),
                template = createRequestBody(sendEntityRequest.template),
                type = createRequestBody(sendEntityRequest.type),
                metadata = createRequestJson(Gson().toJson(sendEntityRequest.metadata)),
                attachments = createListOfAttachments(attachments))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _progress.postValue(true) }
                .doFinally { _progress.postValue(false) }
                .subscribe({
                    _entitySubmitted.postValue(true)
                }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(
                        throwable
                            ?: throw NullPointerException("Expression 'throwable' must not be null")
                    )
                    error.postValue(throwable)
                })
    }


    fun getFiles(parent: String?) {
        MyApplication.rxVault.get(parent)
            .subscribe(
                { vaultFile: VaultFile? ->
                    disposables.add(MyApplication.rxVault.list(vaultFile, null, null, null)
                        .subscribeOn(Schedulers.io())
                        .doOnSubscribe {  }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally {  }
                        .subscribe(
                            { vaultFiles: List<VaultFile> ->
                                val listFormMediaFiles = arrayListOf<FormMediaFile>()
                                vaultFiles.map {
                                    listFormMediaFiles.add(FormMediaFile.fromMediaFile(it))
                                }
                                _attachments.postValue(listFormMediaFiles)
                            }
                        ) { throwable: Throwable? ->
                            FirebaseCrashlytics.getInstance().recordException(throwable!!)
                            error.postValue(throwable)
                        })
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                error.postValue(throwable)
            }.dispose()
    }

    private fun getRootId() {
        MyApplication.rxVault?.root
            ?.subscribe(
                { vaultFile: VaultFile? -> getFiles(vaultFile?.id) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                error.postValue(throwable)
            }?.dispose()
    }

    private fun createListOfAttachments(
        attachments: List<VaultFile?>?
    ): List<MultipartBody.Part?> {

        val listAttachments: MutableList<MultipartBody.Part?> = mutableListOf()
        var fileToUpload: MultipartBody.Part?
        try {
            if (attachments != null) {
                for (i in attachments.indices) {
                    attachments[i]?.let {

                        val requestBody = MediaFileRequestBody(it)
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

    fun createRequestBody(s: String): RequestBody {
        return RequestBody.create(
            MediaType.parse(MULTIPART_FORM_DATA), s)
    }
    fun createRequestJson(s: String): RequestBody {
        return RequestBody.create(
            MediaType.parse("application/json"), s)
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}