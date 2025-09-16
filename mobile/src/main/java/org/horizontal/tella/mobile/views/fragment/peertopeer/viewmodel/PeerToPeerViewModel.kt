package org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzontal.tella_vault.VaultFile
import com.hzontal.utils.MediaFile.isImageFileType
import com.hzontal.utils.MediaFile.isVideoFileType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.peertopeer.FingerprintFetcher
import org.horizontal.tella.mobile.data.peertopeer.FingerprintResult
import org.horizontal.tella.mobile.data.peertopeer.ServerPinger
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState.Companion.createNewSession
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
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.BottomSheetProgressState
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.UploadProgressState
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class PeerToPeerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerClient: TellaPeerToPeerClient,
    peerToPeerManager: PeerToPeerManager,
    val p2PState: P2PSharedState
) : ViewModel() {

    // ------------------- Public state / deps -------------------
    var peerToPeerParticipant: PeerToPeerParticipant = PeerToPeerParticipant.SENDER
    var isManualConnection: Boolean = true
    var hasNavigatedToSuccessFragment = false
    var currentNetworkInfo: NetworkInfo? = null

    val clientHash = peerToPeerManager.clientConnected
    private val networkInfoManager = NetworkInfoManager(context)
    val networkInfo: LiveData<NetworkInfo> get() = networkInfoManager.networkInfo

    // ------------------- Events to the UI -------------------
    private val _registrationSuccess = SingleLiveEvent<Boolean>()
    val registrationSuccess: SingleLiveEvent<Boolean> get() = _registrationSuccess

    private val _registrationServerSuccess = SingleLiveEvent<Boolean>()
    val registrationServerSuccess: SingleLiveEvent<Boolean> get() = _registrationServerSuccess

    private val _getHashSuccess = SingleLiveEvent<String>()
    val getHashSuccess: SingleLiveEvent<String> get() = _getHashSuccess

    val bottomMessageError = SingleLiveEvent<String>()
    val bottomSheetError = SingleLiveEvent<Pair<String, String>>()

    private val _incomingPrepareRequest = MutableSharedFlow<PrepareUploadRequest>(replay = 1, extraBufferCapacity = 1)
    val incomingPrepareRequest: SharedFlow<PrepareUploadRequest> = _incomingPrepareRequest.asSharedFlow()

    private val _incomingRequest = MutableStateFlow<IncomingRegistration?>(null)
    val incomingRequest: StateFlow<IncomingRegistration?> get() = _incomingRequest

    private val _uploadProgress = SingleLiveEvent<UploadProgressState?>()
    val uploadProgress: SingleLiveEvent<UploadProgressState?> get() = _uploadProgress

    private val _bottomSheetProgress = MutableLiveData<BottomSheetProgressState>()
    val bottomSheetProgress: LiveData<BottomSheetProgressState> get() = _bottomSheetProgress

    private val _closeConnection = SingleLiveEvent<Boolean>()
    val closeConnection: SingleLiveEvent<Boolean> get() = _closeConnection

    // ------------------- Manual verify UI flags -------------------
    private val _canTapConfirm = MutableLiveData(false)
    val canTapConfirm: LiveData<Boolean> get() = _canTapConfirm

    private val _waitingForOtherSide = MutableLiveData(false)
    val waitingForOtherSide: LiveData<Boolean> get() = _waitingForOtherSide

    // Cache for "pre-accept" when recipient taps before the request arrives
    private var preConfirmRegistration: Boolean = false

    // Keep connection params until user taps Confirm (sender path)
    private data class PendingConnectParams(
        val ip: String,
        val port: String,
        val hash: String,
        val pin: String
    )
    private var pendingParams: PendingConnectParams? = null

    // ------------------- Save counters -------------------
    private val savingOrDone: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap())
    private var totalFilesExpected = 0
    private var savedCount = 0
    private var targetFolderId: String? = null

    // ------------------- Init: subscribe to streams -------------------
    init {
        observePrepareUploadEvents()
        observeRegistrationEvents()
        observeRegistrationRequests()
        observeUploadProgress()
        observeCloseConnectionEvents()
    }

    // ------------------- Observers -------------------
    private fun observePrepareUploadEvents() {
        viewModelScope.launch {
            PeerEventManager.prepareUploadRequests.collect { request ->
                _incomingPrepareRequest.tryEmit(request)   // your _incomingPrepareRequest already has replay = 1
            }
        }
    }

    private fun observeRegistrationEvents() {
        viewModelScope.launch {
            PeerEventManager.registrationEvents.collect { success ->
                _registrationServerSuccess.postValue(success)
                if (success) {
                    _waitingForOtherSide.postValue(false)
                    _canTapConfirm.postValue(false) // will navigate away
                }
            }
        }
    }

    private fun observeCloseConnectionEvents() {
        viewModelScope.launch {
            PeerEventManager.closeConnectionEvent.collect { success ->
                if (success) {
                    p2PState.session?.status = SessionStatus.CLOSED
                    emitFinalIfReady(SessionStatus.CLOSED)
                }
            }
        }
    }

    private fun observeRegistrationRequests() {
        viewModelScope.launch {
            PeerEventManager.registrationRequests.collect { (registrationId, payload) ->
                if (registrationId.isEmpty()) return@collect

                _incomingRequest.value = IncomingRegistration(registrationId, payload)

                if (!p2PState.isUsingManualConnection) {
                    // Auto mode: accept immediately
                    PeerEventManager.confirmRegistration(registrationId, true)
                    _registrationSuccess.postValue(true)
                    PeerEventManager.clearRegistrationRequest()
                    return@collect
                }

                // Manual mode: if the recipient tapped confirm earlier, accept now.
                if (preConfirmRegistration) {
                    PeerEventManager.confirmRegistration(registrationId, true)
                    PeerEventManager.clearRegistrationRequest()
                    preConfirmRegistration = false
                } else {
                    // Otherwise, allow tapping now (if UI wants to reflect it)
                    _canTapConfirm.postValue(true)
                }
            }
        }
    }

    private fun observeUploadProgress() {
        viewModelScope.launch {
            PeerEventManager.uploadProgressStateFlow.collect { state ->
                initCountersIfNeeded()
                _uploadProgress.postValue(state)

                // Save each FINISHED file exactly once (by transmissionId)
                state.files.forEach { pf ->
                    val txId = pf.transmissionId
                    if (txId != null &&
                        pf.status == P2PFileStatus.FINISHED &&
                        !savingOrDone.contains(txId)
                    ) {
                        viewModelScope.launch(Dispatchers.IO) { saveOneFile(pf) }
                    }
                }

                emitFinalIfReady(state.sessionStatus)
            }
        }
    }

    // ------------------- Manual verification entry points -------------------

    /**
     * Called after IP/port/PIN are entered and TLS cert is fetched.
     * In manual mode we DO NOT auto-register. We enable the "Confirm & connect" button instead.
     */
    fun handleCertificate(ip: String, port: String, pin: String) {
        viewModelScope.launch {
            val reachable = runCatching { peerClient.pingBeforeRegister(ip, port) }.getOrDefault(false)
            if (!reachable) {
                bottomSheetError.postValue(
                    "Connection failed" to "Host not reachable on this Wi-Fi. Check IP/Port and that both devices are on the same network."
                )
                return@launch
            }

            val fpRes: Result<FingerprintResult> = FingerprintFetcher.fetch(context, ip, port.toInt())
            if (fpRes.isFailure) {
                bottomSheetError.postValue(
                    "Connection failed" to ("Couldn’t read peer certificate. " + (fpRes.exceptionOrNull()?.message ?: ""))
                )
                return@launch
            }

            val fp = fpRes.getOrNull()!!
            p2PState.hash = fp.certHex
            _getHashSuccess.postValue(fp.certHex)

            val pinnedPingOk = runCatching {
                ServerPinger.notifyServerPinnedByCert(
                    context = context,
                    ip = ip,
                    port = port.toInt(),
                    expectedCertSha256Hex = fp.certHex
                )
            }.isSuccess

            // Manual verification path: wait for user tap
            p2PState.isUsingManualConnection = true
            pendingParams = PendingConnectParams(ip, port, fp.certHex, pin)
            _canTapConfirm.postValue(true)
            _waitingForOtherSide.postValue(false)
        }
    }

    /** Sender tapped confirm: actually initiate /register on peer (using cached params). */
    // In PeerToPeerViewModel
    fun onUserTappedConfirmAndConnect() {
        _canTapConfirm.postValue(false)
        _waitingForOtherSide.postValue(true)

        val params = pendingParams
        if (params != null) {
            startRegistration(params.ip, params.port, params.hash, params.pin)
            return
        }

        // Fallback: try using current state or re-run handshake
        val ip = p2PState.ip
        val port = p2PState.port
        val pin = p2PState.pin.orEmpty()
        val hash = p2PState.hash

        if (hash.isNotBlank()) {
            startRegistration(ip, port, hash, pin)
        } else {
            viewModelScope.launch {
                handleCertificate(ip, port, pin)
                pendingParams?.let { startRegistration(it.ip, it.port, it.hash, it.pin) }
                    ?: run {
                        _waitingForOtherSide.postValue(false)
                        _canTapConfirm.postValue(true)
                    }
            }
        }
    }



    /** Recipient tapped confirm: allow pre-accept before request arrives. */
    fun onRecipientConfirmTapped() {
        _canTapConfirm.postValue(false)
        _waitingForOtherSide.postValue(true) // "Waiting for the sender…"

        val current = _incomingRequest.value
        if (current != null) {
            onUserConfirmedRegistration(current.registrationId)
        } else {
            preConfirmRegistration = true
        }
    }

    /** Recipient send acceptance to server. Do NOT post local registrationSuccess here. */
    fun onUserConfirmedRegistration(registrationId: String) {
        viewModelScope.launch {
            PeerEventManager.confirmRegistration(registrationId, true)
            PeerEventManager.clearRegistrationRequest()
        }
    }

    fun onUserRejectedRegistration(registrationId: String) {
        viewModelScope.launch {
            PeerEventManager.confirmRegistration(registrationId, false)
            PeerEventManager.clearRegistrationRequest()
        }
    }

    /** Sender/initiator path: call server /register */
    fun startRegistration(ip: String, port: String, hash: String, pin: String) {
        viewModelScope.launch {
            when (val result = peerClient.registerPeerDevice(ip, port, hash, pin)) {
                is RegisterPeerResult.Success -> {
                    if (p2PState.session == null) p2PState.session = P2PSharedState.createNewSession()
                    p2PState.session?.sessionId = result.sessionId
                    _registrationSuccess.postValue(true) // Used by sender UI
                }
                RegisterPeerResult.InvalidPin -> bottomMessageError.postValue("Invalid PIN")
                RegisterPeerResult.InvalidFormat -> bottomMessageError.postValue("Invalid request format")
                RegisterPeerResult.Conflict -> bottomMessageError.postValue("Active session already exists")
                RegisterPeerResult.TooManyRequests -> bottomMessageError.postValue("Too many requests, try again later")
                RegisterPeerResult.ServerError -> bottomMessageError.postValue("Server error, try again later")
                RegisterPeerResult.RejectedByReceiver -> bottomMessageError.postValue("Receiver rejected the registration")
                is RegisterPeerResult.Failure -> {
                    Timber.e(result.exception, "Connection failure")
                    bottomSheetError.postValue(
                        "Connection failed" to "Please make sure your connection details are correct and that you are on the same Wi-Fi network."
                    )
                }
            }
        }
    }

    // ------------------- Prepare/Upload/Save logic (unchanged from your version) -------------------

    private fun initCountersIfNeeded() {
        val session = p2PState.session ?: return
        if (totalFilesExpected == 0) {
            totalFilesExpected = session.files.size
            savedCount = session.files.values.count { it.status == P2PFileStatus.SAVED }
            session.files.values.forEach { pf ->
                val tx = pf.transmissionId
                if (!tx.isNullOrBlank() && (pf.status == P2PFileStatus.SAVED || pf.status == P2PFileStatus.FAILED)) {
                    savingOrDone.add(tx)
                }
            }
            postBottomSheetProgress()
        }
    }

    private fun maybeFinalizeAfterSave() {
        val session = p2PState.session ?: return
        if (!allFilesSavedOrFailed()) return

        val final = when (session.status) {
            SessionStatus.CLOSED -> SessionStatus.CLOSED
            SessionStatus.SENDING, SessionStatus.SAVING -> computeFinalStatus()
            SessionStatus.FINISHED, SessionStatus.FINISHED_WITH_ERRORS -> session.status
            else -> computeFinalStatus()
        }

        session.status = final

        _uploadProgress.postValue(
            UploadProgressState(
                title = session.title.orEmpty(),
                sessionStatus = final,
                files = session.files.values.toList(),
                percent = 100
            )
        )
    }

    private fun obtainTargetFolderId(): String {
        targetFolderId?.let { return it }
        val title = (p2PState.session?.title ?: "").trim()
        val finalTitle = if (title.isEmpty()) "Transfer" else title

        val vault = MyApplication.keyRxVault.rxVault.blockingFirst()
        val root = vault.root.blockingGet()
        val folder = vault.builder()
            .setName(finalTitle)
            .setType(VaultFile.Type.DIRECTORY)
            .build(root.id)
            .blockingGet()
        targetFolderId = folder.id
        return folder.id
    }

    private fun saveOneFile(pf: ProgressFile) {
        val txId = pf.transmissionId ?: return
        if (!savingOrDone.add(txId)) return

        if (p2PState.session?.status == SessionStatus.SENDING) {
            p2PState.session?.status = SessionStatus.SAVING
            _uploadProgress.postValue(
                UploadProgressState(
                    title = p2PState.session?.title.orEmpty(),
                    sessionStatus = SessionStatus.SAVING,
                    files = p2PState.session?.files?.values?.toList().orEmpty(),
                    percent = 100
                )
            )
        }

        try {
            val path = pf.path ?: return
            val f = File(path)
            if (!f.exists()) return

            val folderId = obtainTargetFolderId()
            val vault = MyApplication.keyRxVault.rxVault.blockingFirst()

            val vaultFile = try {
                when {
                    isImageFileType(pf.file.fileType) -> {
                        val bytes = f.readBytes()
                        if (pf.file.fileType.contains("png", true)) {
                            MediaFileHandler.savePngImage(bytes)
                        } else {
                            MediaFileHandler.saveJpegPhoto(bytes, folderId).blockingGet()
                        }
                    }
                    isVideoFileType(pf.file.fileType) -> {
                        MediaFileHandler.saveMp4Video(f, folderId)
                    }
                    else -> {
                        vault.builder(f.inputStream())
                            .setName(f.name)
                            .setMimeType(pf.file.fileType)
                            .setType(VaultFile.Type.FILE)
                            .build(folderId)
                            .blockingGet()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save file ${pf.file.fileName}")
                null
            }

            val txKey = pf.transmissionId
            if (txKey != null) {
                p2PState.session?.files?.get(txKey)?.status = P2PFileStatus.SAVED
            }
            pf.status = P2PFileStatus.SAVED
            pf.vaultFile = vaultFile
            savedCount++
            f.delete()
        } catch (e: Exception) {
            Timber.e(e, "Saving to vault failed for file ${pf.file.fileName}")
            pf.status = P2PFileStatus.FAILED
        } finally {
            postBottomSheetProgress()
            maybeFinalizeAfterSave()
        }
    }

    private fun postBottomSheetProgress() {
        _bottomSheetProgress.postValue(
            BottomSheetProgressState(
                current = savedCount,
                total = totalFilesExpected,
                percent = if (totalFilesExpected > 0) (savedCount * 100 / totalFilesExpected) else 0
            )
        )
    }

    private fun sessionIsTerminal(sessionStatus: SessionStatus): Boolean =
        sessionStatus == SessionStatus.FINISHED ||
                sessionStatus == SessionStatus.FINISHED_WITH_ERRORS ||
                sessionStatus == SessionStatus.CLOSED

    private fun allFilesSavedOrFailed(): Boolean =
        p2PState.session?.files?.values?.all {
            it.status == P2PFileStatus.SAVED || it.status == P2PFileStatus.FAILED
        } == true

    private fun computeFinalStatus(): SessionStatus {
        val files = p2PState.session?.files?.values.orEmpty()
        val anyFailed = files.any { it.status == P2PFileStatus.FAILED }
        return if (anyFailed) SessionStatus.FINISHED_WITH_ERRORS else SessionStatus.FINISHED
    }

    private fun emitFinalIfReady(triggerStatus: SessionStatus) {
        val session = p2PState.session ?: return
        if (!sessionIsTerminal(triggerStatus)) return
        if (!allFilesSavedOrFailed()) return

        val final = when (session.status) {
            SessionStatus.CLOSED -> SessionStatus.CLOSED
            else -> computeFinalStatus()
        }
        session.status = final

        _uploadProgress.postValue(
            UploadProgressState(
                title = session.title.orEmpty(),
                sessionStatus = final,
                files = session.files.values.toList(),
                percent = 100
            )
        )
    }

    // ------------------- Misc -------------------
    fun confirmPrepareUpload(sessionId: String, accepted: Boolean) {
        PeerEventManager.resolveUserDecision(sessionId, accepted)
    }

    fun clearPrepareRequest() {
        hasNavigatedToSuccessFragment = false
    }

    fun resetRegistrationState() {
        _registrationServerSuccess.postValue(false)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun updateNetworkInfo() {
        networkInfoManager.fetchCurrentNetworkInfo()
    }

    fun closePeerConnection() {
        viewModelScope.launch {
            val ip = p2PState.ip
            val port = p2PState.port
            val fingerprint = p2PState.hash
            val success = peerClient.closeConnection(
                ip = ip,
                port = port,
                expectedFingerprint = fingerprint,
                sessionId = p2PState.session?.sessionId ?: ""
            )
            if (!success) Timber.e("Failed to close peer connection.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        p2PState.clear()
    }
}
