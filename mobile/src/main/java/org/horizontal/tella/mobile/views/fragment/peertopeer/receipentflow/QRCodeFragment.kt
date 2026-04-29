package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.P2PNetworkAddressPolicy
import org.horizontal.tella.mobile.data.peertopeer.PeerKeyProvider
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.port
import org.horizontal.tella.mobile.databinding.FragmentQrCodeBinding
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionQrCodec
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class QRCodeFragment : BaseBindingFragment<FragmentQrCodeBinding>(FragmentQrCodeBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var qrPayload: String? = null

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    @Inject
    lateinit var peerToPeerManager: PeerToPeerManager

    @Inject
    lateinit var p2PSharedState: P2PSharedState

    /** Ensures only one setup runs at a time so server PIN and QR payload cannot diverge. */
    private val qrSetupMutex = Mutex()

    private var qrSetupStarted = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        qrPayload?.let { cached ->
            generateQrCode(cached)
            setQrRegenerationLoading(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModel.networkInfo.observe(viewLifecycleOwner) { info ->
                val ip = info.ipAddress
                if (!qrSetupStarted || qrPayload == null) {
                    qrSetupStarted = true
                    setQrRegenerationLoading(true)
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            qrSetupMutex.withLock {
                                setupServerAndQr(ip.orEmpty())
                            }
                        } finally {
                            if (isAdded) {
                                setQrRegenerationLoading(false)
                            }
                        }
                    }
                }
            }
            viewModel.updateNetworkInfo()
        } else {
            val ip = viewModel.currentNetworkInfo?.ipAddress
            qrSetupStarted = true
            setQrRegenerationLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    qrSetupMutex.withLock {
                        setupServerAndQr(ip.orEmpty())
                    }
                } finally {
                    if (isAdded) {
                        setQrRegenerationLoading(false)
                    }
                }
            }
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

    private suspend fun setupServerAndQr(primaryIpHint: String) {
        // New receiver session: drop any stale ping event from a previous attempt.
        peerToPeerManager.clearClientConnected()
        Timber.d("P2P receiver session started: cleared stale ping replay cache")

        val discovered = viewModel.collectLocalIpv4AddressesForNearbySharing()
        val hint = primaryIpHint.trim()
        val mergedForSelection = buildList {
            if (hint.isNotEmpty()) add(hint)
            for (a in discovered) {
                if (a.isNotBlank() && a !in this) add(a)
            }
        }
        val allIps = P2PNetworkAddressPolicy.filterAndOrderForAdvertise(mergedForSelection)
        if (allIps.isEmpty()) {
            Timber.e(
                "P2P QR: no site-local (RFC1918) IPv4 for certificate/QR after policy filter; raw merged=%s",
                mergedForSelection.joinToString(),
            )
            return
        }
        val advertiseToPeerPrimary = allIps.first()
        val keyPair = PeerKeyProvider.getKeyPair()
        val certificate = PeerKeyProvider.getCertificate(allIps)
        val config = KeyStoreConfig()

        val certHash = CertificateUtils.getLeafCertificateDerSha256Hex(certificate)
        val pin = (100000..999999).random()
        val port = port
        val pinString = pin.toString()

        val started = withContext(Dispatchers.IO) {
            peerServerStarterManager.startServer(
                advertiseToPeerPrimary,
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
        p2PSharedState.ip = advertiseToPeerPrimary

        val json = PeerConnectionQrCodec.toJson(allIps, port, certHash, pinString)
        qrPayload = json
        generateQrCode(json)
    }


    private fun generateQrCode(content: String) {
        try {
            val sizePx = (215f * resources.displayMetrics.density).roundToInt().coerceAtLeast(215)
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 0,
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints,
            )
            val w = bitMatrix.width
            val h = bitMatrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] =
                        if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = createBitmap(w, h)
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
            binding.qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Timber.e(e, "P2P QR: encode failed")
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
    }

    private fun handleConnectManually() {
        binding.connectManuallyButton.setOnClickListener {
            connectManually()
        }
    }

    private fun connectManually() {
        val json = qrPayload ?: return
        bundle.putString("payload", json)
        navManager().navigateFromScanQrCodeToDeviceInfo()
    }

    private fun initObservers() {
        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                navManager().navigateFromQrCodeScreenToWaitingReceiverFragment()
            }
        }
    }
}