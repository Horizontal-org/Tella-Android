package org.horizontal.tella.mobile.views.fragment.peertopeer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.util.fromJsonToObjectList
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class SenderViewModel @Inject constructor(
    private val peerClient: TellaPeerToPeerClient
) : ViewModel() {
    private val _prepareResults = MutableLiveData<PeerPrepareUploadResponse>()
    val prepareResults: LiveData<PeerPrepareUploadResponse> = _prepareResults
    private val _prepareRejected = MutableLiveData<Event<Boolean>>()
    val prepareRejected: LiveData<Event<Boolean>> = _prepareRejected


    fun putVaultFilesInForm(vaultFileList: String): Single<List<VaultFile>> {
        return Single.fromCallable {
            vaultFileList.fromJsonToObjectList(String::class.java) ?: emptyList()
        }
            .flatMap { fileIds ->
                MyApplication.keyRxVault.rxVault
                    .firstOrError()
                    .flatMap { rxVault ->
                        Observable.fromIterable(fileIds)
                            .flatMapSingle { fileId ->
                                rxVault[fileId]
                                    .subscribeOn(Schedulers.io())
                                    .onErrorReturn { null } // safe, allows null
                            }
                            .filter { it != null } // filter out nulls
                            .map { it!! } // safe to force unwrap if you're sure it's not null now
                            .toList()
                    }
            }
            .subscribeOn(Schedulers.io())
    }

    fun mediaFilesToVaultFiles(files: List<FormMediaFile>?): List<VaultFile> {
        val vaultFiles = ArrayList<VaultFile>()
        files?.map { mediaFile ->
            vaultFiles.add(mediaFile.vaultFile)
        }
        return vaultFiles
    }

    fun prepareUploadsFromVaultFiles(
        files: List<VaultFile>,
        title: String = "Title of the report"
    ) {
        val info = PeerSessionManager.getConnectionInfo() ?: run {
            Timber.e("Connection info missing")
            return
        }

        viewModelScope.launch {
            val result = peerClient.prepareUpload(
                ip = info.ip,
                port = info.port,
                expectedFingerprint = info.expectedFingerprint,
                title = title,
                files = files,
                sessionId = info.sessionId
            )

            result.onSuccess { transmissionId ->
                Timber.d("Success: transmissionId = $transmissionId")
                _prepareResults.postValue(PeerPrepareUploadResponse(transmissionId))
            }.onFailure { error ->
                //  if (error.message?.contains("403") == true) {
                withContext(Dispatchers.Main) {
                    _prepareRejected.value = Event(true)
                }
            }
        }
    }


}


