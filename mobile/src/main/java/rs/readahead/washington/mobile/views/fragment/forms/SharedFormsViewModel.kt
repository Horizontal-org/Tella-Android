package rs.readahead.washington.mobile.views.fragment.forms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.AsyncSubject
import org.javarosa.core.model.FormDef
import org.javarosa.core.model.instance.InstanceInitializationFactory
import org.javarosa.core.reference.ReferenceManager
import org.javarosa.form.api.FormEntryController
import org.javarosa.form.api.FormEntryModel
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.event.ShowBlankFormEntryEvent
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository
import rs.readahead.washington.mobile.domain.entity.collect.*
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository
import rs.readahead.washington.mobile.odk.FormController
import java.util.*

class SharedFormsViewModel(private val mApplication: Application) : AndroidViewModel(mApplication) {

    var onCreateFormController = MutableLiveData<FormController?>()
    var onGetBlankFormDefSuccess = MutableLiveData<FormPair>()
    var onBlankFormsListResult = MutableLiveData<ListFormResult>()
    var onError = MutableLiveData<Throwable>()
    var onFormDefError = MutableLiveData<Throwable>()
    var showBlankFormRefreshLoading = MutableLiveData<Boolean>()
    var onNoConnectionAvailable = MutableLiveData<Boolean>()
    var onBlankFormDefRemoved = MutableLiveData<Boolean>()
    var onDownloadBlankFormDefStart = MutableLiveData<Boolean>()
    var onUpdateBlankFormDefStart = MutableLiveData<Boolean>()
    var onUpdateBlankFormDefSuccess = MutableLiveData<Pair<CollectForm, FormDef?>>()
    var onDownloadBlankFormDefSuccess = MutableLiveData<CollectForm?>()
    var onInstanceFormDefSuccess = MutableLiveData<CollectFormInstance>()
    var onToggleFavoriteSuccess = MutableLiveData<CollectForm?>()
    var onFormInstanceDeleteSuccess = MutableLiveData<Boolean>()
    var onCountCollectServersEnded = MutableLiveData<Long>()
    var onUserCancel = MutableLiveData<Boolean>()
    var showFab = MutableLiveData<Boolean>()
    var onFormInstanceListSuccess = MutableLiveData<List<CollectFormInstance>>()
    var onDraftFormInstanceListSuccess = MutableLiveData<List<CollectFormInstance>>()
    var onFormInstanceListError = MutableLiveData<Throwable>()

    private var keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    private var odkRepository: IOpenRosaRepository = OpenRosaRepository()
    private val asyncDataSource = AsyncSubject.create<DataSource>()


    fun createFormController(collectForm: CollectForm, formDef: FormDef) {
        try {
            val instance = CollectFormInstance()
            instance.serverId = collectForm.serverId
            instance.serverName = collectForm.serverName
            instance.username = collectForm.username
            instance.status = CollectFormInstanceStatus.UNKNOWN
            instance.formID = collectForm.form.formID
            instance.version = collectForm.form.version
            instance.formName = collectForm.form.name
            instance.instanceName = collectForm.form.name
            val fc = createFormController(instance, formDef)
            onCreateFormController.postValue(fc)

        } catch (throwable: Throwable) {
            onError.postValue(throwable)
        }
    }

    fun createFormController(instance: CollectFormInstance) {
        try {
            val fc: FormController = createFormController(instance, instance.formDef)
            onCreateFormController.postValue(fc)
        } catch (throwable: Throwable) {
            onError.postValue(throwable)
        }
    }

    private fun createFormController(
        instance: CollectFormInstance,
        formDef: FormDef?
    ): FormController {
        requireNotNull(formDef)
        val fem = FormEntryModel(formDef)
        val fec = FormEntryController(fem)
        val fc = FormController(fec, instance)
        FormController.setActive(fc)

        // true - no saved instance data..
        formDef.initialize(true, InstanceInitializationFactory())

        // Remove previous forms

        // Remove previous forms
        //ReferenceManager.`__`().clearSession()
         ReferenceManager.`_`().clearSession();

        // This should get moved to the Application Class
        /*if (ReferenceManager._().getFactories().length == 0) {
            // this is /sdcard/odk
            ReferenceManager._().addReferenceFactory(new FileReferenceFactory(Collect.ODK_ROOT));
        }*/fc.initFormChangeTracking() // set clear form to track changes
        return fc
    }

