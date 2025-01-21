package org.horizontal.tella.mobile.views.fragment.vault.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.tella_vault.rx.RxVault
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.BuildConfig
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.database.UwaziDataSource
import org.horizontal.tella.mobile.data.googledrive.GoogleDriveRepository
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.entity.googledrive.Config
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.entity.uwazi.CollectTemplate
import org.horizontal.tella.mobile.media.MediaFileHandler
import javax.inject.Inject


@HiltViewModel
class HomeVaultViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val keyDataSource: KeyDataSource,
    private val config: Config
) : ViewModel() {
    private val disposables = CompositeDisposable()
    private val rxVault: RxVault? = MyApplication.rxVault

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _recentFiles = MutableLiveData<List<VaultFile?>>()
    val recentFiles: LiveData<List<VaultFile?>> get() = _recentFiles

    private val _recentFilesError = MutableLiveData<Throwable>()
    val recentFilesError: LiveData<Throwable> get() = _recentFilesError

    private val _favoriteCollectForms = MutableLiveData<List<CollectForm>>()
    val favoriteCollectForms: LiveData<List<CollectForm>> get() = _favoriteCollectForms

    private val _favoriteCollectFormsError = MutableLiveData<Throwable>()
    val favoriteCollectFormsError: LiveData<Throwable> get() = _favoriteCollectFormsError

    // LiveData properties for favorite collect templates
    private val _favoriteCollectTemplates = MutableLiveData<List<CollectTemplate>>()
    val favoriteCollectTemplates: LiveData<List<CollectTemplate>> get() = _favoriteCollectTemplates

    private val _favoriteCollectTemplatesError = MutableLiveData<Throwable?>()
    val favoriteCollectTemplatesError: LiveData<Throwable?> get() = _favoriteCollectTemplatesError

    // LiveData for server counts
    private val _serverCounts = MutableLiveData<ServerCounts>()
    val serverCounts: LiveData<ServerCounts> get() = _serverCounts

    // LiveData for server counts errors
    private val _serverCountError = MutableLiveData<Throwable?>()
    val serverCountError: LiveData<Throwable?> get() = _serverCountError

    private val _mediaExportStatus = MutableLiveData<ExportStatus>()
    val mediaExportStatus: LiveData<ExportStatus> get() = _mediaExportStatus

    // Enum class to represent the export status
    enum class ExportStatus {
        STARTED,
        ENDED,
        SUCCESS,
        ERROR
    }
    // Execute Panic Mode
    fun executePanicMode() {
        keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { dataSource: DataSource ->
                if (Preferences.isDeleteGalleryEnabled()) {
                    rxVault?.destroy()?.blockingAwait()
                    MediaFileHandler.destroyGallery(appContext)
                }
                if (Preferences.isDeleteServerSettingsActive()) {
                    dataSource.deleteDatabase()
                } else {
                    if (Preferences.isEraseForms()) {
                        dataSource.deleteFormsAndRelatedTables()
                    }
                }
                clearSharedPreferences()
                MyApplication.exit(appContext)
                MyApplication.resetKeys()
                if (Preferences.isUninstallOnPanic()) {
                    uninstallTella(appContext)
                }
                Completable.complete()
            }
            .blockingAwait()
    }

    // Fetch server counts
    fun countAllServers() {
        val dropBoxCount = keyDataSource.dropBoxDataSource
            .subscribeOn(Schedulers.io())
            .flatMapSingle { it.listDropBoxServers() }

        val googleDriveCount = keyDataSource.googleDriveDataSource
            .subscribeOn(Schedulers.io())
            .flatMapSingle { it.listGoogleDriveServers(config.googleClientId) }

        val tellaUploadCount = keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .flatMapSingle { it.listTellaUploadServers() }

        val collectServersCount = keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .flatMapSingle { it.listCollectServers() }

        val uwaziServersCount = keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .flatMapSingle { it.listUwaziServers() }

        val nextCloudServerCount = keyDataSource.nextCloudDataSource
            .subscribeOn(Schedulers.io())
            .flatMapSingle { it.listNextCloudServers() }

        disposables.add(
            Single.zip(
                dropBoxCount.firstOrError(),
                googleDriveCount.firstOrError(),
                tellaUploadCount.firstOrError(),
                collectServersCount.firstOrError(),
                uwaziServersCount.firstOrError(),
                nextCloudServerCount.firstOrError()
            ) { dropboxServers: List<DropBoxServer>,
                googleDriveServers: List<GoogleDriveServer>,
                tellaUploadServers: List<TellaReportServer>,
                collectServers: List<CollectServer>,
                uwaziServers: List<UWaziUploadServer>,
                nextCloudServers: List<NextCloudServer> ->
                ServerCounts(
                    dropBoxServers = dropboxServers,
                    googleDriveServers = googleDriveServers,
                    tellaUploadServers = tellaUploadServers,
                    collectServers = collectServers,
                    uwaziServers = uwaziServers,
                    nextCloudServers = nextCloudServers
                )
            }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { serverCounts ->
                        _serverCounts.value = serverCounts
                    },
                    { throwable ->
                        _serverCountError.value = throwable
                    }
                )
        )
    }

    fun exportMediaFiles(vaultFiles: List<VaultFile?>) {
        disposables.add(
            Single.fromCallable {
                val resultList = MediaFileHandler.walkAllFiles(vaultFiles)
                for (vaultFile in resultList) {
                    // Uncomment this line to export the files
                    // vaultFile?.let { mediaFileHandler.exportMediaFile(context, it) }
                }
                vaultFiles.size
            }
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { _mediaExportStatus.postValue(ExportStatus.STARTED) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _mediaExportStatus.postValue(ExportStatus.ENDED) }
                .subscribe(
                    { num: Int -> _mediaExportStatus.postValue(ExportStatus.SUCCESS) },
                    { throwable: Throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _mediaExportStatus.postValue(ExportStatus.ERROR)
                    }
                )
        )
    }

    // Get Recent Files
    fun getRecentFiles(filterType: FilterType?, sort: Sort?, limits: Limits) {
        rxVault?.list(filterType, sort, limits)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(
                { vaultFiles: List<VaultFile?> ->
                    _recentFiles.value = vaultFiles
                },
                { throwable: Throwable ->
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                    _recentFilesError.value = throwable
                }
            )?.let { disposables.add(it) }
    }


    // Get Favorite Collect Forms
    fun getFavoriteCollectForms() {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .flatMap { dataSource: DataSource ->
                    dataSource.listFavoriteCollectForms().toObservable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { forms: List<CollectForm> ->
                        _favoriteCollectForms.value = forms
                    },
                    { throwable: Throwable ->
                        _favoriteCollectFormsError.value = throwable
                    }
                )
        )
    }

    // Fetch favorite collect templates
    fun getFavoriteCollectTemplates() {
        disposables.add(
            keyDataSource.uwaziDataSource
                .subscribeOn(Schedulers.io())
                .flatMap { dataSource: UwaziDataSource ->
                    dataSource.listFavoriteTemplates().toObservable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { templates: List<CollectTemplate>? ->
                        _favoriteCollectTemplates.value = templates ?: emptyList()
                    },
                    { throwable: Throwable? ->
                        _favoriteCollectTemplatesError.value = throwable
                    }
                )
        )
    }
    // Clear Shared Preferences
    private fun clearSharedPreferences() {
        Preferences.setPanicMessage(null)
    }

    // Uninstall Tella
    private fun uninstallTella(context: Context) {
        val packageUri = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
        context.startActivity(intent)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}
