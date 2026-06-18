package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.data.peertopeer.managers.ReceiverSessionSetup
import org.horizontal.tella.mobile.data.peertopeer.model.P2PVerificationStep
import org.horizontal.tella.mobile.databinding.ShowDeviceInfoLayoutBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import javax.inject.Inject

/**
 * Recipient "Connect manually" screen. Standalone counterpart to [QRCodeFragment]: it brings up its
 * own receiver server (or reuses the one already started by the QR screen) and shows the IP / PIN /
 * port the sender must type in. When the sender pings, it moves on to recipient-hash verification.
 */
@AndroidEntryPoint
class ShowDeviceInfoFragment :
    BaseBindingFragment<ShowDeviceInfoLayoutBinding>(ShowDeviceInfoLayoutBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    @Inject
    lateinit var receiverSessionSetup: ReceiverSessionSetup

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    private var movedToVerification = false
    private var startedOwnServer = false
    private var serverStartRequested = false

    private val serverSetupMutex = Mutex()
    private var serverSetupDone = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.p2PState.isUsingManualConnection = true
        viewModel.p2PState.receiverCanScanQr = false
        initListeners()
        initObservers()
        ensureServer()
    }

    private fun initListeners() {
        binding.toolbar.backClickListener = { navigateBack() }
    }

    private fun ensureServer() {
        if (receiverSessionSetup.hasRunningSession()) {
            // Reuse the server already started by the QR screen.
            serverSetupDone = true
            showCredentials()
            return
        }
        if (serverStartRequested) return
        serverStartRequested = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModel.networkInfo.observe(viewLifecycleOwner) { info ->
                startServer(info.ipAddress.orEmpty())
            }
            viewModel.updateNetworkInfo()
        } else {
            startServer(viewModel.currentNetworkInfo?.ipAddress.orEmpty())
        }
    }

    private fun startServer(primaryIpHint: String) {
        viewLifecycleOwner.lifecycleScope.launch {

            serverSetupMutex.withLock {
                if (serverSetupDone || receiverSessionSetup.hasRunningSession()) {
                    serverSetupDone = true
                    showCredentials()
                    return@withLock
                }
                val json = receiverSessionSetup.start(
                    primaryIpHint = primaryIpHint,
                    discoveredIps = viewModel.collectLocalIpv4AddressesForNearbySharing(),
                ) ?: return@withLock
                startedOwnServer = true
                serverSetupDone = true
                showCredentials()
            }
        }
    }

    private fun showCredentials() = with(binding) {
        connectCode.setRightText(viewModel.p2PState.ip)
        pin.setRightText(viewModel.p2PState.pin)
        port.setRightText(viewModel.p2PState.port)
    }

    private fun initObservers() {
        lifecycleScope.launch {
            viewModel.clientHash.collect {
                moveToRecipientHashVerificationIfNeeded()
            }
        }
        lifecycleScope.launch {
            viewModel.recipientHashVerification.collect {
                moveToRecipientHashVerificationIfNeeded()
            }
        }
    }

    private fun moveToRecipientHashVerificationIfNeeded() {
        if (movedToVerification) return
        movedToVerification = true
        viewModel.p2PState.activeVerificationStep = P2PVerificationStep.RECIPIENT_HASH
        navManager().navigateFromDeviceInfoScreenTRecipientVerificationScreen()
    }

    private fun navigateBack() {
        // Only tear down the server if this screen started it; if it was reused from the QR screen,
        // leave that screen's server running.
        if (startedOwnServer) {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) { peerServerStarterManager.stopServer() }
                nav().popBackStack()
            }
        } else {
            nav().popBackStack()
        }
    }
}
