package rs.readahead.washington.mobile.views.activity.viewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.media.MediaFileHandler
import java.util.concurrent.Callable

class SharedMediaFileViewModel(application: Application) : AndroidViewModel(application) {
    private val disposables = CompositeDisposable()
    private var _error = MutableLiveData<Throwable>()
    val error : LiveData<Throwable> get() = _error


    private var _onMediaFileExportStatus = MutableLiveData<MediaFileExportStatus>()
    val onMediaFileExportStatus : LiveData<MediaFileExportStatus> get() = _onMediaFileExportStatus


    fun exportNewMediaFile(withMetadata: Boolean, vaultFile: VaultFile, path: Uri?) {
        disposables.add(Completable.fromCallable(Callable<Void?> {
            MediaFileHandler.exportMediaFile(
                getApplication(),
                vaultFile,
                path
            )
            if (withMetadata && vaultFile.metadata != null) {
                MediaFileHandler.exportMediaFile(
                    getApplication(),
                    MediaFileHandler.maybeCreateMetadataMediaFile(vaultFile),
                    path
                )
            }
            null
        })
            .subscribeOn(Schedulers.computation())
            .doOnSubscribe { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_START) } //onExportStarted
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_END) } //view.onExportEnded()
            .subscribe(
                {  _onMediaFileExportStatus.postValue(MediaFileExportStatus.EXPORT_PROGRESS) } //view.onMediaExported()
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(
                        throwable
                    )
            }
        )
    }

}