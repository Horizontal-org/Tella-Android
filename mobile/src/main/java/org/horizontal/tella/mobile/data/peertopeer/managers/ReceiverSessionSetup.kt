package org.horizontal.tella.mobile.data.peertopeer.managers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.P2PNetworkAddressPolicy
import org.horizontal.tella.mobile.data.peertopeer.PeerKeyProvider
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.port
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionQrCodec
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts the receiver mTLS server and populates [P2PSharedState] with the freshly generated
 * connection credentials (IP / port / PIN / certificate hash). Shared by the QR screen and the
 * manual-connection screen so neither has to own the other's setup logic.
 */
@Singleton
class ReceiverSessionSetup @Inject constructor(
    private val peerServerStarterManager: PeerServerStarterManager,
    private val p2PSharedState: P2PSharedState,
) {

    suspend fun start(primaryIpHint: String, discoveredIps: List<String>): String? {
        val hint = primaryIpHint.trim()
        val mergedForSelection = buildList {
            if (hint.isNotEmpty()) add(hint)
            for (address in discoveredIps) {
                if (address.isNotBlank() && address !in this) add(address)
            }
        }

        val allIps = P2PNetworkAddressPolicy.filterAndOrderForAdvertise(mergedForSelection)
        if (allIps.isEmpty()) {
            Timber.e(
                "P2P receiver: no site-local (RFC1918) IPv4 after policy filter; raw merged=%s",
                mergedForSelection.joinToString(),
            )
            return null
        }

        val advertiseToPeerPrimary = allIps.first()
        val keyPair = PeerKeyProvider.getKeyPair()
        val certificate = PeerKeyProvider.getCertificate(allIps)
        val certHash = CertificateUtils.getLeafCertificateDerSha256Hex(certificate)
        val pinString = (100000..999999).random().toString()

        val started = withContext(Dispatchers.IO) {
            peerServerStarterManager.startServer(
                advertiseToPeerPrimary,
                keyPair,
                pinString,
                certificate,
                KeyStoreConfig(),
                p2PSharedState,
            )
        }
        if (!started) {
            Timber.e("P2P receiver: server failed to start")
            return null
        }

        p2PSharedState.pin = pinString
        p2PSharedState.port = port.toString()
        p2PSharedState.hash = certHash
        p2PSharedState.ip = advertiseToPeerPrimary
        p2PSharedState.advertisedIpAddresses = allIps
        p2PSharedState.localReceiverHash = certHash

        return PeerConnectionQrCodec.toReceiverJson(
            ipAddresses = allIps,
            port = port,
            certificateHash = certHash,
            pin = pinString,
            senderShowHash = false,
        )
    }

    /** True when a receiver server is already running with credentials available for reuse. */
    fun hasRunningSession(): Boolean =
        peerServerStarterManager.isRunning() &&
                p2PSharedState.pin?.isNotBlank() == true &&
                p2PSharedState.port.isNotBlank()
}
