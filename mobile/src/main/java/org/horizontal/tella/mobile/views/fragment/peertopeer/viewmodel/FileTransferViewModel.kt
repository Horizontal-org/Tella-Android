package org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel

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
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus
import org.horizontal.tella.mobile.data.peertopeer.remote.PeerUploadOutcome
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadResult
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.util.Event
import org.horizontal.tella.mobile.util.fromJsonToObjectList
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PeerToPeerParticipant
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.UploadProgressState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FileTransferViewModel @Inject constructor(
    private val peerClient: TellaPeerToPeerClient,
    var p2PSharedState: P2PSharedState
) : ViewModel() {
    private val _prepareResults = SingleLiveEvent<PeerPrepareUploadResponse>()
    val prepareResults: SingleLiveEvent<PeerPrepareUploadResponse> = _prepareResults
    /** 403 → recipient-rejected handling; other failures → pop to Nearby Sharing root + generic error. */
    private val _prepareFailure = SingleLiveEvent<Event<PrepareFailureKind>>()
    val prepareFailure: SingleLiveEvent<Event<PrepareFailureKind>> = _prepareFailure
    private val _uploadProgress = SingleLiveEvent<UploadProgressState>()
    val uploadProgress: SingleLiveEvent<UploadProgressState> get() = _uploadProgress
    var peerToPeerParticipant: PeerToPeerParticipant = PeerToPeerParticipant.SENDER

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
                            .filter { true } // filter out nulls
                            .map { it } // safe to force unwrap if you're sure it's not null now
                            .toList()
                    }
            }
            .subscribeOn(Schedulers.io())
    }

    fun prepareUploadsFromVaultFiles() {
        Timber.d("session id ***prepareUploadsFromVaultFiles ${p2PSharedState.session?.sessionId}")

        viewModelScope.launch {
            when (val result = peerClient.prepareUpload(
                ip = p2PSharedState.ip,
                port = p2PSharedState.port,
                expectedFingerprint = p2PSharedState.hash,
                title = getTitleFromState(),
                files = getVaultFilesFromState(),
                sessionId = getSessionId()
            )) {
                is PrepareUploadResult.Success -> {
                    _prepareResults.postValue(PeerPrepareUploadResponse(result.transmissions))
                }

                is PrepareUploadResult.Forbidden -> {
                    withContext(Dispatchers.Main) {
                        Timber.w("Rejected")
                        _prepareFailure.value = Event(PrepareFailureKind.RECIPIENT_REJECTED)
                    }
                }

                is PrepareUploadResult.BadRequest -> {
                    withContext(Dispatchers.Main) {
                        Timber.e("Bad request – possibly invalid data")
                        _prepareFailure.value = Event(PrepareFailureKind.GENERIC)
                    }
                }

                is PrepareUploadResult.Conflict -> {
                    withContext(Dispatchers.Main) {
                        Timber.e("Upload conflict – another session may be active")
                        _prepareFailure.value = Event(PrepareFailureKind.GENERIC)
                    }
                }

                is PrepareUploadResult.TooManyRequests -> {
                    withContext(Dispatchers.Main) {
                        Timber.w("Prepare upload rate limited (429)")
                        _prepareFailure.value = Event(PrepareFailureKind.GENERIC)
                    }
                }

                is PrepareUploadResult.ServerError -> {
                    withContext(Dispatchers.Main) {
                        Timber.e("Internal server error – try again later")
                        _prepareFailure.value = Event(PrepareFailureKind.GENERIC)
                    }
                }

                is PrepareUploadResult.Failure -> {
                    withContext(Dispatchers.Main) {
                        Timber.e(result.exception, "Unhandled error during upload")
                        _prepareFailure.value = Event(PrepareFailureKind.GENERIC)
                    }
                }

            }
        }
    }

    fun uploadAllFiles() {
        viewModelScope.launch {
            val session = p2PSharedState.session ?: return@launch
            val ip = p2PSharedState.ip
            val port = p2PSharedState.port
            val fingerprint = p2PSharedState.hash

            val totalSize = session.files.values.sumOf { it.vaultFile?.size ?: 0L }

            fun postProgress() {
                val uploaded = session.files.values.sumOf { it.bytesTransferred }
                val percent = if (totalSize > 0) ((uploaded * 100) / totalSize).toInt() else 0
                _uploadProgress.postValue(
                    UploadProgressState(
                        title = session.title.orEmpty(),
                        percent = percent,
                        sessionStatus = session.status,
                        files = session.files.values.toList()
                    )
                )
            }

            // upload sequentially; each file reflects its true result
            for (pf in session.files.values) {
                val vf = pf.vaultFile ?: continue
                val input = MediaFileHandler.getStream(vf)
                pf.status = P2PFileStatus.SENDING
                postProgress()

                try {
                    if (input != null) {
                        when (
                            peerClient.uploadFileWithProgress(
                                ip = ip,
                                port = port,
                                expectedFingerprint = fingerprint,
                                sessionId = session.sessionId.orEmpty(),
                                fileId = pf.file.id,
                                transmissionId = pf.transmissionId.orEmpty(),
                                inputStream = input,
                                fileSize = vf.size,
                                fileName = vf.name,
                            ) { written, _ ->
                                pf.bytesTransferred = written.toInt()
                                postProgress()
                            }
                        ) {
                            PeerUploadOutcome.Success ->
                                pf.status = P2PFileStatus.FINISHED
                            PeerUploadOutcome.Failed ->
                                pf.status = P2PFileStatus.FAILED
                            PeerUploadOutcome.TooManyRequests -> {
                                pf.status = P2PFileStatus.FAILED
                                postProgress()
                                return@launch
                            }
                        }
                    } else {
                        pf.status = P2PFileStatus.FAILED
                    }
                } catch (e: Exception) {
                    pf.status = P2PFileStatus.FAILED
                    Timber.e(e, "Upload failed for ${pf.file.fileName}")
                } finally {
                    input?.close()
                }
                postProgress()
            }

            // session is done (partial or full)
            session.status = SessionStatus.FINISHED
            _uploadProgress.postValue(
                UploadProgressState(
                    title = session.title.orEmpty(),
                    percent = 100,
                    sessionStatus = session.status,
                    files = session.files.values.toList()
                )
            )
        }
    }

    private fun getVaultFilesFromState(): List<VaultFile> {
        return p2PSharedState.session?.files
            ?.values
            ?.mapNotNull { it.vaultFile }
            .orEmpty()
    }

    private fun getTitleFromState(): String {
        return p2PSharedState.session?.title ?: ""
    }

    private fun getSessionId(): String {
        return p2PSharedState.session?.sessionId ?: ""
    }

    override fun onCleared() {
        super.onCleared()
        p2PSharedState.clear()
    }

    fun closePeerConnection() {
        viewModelScope.launch {
            val ip = p2PSharedState.ip
            val port = p2PSharedState.port
            val fingerprint = p2PSharedState.hash
            val success = peerClient.closeConnection(
                ip = ip,
                port = port,
                expectedFingerprint = fingerprint,
                sessionId = p2PSharedState.session?.sessionId ?: ""
            )
            if (!success) Timber.e("Failed to close peer connection.")
        }
    }

}

enum class PrepareFailureKind {
    /** HTTP 403 — recipient rejected; return to prepare with a specific toast. */
    RECIPIENT_REJECTED,

    /** Other failures (e.g. 429, 5xx, network) — exit flow and show generic error. */
    GENERIC
}


