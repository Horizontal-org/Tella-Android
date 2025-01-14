package org.horizontal.tella.mobile.views.activity.viewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.media.MediaFileHandler
import java.util.concurrent.Callable
import javax.inject.Inject


@HiltViewModel
class SharedMediaFileViewModel @Inject constructor(
    application: Application,
    private val keyDataSource: KeyDataSource
) : AndroidViewModel(application) {

    private val disposables = CompositeDisposable()
    private var _error = MutableLiveData<Int>()
    val error: LiveData<Int> get() = _error
    private lateinit var mediaFileDeleteConfirmation: MediaFileDeleteConfirmation

    private var _onMediaFileExportStatus = MutableLiveData<MediaFileExportStatus>()
    val onMediaFileExportStatus: LiveData<MediaFileExportStatus> get() = _onMediaFileExportStatus

    private var _onMediaFileDeleted = MutableLiveData<Boolean>()
    val onMediaFileDeleted: LiveData<Boolean> get() = _onMediaFileDeleted

    private var _onMediaFileDeleteConfirmed = MutableLiveData<MediaFileDeleteConfirmation>()
    val onMediaFileDeleteConfirmed: LiveData<MediaFileDeleteConfirmation> get() = _onMediaFileDeleteConfirmed

    private var _onMediaFileRenamed = MutableLiveData<VaultFile>()
    val onMediaFileRenamed: LiveData<VaultFile> get() = _onMediaFileRenamed

    private var _onMediaFileGot = MutableLiveData<VaultFile>()
    val onMediaFileGot: LiveData<VaultFile> get() = _onMediaFileGot

    /**
     * Exports the given [vaultFile] to the specified [path] on the device, optionally including its metadata.
     *
     * @param withMetadata If true, the metadata of the [vaultFile] will also be exported.
     * @param vaultFile The VaultFile to be exported.
     * @param path The Uri of the destination path where the file will be exported.
     */
    fun exportNewMediaFile(withMetadata: Boolean, vaultFile: VaultFile, path: Uri?) {
        disposables.add(
            // Export the main vaultFile
            Completable.fromCallable(Callable<Void?> {
                MediaFileHandler.exportMediaFile(
                    getApplication(),
                    vaultFile,
                    path
                )
                // Check if metadata should also be exported
                if (withMetadata && vaultFile.metadata != null) {
                    // Export the media file
                    MediaFileHandler.exportMediaFile(
                        getApplication(),
                        MediaFileHandler.maybeCreateMetadataMediaFile(vaultFile),
                        path
                    )
                }
                null // Completable doesn't return a value
            })
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe {
                    // Notify observers that the export process has started
                    _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_START)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    // Notify observers that the export process has ended
                    _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_END)
                }
                .subscribe(
                    { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_PROGRESS) }
                ) { throwable: Throwable? ->
                    // Handle any errors that occurred during the export process
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    _error.postValue(R.string.gallery_toast_fail_exporting_to_device)

                }
        )
    }

    /**
     * Renames a VaultFile with the specified [id] to the given [name].
     *
     * @param id The unique identifier of the VaultFile to be renamed.
     * @param name The new name for the VaultFile.
     */

    fun renameVaultFile(id: String, name: String) {
        disposables.add(MyApplication.rxVault.rename(id, name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { vaultFile: VaultFile? ->
                    // Notify observers that the renaming was successful and provide the updated VaultFile
                    _onMediaFileRenamed.postValue(vaultFile)
                }
            ) { throwable: Throwable? ->
                // Handle any errors that occurred during the renaming process
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(R.string.gallery_toast_fail_deleting_files)
            })
    }

    /**
     * Deletes the specified [vaultFile] from the application's media storage.
     *
     * @param vaultFile The VaultFile to be deleted.
     */
    fun deleteMediaFiles(vaultFile: VaultFile) {
        disposables.add(MyApplication.rxVault.delete(vaultFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                // Notify observers that the deletion was successful, providing a boolean flag indicating success
                { isDeleted: Boolean? -> _onMediaFileDeleted.postValue(isDeleted) }
            ) { throwable: Throwable? ->
                // Handle any errors that occurred during the deletion process
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(R.string.gallery_toast_fail_deleting_files)

            })
    }


    /**
     * Confirms whether to delete the specified [vaultFile] based on its association with other FormMediaFiles.
     *
     * @param vaultFile The VaultFile to be confirmed for deletion.
     */
    fun confirmDeleteMediaFile(vaultFile: VaultFile) {
        disposables.add(
            keyDataSource.dataSource
                .flatMapSingle { obj: DataSource -> obj.reportMediaFiles }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ formMediaFileList: List<FormMediaFile> ->
                    if (formMediaFileList.isEmpty()) {
                        // Set confirmation based on the association with FormMediaFiles
                        mediaFileDeleteConfirmation = MediaFileDeleteConfirmation(vaultFile, false)
                        _onMediaFileDeleteConfirmed.postValue(mediaFileDeleteConfirmation)
                    } else {
                        var isShowConfirmation = false
                        //#1. Iterate through list
                        for (formMediaFile in formMediaFileList) {
                            if (formMediaFile.id == vaultFile.id) {
                                isShowConfirmation = true
                                break
                            }
                        }
                        mediaFileDeleteConfirmation =
                            MediaFileDeleteConfirmation(vaultFile, isShowConfirmation)
                        _onMediaFileDeleteConfirmed.postValue(mediaFileDeleteConfirmation)
                    }
                }, { throwable: Throwable? ->
                    // Handle any errors that occurred during data retrieval
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    _error.postValue(R.string.gallery_toast_fail_deleting_files)
                })
        )
    }

    /**
     * Retrieves a VaultFile based on its [id].
     *
     * @param id The ID of the VaultFile to retrieve.
     */
    fun getMediaFile(id: String) {
        disposables.add(
            Single
                .fromCallable {
                    MyApplication.rxVault[id]
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFile: Single<VaultFile>? ->
                        if (vaultFile == null) {
                            _error.postValue(R.string.default_error_msg)
                        } else {
                            _onMediaFileGot.postValue(vaultFile.blockingGet())
                        }
                    }
                ) { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    _error.postValue(R.string.default_error_msg)
                })
    }

}