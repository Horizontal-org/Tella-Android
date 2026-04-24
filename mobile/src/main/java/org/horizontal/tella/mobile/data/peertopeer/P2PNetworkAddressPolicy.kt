package org.horizontal.tella.mobile.data.peertopeer

import java.util.Locale

/**
 * Chooses a reachable same-LAN (RFC1918) address for Nearby Sharing discovery/QR, not the TLS bind address.
 *
 * Excludes loopback, link-local, CGNAT 100.64/10, multicast, and benchmark (198.18/15) ranges.
 * Does not treat "public" global IPv4 as a LAN address for P2P advertisement.
 */
object P2PNetworkAddressPolicy {

    const val INADDR_ANY_IPV4: String = "0.0.0.0"

    /**
     * IPv4 [a,b,c,d] for classification; [null] if not a valid IPv4 literal.
     */
    private fun parseIpv4Octets(s: String): IntArray? {
        val t = s.trim()
        if (t.isEmpty()) return null
        val parts = t.split('.')
        if (parts.size != 4) return null
        val out = IntArray(4)
        for (i in 0..3) {
            val p = parts[i]
            if (p.isEmpty() || p.length > 3) return null
            val n = p.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            out[i] = n
        }
        return out
    }

    private fun isExcludedUnusableLanIp(o: IntArray): Boolean {
        val a = o[0]
        val b = o[1]
        val c = o[2]
        val d = o[3]
        if (a == 0 && b == 0 && c == 0 && d == 0) return true
        if (a == 127) return true
        if (a == 169 && b == 254) return true
        if (a in 224..239) return true
        if (a == 100 && b in 64..127) return true
        if (a == 198 && b in 18..19) return true
        return false
    }

    /** Lower [tier] = higher preference. Null = not used for P2P advertisement. */
    private fun rfc1918PreferenceTier(o: IntArray): Int? {
        if (isExcludedUnusableLanIp(o)) return null
        val a = o[0]
        val b = o[1]
        when {
            a == 192 && b == 168 -> return 0
            a == 10 -> return 1
            a == 172 && b in 16..31 -> return 2
            else -> return null
        }
    }

    fun isEligibleForAdvertise(ip: String): Boolean {
        val octets = parseIpv4Octets(ip) ?: return false
        return rfc1918PreferenceTier(octets) != null
    }

    /**
     * Distinct, filtered, ordered: 192.168/16, then 10/8, then 172.16-31, stable tie-breaker by string.
     */
    fun filterAndOrderForAdvertise(ips: List<String>): List<String> {
        if (ips.isEmpty()) return emptyList()
        val withTier = ArrayList<Pair<String, Int>>()
        val seen = HashSet<String>()
        for (raw in ips) {
            val t = raw.trim()
            if (t.isEmpty()) continue
            if (!seen.add(t)) continue
            val o = parseIpv4Octets(t) ?: continue
            val tier = rfc1918PreferenceTier(o) ?: continue
            withTier.add(t to tier)
        }
        withTier.sortWith(compareBy<Pair<String, Int>> { it.second }.thenBy { it.first })
        return withTier.map { it.first }
    }

    /**
     * Human-readable reason for logging (English, for logcat / field debugging).
     */
    fun singleIpSelectionLog(chosen: String, orderedEligible: List<String>, rawSourcesLabeled: String): String {
        val o = parseIpv4Octets(chosen) ?: return "invalid IP for selection: $chosen"
        val tier = rfc1918PreferenceTier(o)
        val tierName = when (tier) {
            0 -> "192.168.0.0/16 (site-local, preferred)"
            1 -> "10.0.0.0/8 (private)"
            2 -> "172.16.0.0/12 (private)"
            else -> "unusable for LAN advertise"
        }
        return String.format(
            Locale.US,
            "selected=%s; tier=%s; ordered_eligible=%s; raw=%s",
            chosen,
            tierName,
            orderedEligible.joinToString(),
            rawSourcesLabeled
        )
    }
}