    fun getBlankFormDef(form: CollectForm?) {
        keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: DataSource ->
                dataSource.getBlankFormDef(form).toObservable()
            }
            ?.subscribe(
                { formDef: FormDef? ->
                    //onGetBlankFormDefSuccess.postValue(FormPair(form, formDef))
                    if (form != null && formDef != null) {
                        onGetBlankFormDefSuccess.postValue(FormPair(form, formDef))
                        //OMG
                        MyApplication.bus().post(ShowBlankFormEntryEvent(FormPair(form, formDef)))
                    }
                },
                { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    onFormDefError.postValue(throwable)
                }
            )?.let {
                disposables.add(
                    it
            )
            }
    }

    fun getInstanceFormDef(instanceId: Long) {
        var collectFormInstance: CollectFormInstance? = null
        disposables.add(keyDataSource.dataSource
            .flatMapSingle { dataSource: DataSource ->
                dataSource.getInstance(
                    instanceId
                )
            }
            .flatMapSingle { instance: CollectFormInstance ->
                collectFormInstance = instance
                MyApplication.rxVault[instance.widgetMediaFilesIds]
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ vaultFiles: List<VaultFile> ->
                for (file in vaultFiles) {
                    collectFormInstance?.setWidgetMediaFile(
                        file.name,
                        FormMediaFile.fromMediaFile(file)
                    )
                }
                onInstanceFormDefSuccess.postValue(
                    collectFormInstance?.let { maybeCloneInstance(it) }
                )
            }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                onFormDefError.postValue(throwable)
            }
        )
    }

    private fun maybeCloneInstance(instance: CollectFormInstance): CollectFormInstance {
        if (instance.status == CollectFormInstanceStatus.SUBMITTED) {
            instance.clonedId = instance.id // we are clone of submitted form
            instance.id = 0
            instance.status = CollectFormInstanceStatus.UNKNOWN
            instance.updated = 0
            instance.instanceName = instance.formName
            for (mediaFile in instance.widgetMediaFiles) {
                mediaFile.status = FormMediaFileStatus.UNKNOWN
            }
        }
        return instance
    }
    fun toggleFavorite(collectForm: CollectForm?) {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle { dataSource: DataSource ->
                dataSource.toggleFavorite(
                    collectForm
                )
            }
            .subscribe(
                { form: CollectForm? ->
                    onToggleFavoriteSuccess.postValue(
                        form
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                onError.postValue(throwable)
            }
        )
    }

    fun deleteFormInstance(id: Long) {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { dataSource: DataSource ->
                dataSource.deleteInstance(
                    id
                )
            }
            .subscribe(
                { onFormInstanceDeleteSuccess.postValue(true) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                onError.postValue(throwable)
            }
        )
    }

    fun countCollectServers() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle { obj: DataSource -> obj.countCollectServers() }
            .subscribe(
                { num: Long ->
                   onCountCollectServersEnded.postValue(
                        num
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                onError.postValue(throwable)
            }
        )
    }

    fun refreshBlankForms() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { showBlankFormRefreshLoading.postValue(true) }
            .flatMap { dataSource: DataSource ->
                dataSource.listCollectServers().toObservable()
            }
            .flatMap { servers: List<CollectServer> ->
                if (servers.isEmpty()) {
                   // return Single.just(ListFormResult()).toObservable()
                }
                if (!MyApplication.isConnectedToInternet(mApplication.baseContext)) {
                    throw NoConnectivityException()
                }
                val singles: MutableList<Single<ListFormResult>> =
                    ArrayList()
                for (server in servers) {
                    singles.add(odkRepository.formList(server))
                }
                Single.zip(
                    singles
                ) { objects: Array<Any?> ->
                    val allResults = ListFormResult()
                    for (obj in objects) {
                        if (obj is ListFormResult) {
                            val forms =
                                obj.forms
                            val errors =
                                obj.errors
                            allResults.forms.addAll(forms)
                            allResults.errors.addAll(errors)
                        }
                    }
                    allResults
                }.toObservable()
            }
            .flatMap { listFormResult: ListFormResult? ->
                keyDataSource.dataSource.flatMap { dataSource: DataSource ->
                    dataSource.updateBlankForms(
                        listFormResult
                    ).toObservable()
                }
            }
            .doFinally { showBlankFormRefreshLoading.postValue(false) }
            .subscribe(
                { listFormResult: ListFormResult ->
                    // log errors if any in result..
                    for (error in listFormResult.errors) {
                        FirebaseCrashlytics.getInstance().recordException(error.exception)
                    }
                    onBlankFormsListResult.postValue(listFormResult)
                }
            ) { throwable: Throwable? ->
                if (throwable is NoConnectivityException) {
                    onNoConnectionAvailable.postValue(true)
                } else {
                    FirebaseCrashlytics.getInstance().recordException(throwable ?: throw NullPointerException("Expression 'throwable' must not be null"))
                    onError.postValue(throwable)
                }
            }
        )
    }

    fun listBlankForms() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: DataSource ->
                dataSource.listBlankForms().toObservable()
            }
            .subscribe(
                { forms: List<CollectForm>? ->
                    onBlankFormsListResult.postValue(
                        ListFormResult(forms)
                    )
                }
            ) { throwable: Throwable? ->
                onError.postValue(
                    throwable
                )
            }
        )
    }

    fun removeBlankFormDef(form: CollectForm?) {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { dataSource: DataSource ->
                dataSource.removeBlankFormDef(
                    form
                )
            }
            .subscribe(
                { onBlankFormDefRemoved.postValue(true) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable ?: throw NullPointerException("Expression 'throwable' must not be null"))
                onError.postValue(throwable)
            }
        )
    }

    fun downloadBlankFormDef(form: CollectForm) {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { onDownloadBlankFormDefStart.postValue(true) }
            .flatMap { dataSource: DataSource ->
                dataSource.getCollectServer(
                    form.serverId
                ).toObservable()
            }.flatMap { server: CollectServer? ->
                odkRepository.getFormDef(
                    server,
                    form
                ).toObservable()
            }.flatMap { formDef: FormDef? ->
                keyDataSource.dataSource.flatMap { dataSource: DataSource ->
                    dataSource.updateBlankFormDef(
                        form,
                        formDef
                    ).toObservable()
                }
            }
            .doFinally { onDownloadBlankFormDefStart.postValue(false) }
            .subscribe(
                {
                    onDownloadBlankFormDefSuccess.postValue(
                        form
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable ?: throw NullPointerException("Expression 'throwable' must not be null"))
                onFormDefError.postValue(throwable)
            }
        )
    }

    fun updateBlankFormDef(form: CollectForm) {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { onUpdateBlankFormDefStart.postValue(true) }
            .flatMap { dataSource: DataSource ->
                dataSource.getCollectServer(
                    form.serverId
                ).toObservable()
            }.flatMap { server: CollectServer? ->
                odkRepository.getFormDef(
                    server,
                    form
                ).toObservable()
            }.flatMap { formDef: FormDef? ->
                keyDataSource.dataSource.flatMap { dataSource: DataSource ->
                    dataSource.updateBlankCollectFormDef(
                        form,
                        formDef
                    ).toObservable()
                }
            }
            .doFinally { onUpdateBlankFormDefStart.postValue(false) }
            .subscribe(
                { formDef: FormDef? ->
                    onUpdateBlankFormDefSuccess.postValue(Pair(
                        form,
                        formDef)
                    )
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                onFormDefError.postValue(throwable)
            }
        )
    }

    fun listDraftFormInstances() {
        disposables.add(asyncDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle { obj: DataSource -> obj.listDraftForms() }
            .subscribe(
                { forms: List<CollectFormInstance> ->
                    onDraftFormInstanceListSuccess.postValue(forms)
                },
                { throwable: Throwable? ->
                   onFormInstanceListError.postValue(
                        throwable
                    )
                }
            )
        )
    }

    fun listSubmitFormInstances() {
        disposables.add(asyncDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle { obj: DataSource -> obj.listSentForms() }
            .subscribe(
                { forms: List<CollectFormInstance> ->
                    onFormInstanceListSuccess.postValue(
                        forms
                    )
                },
                { throwable: Throwable? ->
                   onFormInstanceListError.postValue(
                        throwable
                    )
                }
            )
        )
    }

    fun userCancel() {
        disposables.clear()
        onUserCancel.postValue(true)
    }
}