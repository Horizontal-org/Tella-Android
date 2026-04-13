package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.PeerKeyProvider
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.port
import org.horizontal.tella.mobile.databinding.FragmentQrCodeBinding
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class QRCodeFragment : BaseBindingFragment<FragmentQrCodeBinding>(FragmentQrCodeBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var payload: PeerConnectionPayload? = null
    private var qrPayload: String? = null

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    @Inject
    lateinit var peerToPeerManager: PeerToPeerManager

    @Inject
    lateinit var p2PSharedState: P2PSharedState

    /** Ensures only one setup runs at a time so server PIN and QR payload cannot diverge. */
    private val qrSetupMutex = Mutex()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ip = viewModel.currentNetworkInfo?.ipAddress
        if (!ip.isNullOrEmpty()) {
            setQrRegenerationLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    qrSetupMutex.withLock {
                        setupServerAndQr(ip)
                    }
                } finally {
                    if (isAdded) {
                        setQrRegenerationLoading(false)
                    }
                }
            }
        } else {
            setQrRegenerationLoading(false)
        }
        handleBack()
        handleConnectManually()

        viewModel.isManualConnection = false

        viewModel.registrationServerSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Navigate to the next screen
                navManager().navigateFromQrCodeScreenToWaitingReceiverFragment()
                //  reset the LiveData state if we want to consume event once
                viewModel.resetRegistrationState()
            } else {
            }
        }
        initObservers()
    }

    private fun setQrRegenerationLoading(loading: Boolean) {
        binding.progressCircular.isVisible = loading
        binding.connectManuallyButton.isEnabled = !loading
        if (loading) {
            binding.qrCodeImageView.setImageDrawable(null)
        }
    }

    private suspend fun setupServerAndQr(ip: String) {
        val keyPair = PeerKeyProvider.getKeyPair()
        val certificate = PeerKeyProvider.getCertificate(ip)
        val config = KeyStoreConfig()

        val certHash = CertificateUtils.getPublicKeyHash(certificate)
        val pin = (100000..999999).random()
        val port = port
        val pinString = pin.toString()

        val started = withContext(Dispatchers.IO) {
            peerServerStarterManager.startServer(
                ip,
                keyPair,
                pinString,
                certificate,
                config,
                p2PSharedState
            )
        }
        if (!started) {
            Timber.e("P2P QR: server failed to start; not updating PIN/QR")
            return
        }

        p2PSharedState.pin = pinString
        p2PSharedState.port = port.toString()
        p2PSharedState.hash = certHash
        p2PSharedState.ip = ip

        payload = PeerConnectionPayload(
            ipAddress = ip,
            port = port,
            certificateHash = certHash,
            pin = pinString
        )

        val json = Gson().toJson(payload)
        qrPayload = json
        generateQrCode(json)
    }


    private fun generateQrCode(content: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(
                content,
                BarcodeFormat.QR_CODE,
                600,
                600
            )
            binding.qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun handleBack() {
        val leave = {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    peerServerStarterManager.stopServer()
                }
                nav().popBackStack()
            }
        }
        binding.toolbar.backClickListener = { leave() }
        binding.backBtn.setOnClickListener { leave() }
    }

    private fun handleConnectManually() {
        binding.connectManuallyButton.setOnClickListener {
            connectManually()
        }
    }

    private fun connectManually() {
        val json = qrPayload ?: return
        if (payload == null) return
        bundle.putString("payload", json)
        navManager().navigateFromScanQrCodeToDeviceInfo()
    }

    private fun initObservers() {
        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                navManager().navigateFromQrCodeScreenToWaitingReceiverFragment()
            } else {
            }
        }
    }
}