package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.fragment.app.activityViewModels
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.horizontal.tella.mobile.databinding.FragmentSenderShowQrBinding
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionQrCodec
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * Sender flow step 1 — show sender certificate QR for the recipient to scan (iOS-aligned).
 */
class SenderShowQrFragment :
    BaseBindingFragment<FragmentSenderShowQrBinding>(FragmentSenderShowQrBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.prepareSenderSession()
        val payload = PeerConnectionQrCodec.toSenderJson(viewModel.p2PState.localSenderHash)
        generateQrCode(payload)
        handleBack()
        binding.scanRecipientQrButton.setOnClickListener {
            navManager().navigateFromSenderShowQrToScanReceiverQrScreen()
        }
        binding.connectManuallyButton.setOnClickListener {
            viewModel.p2PState.senderCanScanQr = false
            navManager().navigateFromSenderShowQrToSenderManualConnectionScreen()
        }
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
            Timber.e(e, "P2P sender QR: encode failed")
        }
    }

    private fun handleBack() {
        binding.toolbar.backClickListener = { nav().popBackStack() }
    }
}
