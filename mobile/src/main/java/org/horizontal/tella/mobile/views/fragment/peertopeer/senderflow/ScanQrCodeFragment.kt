package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.databinding.ScanQrcodeFragmentBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel

class ScanQrCodeFragment :
    BaseBindingFragment<ScanQrcodeFragmentBinding>(ScanQrcodeFragmentBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private lateinit var barcodeView: CompoundBarcodeView

    companion object {
        private const val CAMERA_REQUEST_CODE = 1001
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barcodeView = CompoundBarcodeView(requireContext())
        barcodeView = binding.qrCodeScanView

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            baseActivity.maybeChangeTemporaryTimeout {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            }
        }
        handleBack()
        initListeners()
        initObservers()
    }

    private fun startScanning() {
        barcodeView.decodeContinuous(object : BarcodeCallback {

            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { qrContent ->
                    barcodeView.pause()

                    try {
                        val payload = Gson().fromJson(qrContent, PeerConnectionPayload::class.java)

                        viewModel.startRegistration(
                            ip = payload.connectCode,
                            port = payload.port.toString(),
                            hash = payload.certificateHash,
                            pin = payload.pin
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Show a message: Invalid QR Code
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
            }
        })

        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onDestroyView() {
        barcodeView.pauseAndWait()
        super.onDestroyView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        }
    }

    private fun handleBack() {
        binding.toolbar.backClickListener = { nav().popBackStack() }
        binding.backBtn.setOnClickListener { nav().popBackStack() }
    }

    private fun initListeners() {
        binding.connectManuallyButton.setOnClickListener {
            navManager().navigateFromScanQrCodeToSenderManualConnectionScreen()
        }
    }

    private fun initObservers(){
        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                navManager().navigateFromScanQrCodeToPrepareUploadFragment()
            } else {
                //  handle error UI
            }
        }
    }
}
