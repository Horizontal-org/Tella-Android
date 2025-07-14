package org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.peertopeer.FingerprintFetcher
import org.horizontal.tella.mobile.data.peertopeer.ServerPinger
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.data.peertopeer.remote.RegisterPeerResult
import org.horizontal.tella.mobile.domain.peertopeer.IncomingRegistration
import org.horizontal.tella.mobile.domain.peertopeer.PeerEventManager
import org.horizontal.tella.mobile.util.NetworkInfo
import org.horizontal.tella.mobile.util.NetworkInfoManager
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PeerToPeerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerClient: TellaPeerToPeerClient,
    peerToPeerManager: PeerToPeerManager,
    val p2PState: P2PSharedState
) : ViewModel() {

    var isManualConnection: Boolean = true // default is manual
    var hasNavigatedToSuccessFragment = false
    var currentNetworkInfo: NetworkInfo? = null
    private val _registrationSuccess = SingleLiveEvent<Boolean>()
    val registrationSuccess: LiveData<Boolean> get() = _registrationSuccess
    private val _getHashSuccess = SingleLiveEvent<String>()
    val getHashSuccess: LiveData<String> get() = _getHashSuccess
    private val _getHashError = SingleLiveEvent<Throwable>()
    val getHashError: LiveData<Throwable> get() = _getHashError
    val clientHash = peerToPeerManager.clientConnected
    private val _registrationServerSuccess = SingleLiveEvent<Boolean>()
    val registrationServerSuccess: LiveData<Boolean> = _registrationServerSuccess
    private val _incomingPrepareRequest = SingleLiveEvent<PrepareUploadRequest?>()
    val incomingPrepareRequest: SingleLiveEvent<PrepareUploadRequest?> = _incomingPrepareRequest
    private val _incomingRequest = MutableStateFlow<IncomingRegistration?>(null)
    val incomingRequest: StateFlow<IncomingRegistration?> = _incomingRequest
    private val networkInfoManager = NetworkInfoManager(context)
    val networkInfo: LiveData<NetworkInfo> get() = networkInfoManager.networkInfo

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
                    // QR-mode: auto-accept immediately
                    PeerEventManager.confirmRegistration(registrationId, true)
                    _registrationSuccess.postValue(true)
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

                    _registrationSuccess.postValue(true)
                }

                RegisterPeerResult.InvalidPin -> {
                    _getHashError.postValue(Exception("Invalid PIN"))
                }

                RegisterPeerResult.InvalidFormat -> {
                    _getHashError.postValue(Exception("Invalid request format"))
                }

                RegisterPeerResult.Conflict -> {
                    _getHashError.postValue(Exception("Active session already exists"))
                }

                RegisterPeerResult.TooManyRequests -> {
                    _getHashError.postValue(Exception("Too many requests, try again later"))
                }

                RegisterPeerResult.ServerError -> {
                    _getHashError.postValue(Exception("Server error, try again later"))
                }

                RegisterPeerResult.RejectedByReceiver -> {
                    _getHashError.postValue(Exception("Receiver rejected the registration"))
                }

                is RegisterPeerResult.Failure -> {
                    Timber.e(result.exception, "Registration failed")
                    _getHashError.postValue(result.exception)
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
                _getHashError.postValue(error)
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

    override fun onCleared() {
        super.onCleared()
        p2PState.clear()
    }

}