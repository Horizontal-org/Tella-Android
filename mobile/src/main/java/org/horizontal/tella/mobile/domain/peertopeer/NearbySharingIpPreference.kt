package org.horizontal.tella.mobile.domain.peertopeer

/**
 * Picks a sensible order for QR-encoded IPs on the **sender**: try addresses on the same
 * IPv4 /24-style subnet as a local interface first (matches iOS / product heuristic).
 */
object NearbySharingIpPreference {

    /** For IPv4 `a.b.c.d`, returns `a.b.c`. Non-IPv4 → `null`. */
    fun ipv4ThreeOctetPrefix(ip: String): String? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        if (parts.any { it.isEmpty() || it.any { c -> !c.isDigit() } }) return null
        return "${parts[0]}.${parts[1]}.${parts[2]}"
    }

    /**
     * Puts QR IPs whose subnet matches any local IPv4 interface first, then the rest in original order.
     */
    fun preferredNearbySharingIPOrder(
        qrAddresses: List<String>,
        localDeviceIPv4Addresses: List<String>,
    ): List<String> {
        if (qrAddresses.isEmpty()) return emptyList()
        val localSubnets = localDeviceIPv4Addresses.mapNotNull { ipv4ThreeOctetPrefix(it) }.toSet()
        if (localSubnets.isEmpty()) return qrAddresses.distinct()

        val preferred = mutableListOf<String>()
        val rest = mutableListOf<String>()
        for (ip in qrAddresses) {
            val p = ipv4ThreeOctetPrefix(ip)
            if (p != null && localSubnets.contains(p)) preferred.add(ip)
            else rest.add(ip)
        }
        val seen = mutableSetOf<String>()
        return (preferred + rest).filter { seen.add(it) }
    }
}
