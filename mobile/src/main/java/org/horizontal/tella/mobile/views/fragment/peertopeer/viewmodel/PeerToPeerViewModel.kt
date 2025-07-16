package org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile.isAudioFileType
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.peertopeer.FingerprintFetcher
import org.horizontal.tella.mobile.data.peertopeer.ServerPinger
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.model.ProgressFile
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.data.peertopeer.remote.RegisterPeerResult
import org.horizontal.tella.mobile.domain.peertopeer.IncomingRegistration
import org.horizontal.tella.mobile.domain.peertopeer.PeerEventManager
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.util.NetworkInfo
import org.horizontal.tella.mobile.util.NetworkInfoManager
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PeerToPeerParticipant
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.UploadProgressState
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PeerToPeerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerClient: TellaPeerToPeerClient,
    peerToPeerManager: PeerToPeerManager,
    val p2PState: P2PSharedState
) : ViewModel() {
    var peerToPeerParticipant: PeerToPeerParticipant = PeerToPeerParticipant.SENDER
    var isManualConnection: Boolean = true
    var hasNavigatedToSuccessFragment = false
    var currentNetworkInfo: NetworkInfo? = null
    private val _registrationSuccess = SingleLiveEvent<Boolean>()
    val registrationSuccess: LiveData<Boolean> get() = _registrationSuccess
    private val _getHashSuccess = SingleLiveEvent<String>()
    val getHashSuccess: LiveData<String> get() = _getHashSuccess
    val bottomMessageError = SingleLiveEvent<String>()
    val bottomSheetError = SingleLiveEvent<Pair<String, String>>()
    val clientHash = peerToPeerManager.clientConnected
    private val _registrationServerSuccess = SingleLiveEvent<Boolean>()
    val registrationServerSuccess: LiveData<Boolean> = _registrationServerSuccess
    private val _incomingPrepareRequest = SingleLiveEvent<PrepareUploadRequest?>()
    val incomingPrepareRequest: SingleLiveEvent<PrepareUploadRequest?> = _incomingPrepareRequest
    private val _incomingRequest = MutableStateFlow<IncomingRegistration?>(null)
    val incomingRequest: StateFlow<IncomingRegistration?> = _incomingRequest
    private val networkInfoManager = NetworkInfoManager(context)
    val networkInfo: LiveData<NetworkInfo> get() = networkInfoManager.networkInfo
    private val _uploadProgress = SingleLiveEvent<UploadProgressState?>()
    val uploadProgress: SingleLiveEvent<UploadProgressState?> = _uploadProgress
    private var isVaultSaveDone = false


    init {
        viewModelScope.launch {
            PeerEventManager.prepareUploadEvents.collect { request ->
                _incomingPrepareRequest.postValue(request)
            }
        }

        viewModelScope.launch {
            PeerEventManager.registrationEvents.collect { success ->
                _registrationServerSuccess.postValue(success)
            }
        }

        viewModelScope.launch {
            PeerEventManager.registrationRequests.collect { (registrationId, payload) ->
                _incomingRequest.value = IncomingRegistration(registrationId, payload)

                if (!p2PState.isUsingManualConnection) {
                    PeerEventManager.confirmRegistration(registrationId, true)
                    _registrationSuccess.postValue(true)
                }
            }
        }

        viewModelScope.launch {
            PeerEventManager.uploadProgressStateFlow.collect { state ->

                val allFinished = state.files.all { it.status == P2PFileStatus.FINISHED }

                if (allFinished && !isVaultSaveDone) {
                    isVaultSaveDone = true // prevent re-saving
                    finalizeAndSaveReceivedFiles(p2PState.session?.title.orEmpty(), state.files)
                }
            }
        }

    }

    fun resetRegistrationState() {
        _registrationServerSuccess.postValue(false)
    }

    fun confirmPrepareUpload(sessionId: String, accepted: Boolean) {
        PeerEventManager.resolveUserDecision(sessionId, accepted)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun updateNetworkInfo() {
        networkInfoManager.fetchCurrentNetworkInfo()
    }

    fun startRegistration(
        ip: String,
        port: String,
        hash: String,
        pin: String
    ) {
        viewModelScope.launch {
            when (val result = peerClient.registerPeerDevice(ip, port, hash, pin)) {
                is RegisterPeerResult.Success -> {
                    with(p2PState) {
                        this.session?.sessionId = result.sessionId
                    }
                    Timber.d("session id ***startRegistration ${p2PState.session?.sessionId}")

                    _registrationSuccess.postValue(true)
                }

                RegisterPeerResult.InvalidPin -> {
                    bottomMessageError.postValue("Invalid PIN")
                }

                RegisterPeerResult.InvalidFormat -> {
                    bottomMessageError.postValue("Invalid request format")
                }

                RegisterPeerResult.Conflict -> {
                    bottomMessageError.postValue("Active session already exists")
                }

                RegisterPeerResult.TooManyRequests -> {
                    bottomMessageError.postValue("Too many requests, try again later")
                }

                RegisterPeerResult.ServerError -> {
                    bottomMessageError.postValue("Server error, try again later")
                }

                RegisterPeerResult.RejectedByReceiver -> {
                    bottomMessageError.postValue("Receiver rejected the registration")
                }

                is RegisterPeerResult.Failure -> {
                    Timber.e(result.exception, "Connection failure")
                    bottomSheetError.postValue(
                        "Connection failed" to
                                "Please make sure your connection details are correct and that you are on the same Wi-Fi network."
                    )
                }
            }
        }
    }


    fun handleCertificate(ip: String, port: String, pin: String) {
        viewModelScope.launch {
            val result = FingerprintFetcher.fetch(ip, port.toInt())
            result.onSuccess { hash ->
                Timber.d("hash ***** $hash")
                p2PState.hash = hash
                _getHashSuccess.postValue(hash)

                // Notify the server after fetching the hash
                runCatching {
                    ServerPinger.notifyServer(ip, port.toInt())
                }.onFailure {
                    Timber.e(it, "Failed to ping server after fetching hash")
                }

            }.onFailure { error ->
                Timber.d("error ***** $error")
                bottomSheetError.postValue(
                    "Connection failed" to
                            "Please make sure your connection details are correct and that you are on the same Wi-Fi network."
                )
            }
        }
    }

    fun onUserConfirmedRegistration(registrationId: String) {
        viewModelScope.launch {
            PeerEventManager.confirmRegistration(registrationId, true)
            _registrationSuccess.postValue(true)
        }
    }

    fun onUserRejectedRegistration(registrationId: String) {
        PeerEventManager.confirmRegistration(registrationId, accepted = false)
    }

    fun clearPrepareRequest() {
        _incomingPrepareRequest.value = null
        hasNavigatedToSuccessFragment = false
    }

    private fun finalizeAndSaveReceivedFiles(
        folderName: String,
        progressFiles: List<ProgressFile>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val vault = MyApplication.keyRxVault.rxVault.blockingFirst()
            val root = vault.root.blockingGet()

            val folder = vault.builder()
                .setName(folderName)
                .setType(VaultFile.Type.DIRECTORY)
                .build(root.id)
                .blockingGet()

            val totalFiles = progressFiles.size
            var savedFiles = 0

            progressFiles.forEach { progressFile ->
                try {
                    val path = progressFile.path ?: return@forEach
                    val file = File(path)
                    if (!file.exists()) return@forEach

                    val vaultFile = try {
                        when {
                            isImageFileType(progressFile.file.fileType) -> {
                                val imageBytes = file.readBytes()
                                if (progressFile.file.fileType.contains("png", ignoreCase = true)) {
                                    MediaFileHandler.savePngImage(imageBytes)
                                } else {
                                    MediaFileHandler.saveJpegPhoto(imageBytes, folder.id)
                                        .blockingGet()
                                }
                            }

                            isVideoFileType(progressFile.file.fileType) -> {
                                MediaFileHandler.saveMp4Video(file, folder.id)
                            }

                            else -> {
                                vault.builder(file.inputStream())
                                    .setName(file.name)
                                    .setMimeType(progressFile.file.fileType)
                                    .setType(VaultFile.Type.FILE)
                                    .build(folder.id)
                                    .blockingGet()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to save file ${progressFile.file.fileName}")
                        null
                    }


                    p2PState.session?.files?.get(progressFile.file.id)?.status =  P2PFileStatus.SAVED
                    progressFile.vaultFile = vaultFile
                    savedFiles++

                    // Delete temp file after saving
                    file.delete()

                    // Emit progress after each file
                    _uploadProgress.postValue(
                        UploadProgressState(
                            title = folderName,
                            percent = ((savedFiles * 100) / totalFiles),
                            sessionStatus = SessionStatus.SENDING,
                            files = progressFiles
                        )
                    )

                } catch (e: Exception) {
                    Timber.e(e, "Saving to vault failed for file ${progressFile.file.fileName}")
                    progressFile.status = P2PFileStatus.FAILED
                }
            }

            p2PState.session?.status = SessionStatus.FINISHED

            _uploadProgress.postValue(
                UploadProgressState(
                    title = folderName,
                    percent = 100,
                    sessionStatus = SessionStatus.FINISHED,
                    files = progressFiles
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        p2PState.clear()
    }

}