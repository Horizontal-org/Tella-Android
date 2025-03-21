package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.horizontal.tella.mobile.databinding.FragmentQrCodeBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class QRCodeFragment : BaseBindingFragment<FragmentQrCodeBinding>(FragmentQrCodeBinding::inflate) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        generateQrCode()
        binding.connectManuallyButton.setOnClickListener { }
    }


    private fun generateQrCode() {
        val textToEncode = "https://www.instagram.com/" // Change to your actual data

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(
                textToEncode,
                BarcodeFormat.QR_CODE,
                240,
                240
            )
            binding.qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}