package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.horizontal.tella.mobile.certificate.CertificateGenerator
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerServer
import org.horizontal.tella.mobile.data.peertopeer.port
import org.horizontal.tella.mobile.databinding.FragmentQrCodeBinding
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.TellaServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel

class QRCodeFragment : BaseBindingFragment<FragmentQrCodeBinding>(FragmentQrCodeBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var server: TellaServer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ip = viewModel.currentNetworkInfo?.ipAddress
        if (!ip.isNullOrEmpty()) {
            setupServerAndQr(ip)
        }
        handleBack()
    }

    private fun setupServerAndQr(ip: String) {
        val (keyPair, certificate) = CertificateGenerator.generateCertificate(ipAddress = ip)
        val config = KeyStoreConfig()

        server = TellaPeerToPeerServer(
            ip = ip,
            keyPair = keyPair,
            certificate = certificate,
            keyStoreConfig = config
        )
        server?.start()

        val certHash = CertificateUtils.getPublicKeyHash(certificate)
        val pin = "111111"
        val port = port

        val payload = PeerConnectionPayload(
            connectCode = ip,
            port = port,
            certificateHash = certHash,
            pin = pin
        )

        val qrPayload = Gson().toJson(payload)
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

    // TODO NEXT STEPS
    //TODO  WORK ON THE SENDER RESPONER
    // TODO PREAPRE REGISTER RESPONE

}