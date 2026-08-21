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
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.data.peertopeer.managers.ReceiverSessionSetup
import org.horizontal.tella.mobile.databinding.FragmentQrCodeBinding
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
    lateinit var receiverSessionSetup: ReceiverSessionSetup

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
        viewModel.p2PState.isUsingManualConnection = false
        viewModel.p2PState.receiverCanScanQr = true

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
        val json = receiverSessionSetup.start(
            primaryIpHint = primaryIpHint,
            discoveredIps = viewModel.collectLocalIpv4AddressesForNearbySharing(),
        ) ?: return
        Timber.d("P2P receiver QR payload=%s", json)
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
        viewModel.p2PState.receiverCanScanQr = false
        viewModel.p2PState.isUsingManualConnection = true
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