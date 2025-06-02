package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.util.fromJsonToObjectList
import timber.log.Timber
import java.io.File
import javax.inject.Inject


@HiltViewModel
class SenderViewModel @Inject constructor(
    private val peerClient: TellaPeerToPeerClient
) : ViewModel() {
    private val _prepareResults = MutableLiveData<List<PeerPrepareUploadResponse>>()
    val prepareResults: LiveData<List<PeerPrepareUploadResponse>> = _prepareResults

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
        ip: String,
        port: String,
        expectedFingerprint: String,
        sessionId: String,
        title: String = "Title of the report"
    ) {
        viewModelScope.launch {
            val successfulResponses = mutableListOf<PeerPrepareUploadResponse>()

            for (file in files) {
                val result = peerClient.prepareUpload(
                    ip = ip,
                    port = port,
                    expectedFingerprint = expectedFingerprint,
                    title = title,
                    file = File(file.path),
                    fileId = file.id,
                    sha256 = file.hash,
                    sessionId = sessionId
                )

                result.onSuccess { transmissionId ->
                    Timber.d("Success: transmissionId = $transmissionId")
                    successfulResponses.add(PeerPrepareUploadResponse(transmissionId))
                }.onFailure {
                    Timber.e(it, "Failed to prepare upload")
                    // You can log or ignore errors here — not returned to LiveData
                }
            }

            _prepareResults.postValue(successfulResponses)
        }
    }


}


