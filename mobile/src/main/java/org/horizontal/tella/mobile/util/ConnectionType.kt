package org.horizontal.tella.mobile.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.util.LinkedHashSet
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import org.horizontal.tella.mobile.data.peertopeer.P2PNetworkAddressPolicy
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants

enum class ConnectionType {
    WIFI, CELLULAR, HOTSPOT, NONE
}

/**
 * Snapshot of connectivity for Nearby Sharing UI.
 *
 * [ipAddress] is only populated for [ConnectionType.WIFI] and [ConnectionType.HOTSPOT] when a
 * policy-eligible LAN IPv4 is available for QR/connection; otherwise null (including [ConnectionType.NONE]).
 */
data class NetworkInfo(
    val connectionType: ConnectionType,
    val ipAddress: String?,
    var port: String = PeerToPeerConstants.NEARBY_SHARING_TLS_PORT.toString()
)

class NetworkInfoManager(private val context: Context) {

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo

    private var currentNetworkInfo: NetworkInfo = NetworkInfo(ConnectionType.NONE, null)

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    fun fetchCurrentNetworkInfo() {
        val cm = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        val network: Network? = cm?.activeNetwork
        val caps: NetworkCapabilities? = cm?.getNetworkCapabilities(network)

        if (caps == null) {
            post(ConnectionType.NONE, null)
            return
        }

        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                post(ConnectionType.WIFI, computeDisplayCandidateIpv4(cm, network))
            }

            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (isLikelyWifiHotspotWhileCellularDefault()) {
                    post(
                        ConnectionType.HOTSPOT,
                        computeDisplayCandidateIpv4(cm, network),
                    )
                } else {
                    post(ConnectionType.NONE, null)
                }
            }

            else -> {
                post(ConnectionType.NONE, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun computeDisplayCandidateIpv4(
        cm: ConnectivityManager?,
        network: Network?,
    ): String? {
        val linkIps = getAllActiveIpv4FromDefaultNetwork(cm, network)
        return pickDisplayIpv4FromSources(linkIps, collectIpv4FromFilteredUpInterfaces())
    }

    private fun post(type: ConnectionType, ip: String?) {
        val safeIp = when (type) {
            ConnectionType.WIFI, ConnectionType.HOTSPOT -> ip
            else -> null
        }
        val info = NetworkInfo(type, safeIp)
        _networkInfo.postValue(info)
        currentNetworkInfo = info
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getAllActiveIpv4FromDefaultNetwork(
        cm: ConnectivityManager?,
        network: Network?
    ): List<String> {
        if (cm == null || network == null) return emptyList()
        val caps: NetworkCapabilities = cm.getNetworkCapabilities(network) ?: return emptyList()
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return emptyList()
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        ) {
            return emptyList()
        }
        val lp: LinkProperties = cm.getLinkProperties(network) ?: return emptyList()
        if (!lp.interfaceName.isNullOrEmpty() && isExcludedFromSensitiveInterfaceScan(lp.interfaceName)) {
            return emptyList()
        }
        return lp.linkAddresses
            .mapNotNull { it.address }
            .filter { it is Inet4Address && !it.isLoopbackAddress }
            .mapNotNull { it.hostAddress?.substringBefore('%')?.takeIf { h -> h.isNotBlank() } }
            .distinct()
            .take(MAX_ADVERTISE_IPV4_COLLECT)
    }

    /**
     * Best address for UI/QR: RFC1918 only, excludes 100.64/10, link-local, etc.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun pickDisplayIpv4FromSources(
        linkIpv4: List<String>,
        interfaceFallback: List<String>
    ): String? {
        val merged = (linkIpv4 + interfaceFallback).distinct().take(MAX_ADVERTISE_IPV4_COLLECT)
        return P2PNetworkAddressPolicy.filterAndOrderForAdvertise(merged).firstOrNull()
    }

    private fun collectIpv4FromFilteredUpInterfaces(): List<String> {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback && !isExcludedFromSensitiveInterfaceScan(it.name) }
                .flatMap { iface ->
                    iface.interfaceAddresses.mapNotNull { ia ->
                        val addr = ia.address
                        if (addr !is Inet4Address || addr.isLoopbackAddress) return@mapNotNull null
                        val h = addr.hostAddress?.substringBefore('%')?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        if (!P2PNetworkAddressPolicy.isEligibleForAdvertise(h)) return@mapNotNull null
                        h
                    }
                }
                .distinct()
                .take(MAX_ADVERTISE_IPV4_COLLECT)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isExcludedFromSensitiveInterfaceScan(interfaceName: String?): Boolean {
        if (interfaceName == null) return false
        val name = interfaceName.lowercase(Locale.US)
        if (name.startsWith("rmnet")) return true
        if (name.startsWith("ccmni")) return true
        if (name.startsWith("pdp")) return true
        if (name.startsWith("tun")) return true
        if (name.startsWith("wg")) return true
        if (name.startsWith("ppp")) return true
        if (name.startsWith("l2tp")) return true
        if (name.startsWith("ipsec")) return true
        if (name.startsWith("utun")) return true
        if (name.startsWith("awdl")) return true
        if (name.startsWith("lo")) return true
        if (name.startsWith("dummy")) return true
        return false
    }

    private fun nearbySharingInterfacePriority(interfaceName: String): Int? {
        if (isExcludedFromSensitiveInterfaceScan(interfaceName)) return null
        val name = interfaceName.lowercase(Locale.US)
        if (name.startsWith("wlan")) return 0
        if (name.startsWith("ap")) return 1
        if (name.startsWith("swlan")) return 1
        if (name.startsWith("bridge")) return 2
        if (name.startsWith("eth") || name.startsWith("en")) return 2
        return null
    }

    /**
     * iOS-like collection for Nearby Sharing:
     * - excludes cellular/tunnel interfaces
     * - prefers Wi-Fi/hotspot interfaces
     * - falls back to broader local IPv4 collection if filtered result is empty
     */
    fun collectNearbySharingIpv4Addresses(): List<String> {
        val bestScoreByIp = linkedMapOf<String, Int>()
        val candidates = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        for (iface in candidates) {
            val score = nearbySharingInterfacePriority(iface.name) ?: continue
            if (!iface.isUp || iface.isLoopback) continue
            val addresses = iface.interfaceAddresses.orEmpty()
            for (ia in addresses) {
                val addr = ia.address ?: continue
                if (addr !is Inet4Address || addr.isLoopbackAddress) continue
                val ip =
                    addr.hostAddress?.substringBefore('%')?.takeIf { it.isNotBlank() } ?: continue
                if (!P2PNetworkAddressPolicy.isEligibleForAdvertise(ip)) continue
                val prev = bestScoreByIp[ip]
                if (prev == null || score < prev) bestScoreByIp[ip] = score
            }
        }

        if (bestScoreByIp.isNotEmpty()) {
            return P2PNetworkAddressPolicy
                .filterAndOrderForAdvertise(bestScoreByIp.keys.toList())
                .take(MAX_ADVERTISE_IPV4_COLLECT)
        }

        // Broaden to link + interface addresses, still RFC1918-only for what we show/share.
        val fromAll = collectAllLocalIpv4AddressesRaw()
        return P2PNetworkAddressPolicy.filterAndOrderForAdvertise(fromAll)
            .take(MAX_ADVERTISE_IPV4_COLLECT)
    }

    /**
     * IPv4 addresses on active links (API 23+) plus interface fallback. Used to match QR multi-IP against local subnets.
     */
    @SuppressLint("MissingPermission")
    fun collectAllLocalIpv4Addresses(): List<String> {
        return P2PNetworkAddressPolicy
            .filterAndOrderForAdvertise(collectAllLocalIpv4AddressesRaw())
            .take(MAX_ADVERTISE_IPV4_COLLECT)
    }

    /**
     * Internal collection for candidate RFC1918 addresses. Not returned unfiltered from public APIs.
     */
    @SuppressLint("MissingPermission")
    private fun collectAllLocalIpv4AddressesRaw(): List<String> {
        val out = LinkedHashSet<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            if (cm != null) {
                for (network in cm.allNetworks) {
                    if (!isNetworkEligibleForLocalIpv4Collection(cm, network)) continue
                    val lp = cm.getLinkProperties(network) ?: continue
                    if (!lp.interfaceName.isNullOrEmpty() && isExcludedFromSensitiveInterfaceScan(lp.interfaceName)) {
                        continue
                    }
                    for (la in lp.linkAddresses) {
                        val a = la.address
                        if (a is Inet4Address && !a.isLoopbackAddress) {
                            val h = a.hostAddress?.substringBefore('%')
                            if (!h.isNullOrBlank() && P2PNetworkAddressPolicy.isEligibleForAdvertise(
                                    h
                                )
                            ) {
                                out.add(h)
                            }
                        }
                    }
                }
            }
        }
        if (out.isEmpty()) {
            out.addAll(collectIpv4FromFilteredUpInterfaces())
        }
        return out.toList().take(MAX_ADVERTISE_IPV4_COLLECT)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkEligibleForLocalIpv4Collection(
        cm: ConnectivityManager,
        network: Network
    ): Boolean {
        val caps = cm.getNetworkCapabilities(network) ?: return false
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Detects portable Wi-Fi hotspot without hidden APIs: look for a separate Wi-Fi network with
     * a soft-AP-style interface and an advertisable RFC1918 address while the default network is cellular.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isLikelyWifiHotspotWhileCellularDefault(): Boolean {
        val cm = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            ?: return isLikelySoftApByInterfaceScan()
        val active = cm.activeNetwork ?: return isLikelySoftApByInterfaceScan()
        val activeCaps = cm.getNetworkCapabilities(active) ?: return isLikelySoftApByInterfaceScan()
        if (!activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return false
        for (network in cm.allNetworks) {
            if (network == active) continue
            val c = cm.getNetworkCapabilities(network) ?: continue
            if (!c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
            val lp = cm.getLinkProperties(network) ?: continue
            val ifn = lp.interfaceName?.lowercase(Locale.US) ?: continue
            if (!isLikelySoftwareAccessPointInterface(ifn)) continue
            val hasAdvertisable = lp.linkAddresses.any { la ->
                val a = la.address
                a is Inet4Address &&
                    P2PNetworkAddressPolicy.isEligibleForAdvertise(
                        a.hostAddress?.substringBefore('%')?.trim().orEmpty()
                    )
            }
            if (hasAdvertisable) return true
        }
        return isLikelySoftApByInterfaceScan()
    }

    private fun isLikelySoftwareAccessPointInterface(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        if (n.startsWith("softap")) return true
        if (n.startsWith("swlan")) return true
        if (n == "ap0" || n == "ap1" || n.startsWith("ap_br")) return true
        return n.matches(AP_NUMBERED_IFACE)
    }

    private fun isLikelySoftApByInterfaceScan(): Boolean {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { ni ->
                if (!ni.isUp || ni.isLoopback) return@any false
                if (isExcludedFromSensitiveInterfaceScan(ni.name)) return@any false
                if (!isLikelySoftwareAccessPointInterface(ni.name)) return@any false
                ni.interfaceAddresses.any { ia ->
                    val a = ia.address
                    a is Inet4Address &&
                            P2PNetworkAddressPolicy.isEligibleForAdvertise(
                                a.hostAddress?.substringBefore('%')?.trim().orEmpty()
                            )
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        private const val MAX_ADVERTISE_IPV4_COLLECT = 5
        private val AP_NUMBERED_IFACE = Regex("^ap\\d+$")
    }
}
