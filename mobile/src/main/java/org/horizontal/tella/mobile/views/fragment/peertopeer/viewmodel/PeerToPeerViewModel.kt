package org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.rx.RxVault
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
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.PeerKeyProvider
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.data.peertopeer.model.P2PVerificationStep
import org.horizontal.tella.mobile.domain.peertopeer.ParsedReceiverQr
import org.horizontal.tella.mobile.domain.peertopeer.PeerEventManager
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState.Companion.createNewSession
import org.horizontal.tella.mobile.data.peertopeer.model.ProgressFile
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.data.peertopeer.remote.PeerManualPingSession
import org.horizontal.tella.mobile.data.peertopeer.remote.RegisterPeerResult
import org.horizontal.tella.mobile.domain.peertopeer.IncomingRegistration
import org.horizontal.tella.mobile.domain.peertopeer.NearbySharingIpPreference
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.util.NetworkInfo
import org.horizontal.tella.mobile.util.NetworkInfoManager
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PeerToPeerParticipant
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.BottomSheetProgressState
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.UploadProgressState
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.Collections
import java.util.Locale
import java.util.UUID
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
    var currentNetworkInfo: NetworkInfo? = null

    private var hasNavigatedFromWaitingToPrepareSuccess: Boolean = false

    fun markNavigatedFromWaitingToPrepareSuccess() {
        hasNavigatedFromWaitingToPrepareSuccess = true
    }

    fun clearWaitingToPrepareSuccessNavigationBlock() {
        hasNavigatedFromWaitingToPrepareSuccess = false
    }

    fun discardStalePrepareOfferReplayAndNavigationGate() {
        _incomingPrepareRequest.resetReplayCache()
        clearWaitingToPrepareSuccessNavigationBlock()
    }

    fun shouldSkipWaitingToPrepareSuccessNavigation(): Boolean =
        hasNavigatedFromWaitingToPrepareSuccess

    val clientHash = peerToPeerManager.clientConnected
    val recipientHashVerification = peerToPeerManager.recipientHashVerification
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

    private val _incomingPrepareRequest =
        MutableSharedFlow<PrepareUploadRequest>(replay = 1, extraBufferCapacity = 1)
    val incomingPrepareRequest: SharedFlow<PrepareUploadRequest> =
        _incomingPrepareRequest.asSharedFlow()

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

    private val _navigateToSenderVerification = SingleLiveEvent<Boolean>()
    val navigateToSenderVerification: SingleLiveEvent<Boolean> get() = _navigateToSenderVerification

    val incompatibleProtocolError = SingleLiveEvent<Unit>()

    private val _isRegistering = MutableLiveData(false)
    val isRegistering: LiveData<Boolean> get() = _isRegistering

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

    private var manualConnectionPinInvalid = false
    private var pinResetRequired = false

    /** Live manual ping : receiver hash from TLS now, senderShowHash on confirm. */
    private var manualPingSession: PeerManualPingSession? = null

    /** Reuse registration nonce for the same target until registration succeeds  */
    private data class RegistrationNonceContext(
        val ip: String,
        val port: String,
        val pin: String,
        val nonce: String,
    ) {
        fun matches(ip: String, port: String, pin: String) =
            this.ip == ip && this.port == port && this.pin == pin
    }

    private var registrationNonceContext: RegistrationNonceContext? = null

    private fun registrationNonceFor(ip: String, port: String, pin: String): String {
        val normalizedPin = pin.trim()
        val existing = registrationNonceContext
        if (existing != null && existing.matches(ip, port, normalizedPin)) {
            return existing.nonce
        }
        val nonce = UUID.randomUUID().toString()
        registrationNonceContext = RegistrationNonceContext(ip, port, normalizedPin, nonce)
        return nonce
    }

    // ------------------- Save counters -------------------
    private val savingOrDone: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap())
    private var totalFilesExpected = 0
    private var savedCount = 0
    private var targetFolderId: String? = null

    fun getTransferFolderId(): String? = targetFolderId

    private val p2pVaultImportLock = Any()

    // ------------------- Init: subscribe to streams -------------------
    init {
        observePrepareUploadEvents()
        observeRegistrationEvents()
        observeRegistrationRequests()
        observeUploadProgress()
        observeCloseConnectionEvents()
        observeSenderHashVerification()
        observeIncompatibleProtocol()
    }

    fun resetConnectionState() {
        PeerKeyProvider.reset()
        p2PState.clear()
        isManualConnection = true
        registrationNonceContext = null
        pendingParams = null
        manualPingSession?.cancel()
        manualPingSession = null
        preConfirmRegistration = false
        PeerEventManager.resetReceiverHashConfirmation()
        manualConnectionPinInvalid = false
        pinResetRequired = false
        _waitingForOtherSide.postValue(false)
    }

    fun clearStaleManualConnectionWaitingState() {
        if (manualPingSession == null && _isRegistering.value != true) {
            _waitingForOtherSide.postValue(false)
        }
    }

    fun clearManualConnectionWaitingOnDiscard() {
        _waitingForOtherSide.postValue(false)
        if (manualConnectionPinInvalid) {
            manualConnectionPinInvalid = false
            pinResetRequired = true
            p2PState.pin = null
            pendingParams = null
            registrationNonceContext = null
        }
    }

    fun consumeManualConnectionPinReset(): Boolean {
        if (!pinResetRequired) return false
        pinResetRequired = false
        return true
    }

    fun prepareSenderSession() {
        val (_, cert) = PeerKeyProvider.ensureSenderIdentity()
        p2PState.localSenderHash = CertificateUtils.getLeafCertificateDerSha256Hex(cert)
    }

    fun onSenderQrScanned(senderHash: String) {
        p2PState.pinSenderHash(senderHash)
    }

    fun onReceiverQrScanned(parsed: ParsedReceiverQr) {
        p2PState.pin = parsed.pin
        p2PState.port = parsed.port.toString()
        p2PState.ip = parsed.ipAddresses.firstOrNull().orEmpty()
        p2PState.advertisedIpAddresses = parsed.ipAddresses
        p2PState.senderShowHash = parsed.senderShowHash
        p2PState.pinReceiverHash(parsed.certificateHash)

        if (parsed.senderShowHash) {
            _navigateToSenderVerification.postValue(true)
            return
        }

        startRegistrationWithIpCandidates(
            rawCandidates = parsed.ipAddresses,
            port = parsed.port.toString(),
            hash = parsed.certificateHash,
            pin = parsed.pin,
        )
    }

    fun showIncompatibleProtocolError() {
        incompatibleProtocolError.call()
    }

    private fun observeSenderHashVerification() {
        viewModelScope.launch {
            PeerEventManager.senderHashVerificationRequests.collect { senderHash ->
                p2PState.activeVerificationStep = P2PVerificationStep.SENDER_HASH
                // Pending display until recipient confirms (server pins after confirm).
                p2PState.hash = senderHash
                _getHashSuccess.postValue(senderHash)
                _canTapConfirm.postValue(true)
                _waitingForOtherSide.postValue(false)
            }
        }
    }

    private fun observeIncompatibleProtocol() {
        viewModelScope.launch {
            PeerEventManager.incompatibleProtocol.collect {
                showIncompatibleProtocolError()
            }
        }
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
                    _closeConnection.postValue(true)
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

                if (p2PState.pinnedSenderHash.isNotBlank()) {
                    Timber.d(
                        "P2P registration auto-accepted (pinnedSenderHash set, manual=%b)",
                        p2PState.isUsingManualConnection,
                    )
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
     * Called after IP/port/PIN are entered. →
     * startManualPing): the receiver hash is read from the TLS handshake, so we navigate to the
     * receiver-hash verification screen immediately — before the recipient confirms. The held HTTP
     * body (senderShowHash) is awaited later, when the sender taps "Confirm and continue".
     */
    fun handleCertificate(ip: String, port: String, pin: String) {
        viewModelScope.launch {
            prepareSenderSession()
            p2PState.isUsingManualConnection = true
            p2PState.senderCanScanQr = false
            p2PState.activeVerificationStep = P2PVerificationStep.RECIPIENT_HASH
            // Brief wait while the TLS handshake completes (receiver hash comes from the handshake).
            _waitingForOtherSide.postValue(true)

            manualPingSession?.cancel()
            val session = peerClient.startManualPing(ip, port)
            manualPingSession = session

            val receiverHash = try {
                session.awaitReceiverHash()
            } catch (e: Exception) {
                Timber.w(e, "P2P manual ping: receiver hash failed")
                manualPingSession = null
                _waitingForOtherSide.postValue(false)
                bottomSheetError.postValue(
                    context.getString(R.string.connection_failed) to
                            context.getString(R.string.peer_to_peer_manual_ping_failed)
                )
                return@launch
            }

            Timber.d("P2P manual ping: receiverHash=%s (from handshake)", receiverHash)
            p2PState.hash = receiverHash
            pendingParams = PendingConnectParams(ip, port, receiverHash, pin)
            _getHashSuccess.postValue(receiverHash)   // navigate to Step 1 (recipient hash)
            _canTapConfirm.postValue(true)            // enable "Confirm and continue"
            _waitingForOtherSide.postValue(false)
        }
    }

    /**
     * Sender tapped confirm.
     * - Step 2 (sender hash) is passive — just wait for the recipient.
     * - Step 1 (recipient hash): pin the receiver hash, then await the held ping body for
     *   `senderShowHash` (released once the recipient confirms), then /register.
     */
    fun onUserTappedConfirmAndConnect() {
        _canTapConfirm.postValue(false)
        _waitingForOtherSide.postValue(true)

        if (p2PState.activeVerificationStep == P2PVerificationStep.SENDER_HASH) {
            return
        }

        val params = pendingParams
        val session = manualPingSession
        if (params != null && session != null) {
            viewModelScope.launch {
                p2PState.pinReceiverHash(params.hash)
                val senderShowHash = try {
                    session.awaitSenderShowHash()
                } catch (e: Exception) {
                    Timber.w(e, "P2P manual ping: senderShowHash failed")
                    manualPingSession = null
                    _waitingForOtherSide.postValue(false)
                    bottomSheetError.postValue(
                        context.getString(R.string.connection_failed) to
                                context.getString(R.string.peer_to_peer_manual_ping_failed)
                    )
                    return@launch
                }
                manualPingSession = null
                p2PState.senderShowHash = senderShowHash
                Timber.d(
                    "P2P manual confirm: receiverHash=%s senderShowHash=%b",
                    params.hash, senderShowHash,
                )
                startRegistration(params.ip, params.port, params.hash, params.pin)
                if (senderShowHash) showSenderHashAfterRegister()
            }
            return
        }

        // Fallback: params cached but no live ping session — register directly.
        if (params != null) {
            p2PState.pinReceiverHash(params.hash)
            startRegistration(params.ip, params.port, params.hash, params.pin)
            if (p2PState.senderShowHash) showSenderHashAfterRegister()
            return
        }

        val ip = p2PState.ip
        val port = p2PState.port
        val pin = p2PState.pin.orEmpty()
        val hash = p2PState.hash
        if (hash.isNotBlank()) {
            startRegistration(ip, port, hash, pin)
            if (p2PState.senderShowHash) showSenderHashAfterRegister()
        } else {
            handleCertificate(ip, port, pin)
        }
    }

    /**
     * After the manual sender sends /register, show this device's own hash on step 2 so the recipient
     * can cross-check it. The sender takes no action on this step — they wait for the recipient to
     * confirm. Only invoked when the ping reported `senderShowHash == true` (flow D); in flow C the
     * receiver already pinned the sender via QR, so this screen is skipped entirely.
     */
    private fun showSenderHashAfterRegister() {
        p2PState.activeVerificationStep = P2PVerificationStep.SENDER_HASH
        _getHashSuccess.postValue(p2PState.localSenderHash)
        _waitingForOtherSide.postValue(true)
        _canTapConfirm.postValue(false)
    }


    /** Recipient tapped confirm: allow pre-accept before request arrives. */
    fun onRecipientConfirmTapped() {
        _canTapConfirm.postValue(false)
        _waitingForOtherSide.postValue(true)

        if (p2PState.activeVerificationStep == P2PVerificationStep.SENDER_HASH) {
            p2PState.senderHashConfirmed = true
            preConfirmRegistration = true
            PeerEventManager.confirmSenderHashVerification(true)
            return
        }

        if (p2PState.activeVerificationStep == P2PVerificationStep.RECIPIENT_HASH ||
            p2PState.isUsingManualConnection
        ) {
            // Flow D step 1 — unblocks /register held on the server.
            p2PState.receiverHashConfirmed = true
            PeerEventManager.confirmReceiverHashVerification(true)
            preConfirmRegistration = true
            return
        }

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

    fun collectLocalIpv4AddressesForNearbySharing(): List<String> =
        networkInfoManager.collectNearbySharingIpv4Addresses()

    /** Sender/initiator path: single IP (manual entry or legacy QR). */
    fun startRegistration(ip: String, port: String, hash: String, pin: String) {
        startRegistrationWithIpCandidates(listOf(ip), port, hash, pin)
    }

    /**
     * Tries `/register` for each candidate IP in order. Candidates are reordered so addresses on the same
     * IPv4 /24-style subnet as this device are tried first, then the rest.
     */
    fun startRegistrationWithIpCandidates(
        rawCandidates: List<String>,
        port: String,
        hash: String,
        pin: String
    ) {
        val distinct = rawCandidates.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinct.isEmpty()) return
        val candidates = NearbySharingIpPreference.preferredNearbySharingIPOrder(
            qrAddresses = distinct,
            localDeviceIPv4Addresses = networkInfoManager.collectAllLocalIpv4Addresses(),
        )
        val pinTrimmed = pin.trim()
        viewModelScope.launch {
            _isRegistering.postValue(true)
            try {
                candidates.forEachIndexed { index, ip ->
                    val nonce = registrationNonceFor(ip, port, pinTrimmed)
                    when (val result =
                        peerClient.registerPeerDevice(ip, port, hash, pinTrimmed, nonce)) {
                        is RegisterPeerResult.Success -> {
                            registrationNonceContext = null
                            if (p2PState.session == null) p2PState.session =
                                P2PSharedState.createNewSession()
                            p2PState.session?.sessionId = result.sessionId
                            p2PState.ip = ip
                            p2PState.markRegistered()
                            _registrationSuccess.postValue(true)
                            return@launch
                        }

                        RegisterPeerResult.IncompatibleProtocol -> {
                            showIncompatibleProtocolError()
                            return@launch
                        }

                        RegisterPeerResult.InvalidPin -> {
                            if (p2PState.isUsingManualConnection) {
                                manualConnectionPinInvalid = true
                                _waitingForOtherSide.postValue(false)
                                _canTapConfirm.postValue(true)
                            }
                            bottomMessageError.postValue(context.getString(R.string.peer_to_peer_invalid_pin))
                            return@launch
                        }

                        RegisterPeerResult.InvalidFormat,
                        RegisterPeerResult.ClientCertificateRequired -> {
                            bottomMessageError.postValue(context.getString(R.string.peer_to_peer_invalid_request_format))
                            return@launch
                        }

                        RegisterPeerResult.RejectedByReceiver -> {
                            bottomMessageError.postValue(context.getString(R.string.peer_to_peer_receiver_rejected_registration))
                            return@launch
                        }

                        RegisterPeerResult.Conflict -> {
                            if (index < candidates.lastIndex && shouldRetryRegisterWithNextIp(result)) return@forEachIndexed
                            bottomMessageError.postValue(context.getString(R.string.peer_to_peer_active_session_exists))
                            return@launch
                        }

                        RegisterPeerResult.TooManyRequests -> {
                            if (index < candidates.lastIndex && shouldRetryRegisterWithNextIp(result)) return@forEachIndexed
                            bottomSheetError.postValue(
                                "Connection failed" to "Please make sure your connection details are correct and that you are on the same Wi-Fi network."
                            )
                            return@launch
                        }

                        RegisterPeerResult.ServerError -> {
                            if (index < candidates.lastIndex && shouldRetryRegisterWithNextIp(result)) return@forEachIndexed
                            bottomMessageError.postValue(context.getString(R.string.peer_to_peer_server_error_try_later))
                            return@launch
                        }

                        is RegisterPeerResult.Failure -> {
                            if (index < candidates.lastIndex && shouldRetryRegisterWithNextIp(result)) return@forEachIndexed
                            Timber.e(result.exception, "Connection failure")
                            bottomSheetError.postValue(
                                "Connection failed" to "Please make sure your connection details are correct and that you are on the same Wi-Fi network."
                            )
                            return@launch
                        }
                    }
                }
            } finally {
                _isRegistering.postValue(false)
            }
        }
    }

    private fun shouldRetryRegisterWithNextIp(result: RegisterPeerResult): Boolean = when (result) {
        RegisterPeerResult.Conflict,
        RegisterPeerResult.TooManyRequests,
        RegisterPeerResult.ServerError,
        is RegisterPeerResult.Failure,
            -> true

        else -> false
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
        completeBottomSheetImportIfTerminal()
    }

    private fun appendIndexedDisplayName(baseName: String, index: Int): String {
        val dot = baseName.lastIndexOf('.')
        return if (dot > 0) {
            baseName.substring(0, dot) + " (" + index + ")" + baseName.substring(dot)
        } else {
            "$baseName ($index)"
        }
    }

    /** Picks a vault name not present in [existingNames] (case-sensitive, matches vault rules). */
    private fun uniqueVaultSiblingName(desired: String, existingNames: Set<String>): String {
        val base = desired.trim().ifBlank { "file" }
        if (base !in existingNames) return base
        var i = 1
        while (true) {
            val candidate = appendIndexedDisplayName(base, i++)
            if (candidate !in existingNames) return candidate
        }
    }

    private fun obtainTargetFolderId(): String {
        synchronized(p2pVaultImportLock) {
            targetFolderId?.let { return it }
            val title = (p2PState.session?.title ?: "").trim()
            val finalTitle = title.ifEmpty { "Transfer" }

            val vault = MyApplication.keyRxVault.rxVault.blockingFirst()
            val root = vault.root.blockingGet()
            val siblingNames =
                vault.list(root).blockingGet().mapNotNull { it.name }.toHashSet()
            val folderName = uniqueVaultSiblingName(finalTitle, siblingNames)
            val folder = vault.builder()
                .setName(folderName)
                .setType(VaultFile.Type.DIRECTORY)
                .build(root.id)
                .blockingGet()
            targetFolderId = folder.id
            return folder.id
        }
    }

    /** P2P bytes are written to a temp file; vault entry should use the sender’s name. */
    private fun vaultDisplayNameForP2pReceive(pf: ProgressFile, tempReceiveFile: File): String {
        val raw = pf.file.fileName.trim()
        val base = if (raw.isNotEmpty()) File(raw).name else tempReceiveFile.name
        return base.ifBlank { "file" }
    }

    /**
     * QuickTime / `.mov` is not handled by [MediaFileHandler.saveMp4Video] (MP4 pipeline). Store the file as-is
     * so it lands in the transfer folder; in-app playback depends on codecs, same as other opaque imports.
     */
    private fun shouldStoreP2pVideoAsOpaqueContainer(
        pf: ProgressFile,
        receivedFile: File
    ): Boolean {
        val mime = pf.file.fileType.lowercase(Locale.US)
        if (mime.contains("quicktime")) return true
        return vaultDisplayNameForP2pReceive(pf, receivedFile).lowercase(Locale.US).endsWith(".mov")
    }

    private fun importP2pReceivedFileAsVaultFile(
        vault: RxVault,
        receivedFile: File,
        pf: ProgressFile,
        folderId: String,
        vaultDisplayName: String,
    ): VaultFile? = try {
        FileInputStream(receivedFile).use { input ->
            vault.builder(input)
                .setName(vaultDisplayName)
                .setMimeType(pf.file.fileType)
                .setAnonymous(false)
                .setType(VaultFile.Type.FILE)
                .build(folderId)
                .blockingGet()
        }
    } catch (e: Exception) {
        Timber.e(e, "P2P vault import failed for ${pf.file.fileName}")
        null
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

        val tempPath = pf.path
        if (tempPath == null) {
            markProgressFileSaveFailed(pf)
            return
        }
        val receivedFile = File(tempPath)
        if (!receivedFile.exists()) {
            clearP2pReceiveHandoff(pf)
            markProgressFileSaveFailed(pf)
            return
        }

        try {
            val vaultFile = synchronized(p2pVaultImportLock) {
                val folderId = obtainTargetFolderId()
                val vault = MyApplication.keyRxVault.rxVault.blockingFirst()
                val parent = vault.get(folderId).blockingGet()
                val taken =
                    vault.list(parent).blockingGet().mapNotNull { it.name }.toHashSet()
                val displayName =
                    uniqueVaultSiblingName(vaultDisplayNameForP2pReceive(pf, receivedFile), taken)

                try {
                    when {
                        isImageFileType(pf.file.fileType) -> {
                            val bytes = receivedFile.readBytes()
                            if (pf.file.fileType.contains("png", true)) {
                                MediaFileHandler.savePngImage(bytes, folderId, displayName)
                            } else {
                                MediaFileHandler.saveJpegPhoto(bytes, folderId, displayName)
                                    .blockingGet()
                            }
                        }

                        isVideoFileType(pf.file.fileType) -> {
                            if (shouldStoreP2pVideoAsOpaqueContainer(pf, receivedFile)) {
                                importP2pReceivedFileAsVaultFile(
                                    vault,
                                    receivedFile,
                                    pf,
                                    folderId,
                                    displayName,
                                )
                            } else {
                                MediaFileHandler.saveMp4Video(
                                    receivedFile,
                                    folderId,
                                    false,
                                    displayName,
                                )
                            }
                        }

                        else -> {
                            importP2pReceivedFileAsVaultFile(
                                vault,
                                receivedFile,
                                pf,
                                folderId,
                                displayName,
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save file ${pf.file.fileName}")
                    null
                }
            }

            val txKey = pf.transmissionId
            if (vaultFile != null) {
                if (txKey != null) {
                    p2PState.session?.files?.get(txKey)?.status = P2PFileStatus.SAVED
                }
                pf.status = P2PFileStatus.SAVED
                pf.vaultFile = vaultFile
                savedCount++
                clearP2pReceiveHandoff(pf, receivedFile)
            } else {
                if (txKey != null) {
                    p2PState.session?.files?.get(txKey)?.status = P2PFileStatus.FAILED
                }
                pf.status = P2PFileStatus.FAILED
                clearP2pReceiveHandoff(pf, receivedFile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Saving to vault failed for file ${pf.file.fileName}")
            pf.status = P2PFileStatus.FAILED
            clearP2pReceiveHandoff(pf, receivedFile)
        } finally {
            postBottomSheetProgress()
            maybeFinalizeAfterSave()
        }
    }

    private fun markProgressFileSaveFailed(pf: ProgressFile) {
        val txKey = pf.transmissionId
        if (txKey != null) {
            p2PState.session?.files?.get(txKey)?.status = P2PFileStatus.FAILED
        }
        pf.status = P2PFileStatus.FAILED
    }

    /**
     * After vault/media import (or on failure), remove the P2P receive temp file and clear paths
     * so content does not linger on disk longer than necessary.
     */
    private fun clearP2pReceiveHandoff(pf: ProgressFile, receivedFile: File? = null) {
        receivedFile?.let { f ->
            runCatching {
                if (f.exists() && !f.delete()) {
                    Timber.w("P2P handoff: temp delete failed %s", f.path)
                }
            }
        }
        pf.path = null
        val tid = pf.transmissionId
        if (tid != null) {
            p2PState.session?.files?.get(tid)?.path = null
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
        if (!allFilesSavedOrFailed()) return

        // Server progress still says SENDING after a failed hash check; we must still close the UI.
        val canFinalize =
            sessionIsTerminal(triggerStatus) ||
                    triggerStatus == SessionStatus.SENDING ||
                    triggerStatus == SessionStatus.SAVING
        if (!canFinalize) return

        if (session.status == SessionStatus.FINISHED ||
            session.status == SessionStatus.FINISHED_WITH_ERRORS
        ) {
            return
        }

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
        completeBottomSheetImportIfTerminal()
    }

    /**
     * The import sheet only dismisses when current == total ([BottomSheetUtils.showProgressImportSheet]).
     * Hash failures never call [saveOneFile], so [savedCount] stays 0 and the sheet would hang at 0/N.
     */
    private fun completeBottomSheetImportIfTerminal() {
        val session = p2PState.session ?: return
        if (session.status != SessionStatus.FINISHED &&
            session.status != SessionStatus.FINISHED_WITH_ERRORS &&
            session.status != SessionStatus.CLOSED
        ) {
            return
        }
        if (totalFilesExpected <= 0) return
        _bottomSheetProgress.postValue(
            BottomSheetProgressState(
                current = totalFilesExpected,
                total = totalFilesExpected,
                percent = 100
            )
        )
    }

    // ------------------- Misc -------------------
    fun confirmPrepareUpload(sessionId: String, accepted: Boolean) {
        PeerEventManager.resolveUserDecision(sessionId, accepted)
        if (!accepted) {
            _incomingPrepareRequest.resetReplayCache()
            clearWaitingToPrepareSuccessNavigationBlock()
        }
    }

    fun resetRegistrationState() {
        _registrationServerSuccess.postValue(false)
        _registrationSuccess.postValue(false)
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
