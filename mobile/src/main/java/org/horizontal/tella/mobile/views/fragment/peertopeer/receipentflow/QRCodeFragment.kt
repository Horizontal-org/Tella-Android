package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.certificate.CertificateGenerator
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.port
import org.horizontal.tella.mobile.databinding.FragmentQrCodeBinding
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel
import javax.inject.Inject

@AndroidEntryPoint
class QRCodeFragment : BaseBindingFragment<FragmentQrCodeBinding>(FragmentQrCodeBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var payload: PeerConnectionPayload? = null
    private lateinit var qrPayload: String

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    @Inject
    lateinit var peerToPeerManager: PeerToPeerManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ip = viewModel.currentNetworkInfo?.ipAddress
        if (!ip.isNullOrEmpty()) {
            setupServerAndQr(ip)
        }
        handleBack()
        handleConnectManually()

        viewModel.isManualConnection = false

        viewModel.registrationServerSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Navigate to the next screen
                //  bundle.putBoolean("isSender", false)
                navManager().navigateFromQrCodeScreenToWaitingReceiverFragment()
                //  reset the LiveData state if we want to consume event once
                viewModel.resetRegistrationState()
            } else {
            }
        }
        initObservers()
    }

    private fun setupServerAndQr(ip: String) {
        val (keyPair, certificate) = CertificateGenerator.generateCertificate(ipAddress = ip)
        val config = KeyStoreConfig()

        peerServerStarterManager.startServer(ip, keyPair, certificate, config)

        val certHash = CertificateUtils.getPublicKeyHash(certificate)
        val pin = (100000..999999).random().toString()
        val port = port

        payload = PeerConnectionPayload(
            ipAddress = ip,
            port = port,
            certificateHash = certHash,
            pin = pin
        )

        qrPayload = Gson().toJson(payload)
        generateQrCode(qrPayload)
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
        binding.toolbar.backClickListener = { nav().popBackStack() }
        binding.backBtn.setOnClickListener { nav().popBackStack() }
    }

    private fun handleConnectManually() {
        binding.connectManuallyButton.setOnClickListener {
            connectManually()
        }
    }

    private fun connectManually() {
        payload?.let {
            bundle.putString("payload", qrPayload)
            navManager().navigateFromScanQrCodeToDeviceInfo()
        }
    }

    private fun initObservers() {
        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                bundle.putBoolean("isSender", false)
                navManager().navigateFromQrCodeScreenToWaitingReceiverFragment()
            } else {
            }
        }
    }

}