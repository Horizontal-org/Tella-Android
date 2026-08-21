package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ScanQrcodeFragmentBinding
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionQrCodec
import org.horizontal.tella.mobile.domain.peertopeer.PeerQrParseResult
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerQrScanMode
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import org.hzontal.shared_ui.utils.DialogUtils
import timber.log.Timber

class ScanQrCodeFragment :
    BaseBindingFragment<ScanQrcodeFragmentBinding>(ScanQrcodeFragmentBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private lateinit var barcodeView: CompoundBarcodeView

    private val scanMode: String
        get() = arguments?.getString(PeerQrScanMode.ARG_SCAN_MODE)
            ?: PeerQrScanMode.SCAN_RECEIVER_QR

    companion object {
        private const val CAMERA_REQUEST_CODE = 1001
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleTextView.text = when (scanMode) {
            PeerQrScanMode.SCAN_SENDER_QR -> getString(R.string.qr_code_scan_sender_instruction)
            else -> getString(R.string.qr_code_scan_instruction)
        }

        barcodeView = binding.qrCodeScanView
        barcodeView.statusView.visibility = View.GONE
        barcodeView.viewFinder.visibility = View.GONE
        // Avoid false reads from 1D barcodes / screen glare; connection QRs are always QR_CODE.
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))

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
                result?.text?.let { raw ->
                    handleScannedPayload(raw.trim())
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
            }
        })
        barcodeView.resume()
    }

    private fun handleScannedPayload(trimmed: String) {
        Timber.d("P2P QR scan mode=%s payload=%s", scanMode, trimmed.take(200))
        when (scanMode) {
            PeerQrScanMode.SCAN_SENDER_QR -> handleSenderQr(trimmed)
            else -> handleReceiverQr(trimmed)
        }
    }

    private fun handleSenderQr(trimmed: String) {
        if (!trimmed.startsWith("{")) {
            Timber.w("P2P QR ignored non-JSON sender payload: %s", trimmed.take(80))
            return
        }
        when (val parsed = PeerConnectionQrCodec.parseAny(trimmed)) {
            is PeerQrParseResult.Sender -> {
                barcodeView.pause()
                viewModel.onSenderQrScanned(parsed.qr.certificateHash)
                navManager().navigateFromScanSenderQrToQrCodeScreen()
            }
            is PeerQrParseResult.Receiver -> showWrongQrTypeError(
                getString(R.string.peer_to_peer_scan_sender_qr_expected),
            )
            PeerQrParseResult.IncompatibleVersion -> {
                barcodeView.pause()
                viewModel.showIncompatibleProtocolError()
            }
            PeerQrParseResult.Invalid -> showInvalidQrError()
        }
    }

    private fun handleReceiverQr(trimmed: String) {
        if (!trimmed.startsWith("{")) {
            Timber.w("P2P QR ignored non-JSON payload: %s", trimmed.take(80))
            if (trimmed.all { it.isDigit() }) {
                showWrongQrTypeError(getString(R.string.peer_to_peer_qr_scanned_number_not_json))
            }
            return
        }
        if (PeerConnectionQrCodec.isV1ReceiverQr(trimmed)) {
            barcodeView.pause()
            viewModel.showIncompatibleProtocolError()
            return
        }
        when (val parsed = PeerConnectionQrCodec.parseAny(trimmed)) {
            is PeerQrParseResult.Receiver -> {
                barcodeView.pause()
                viewModel.onReceiverQrScanned(parsed.qr)
            }
            is PeerQrParseResult.Sender -> showWrongQrTypeError(
                getString(R.string.peer_to_peer_scan_recipient_qr_expected),
            )
            PeerQrParseResult.IncompatibleVersion -> {
                barcodeView.pause()
                viewModel.showIncompatibleProtocolError()
            }
            PeerQrParseResult.Invalid -> showInvalidQrError()
        }
    }

    private fun showWrongQrTypeError(message: String) {
        barcodeView.pause()
        bottomSheetError(getString(R.string.connection_failed), message)
    }

    private fun showInvalidQrError() {
        barcodeView.pause()
        bottomSheetError(
            getString(R.string.connection_failed),
            getString(R.string.peer_to_peer_invalid_qr_code),
        )
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized) {
            barcodeView.resume()
        }
    }

    override fun onDestroyView() {
        if (::barcodeView.isInitialized) {
            barcodeView.pauseAndWait()
        }
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
    }

    private fun initListeners() {
        if (scanMode == PeerQrScanMode.SCAN_RECEIVER_QR) {
            binding.connectManuallyButton.setOnClickListener {
                viewModel.p2PState.senderCanScanQr = false
                navManager().navigateFromScanQrCodeToSenderManualConnectionScreen()
            }
        } else {
            binding.connectManuallyButton.setOnClickListener {
                viewModel.p2PState.receiverCanScanQr = false
                viewModel.p2PState.isUsingManualConnection = true
                navManager().navigateFromScanSenderQrToDeviceInfoScreen()
            }
        }
    }

    private fun initObservers() {
        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success && scanMode == PeerQrScanMode.SCAN_RECEIVER_QR) {
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.set("registrationSuccess", true)
                navManager().navigateFromScanQrCodeToPrepareUploadFragment()
            }
        }

        viewModel.navigateToSenderVerification.observe(viewLifecycleOwner) { go ->
            if (go && scanMode == PeerQrScanMode.SCAN_RECEIVER_QR) {
                viewModel.navigateToSenderVerification.postValue(false)
                navManager().navigateFromScanQrCodeToSenderVerification()
            }
        }

        viewModel.bottomMessageError.observe(viewLifecycleOwner) { message ->
            DialogUtils.showBottomMessage(baseActivity, message, false)
        }

        viewModel.bottomSheetError.observe(viewLifecycleOwner) { (title, description) ->
            bottomSheetError(title, description)
        }

        viewModel.incompatibleProtocolError.observe(viewLifecycleOwner) {
            bottomSheetError(
                getString(R.string.peer_to_peer_incompatible_versions_title),
                getString(R.string.peer_to_peer_incompatible_versions_message),
            )
        }

        if (scanMode == PeerQrScanMode.SCAN_RECEIVER_QR) {
            viewModel.isRegistering.observe(viewLifecycleOwner) { registering ->
                binding.progressCircular.isVisible = registering
                binding.qrCodeScanViewFrame.isVisible = !registering
                binding.connectManuallyButton.isEnabled = !registering
            }
        }
    }

    private fun bottomSheetError(title: String, description: String) {
        showStandardSheet(
            baseActivity.supportFragmentManager,
            title,
            description,
            getString(R.string.try_again),
            null,
            onConfirmClick = {
                if (isAdded) {
                    viewModel.resetRegistrationState()
                    if (::barcodeView.isInitialized) {
                        barcodeView.resume()
                    }
                }
            },
            onCancelClick = null,
        )
    }
}
