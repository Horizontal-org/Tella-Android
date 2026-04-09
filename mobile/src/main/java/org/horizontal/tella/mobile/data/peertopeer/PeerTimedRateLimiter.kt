package org.horizontal.tella.mobile.data.peertopeer

import java.util.concurrent.ConcurrentHashMap

/**
 * Token-bucket style limiter aligned with cerca `TimedRateLimiter` (v0.2.2): `golang.org/x/time/rate`
 * with `rate.Every(refreshPeriod)` and burst allowance. When [PeerServerRateLimitConfig.useFullUriAsKey]
 * is false (Desktop default), the map key is **client IP only**, matching cerca `access(identifier)` —
 * the URL is ignored for throttling.
 */
class PeerTimedRateLimiter(
    private val config: PeerServerRateLimitConfig = PeerServerRateLimitConfig.DEFAULT,
    private val clockNanos: () -> Long = System::nanoTime,
) {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    private data class Bucket(
        var tokens: Double,
        var lastUpdateNanos: Long,
    )

    /**
     * @param urlKey used only when [PeerServerRateLimitConfig.useFullUriAsKey] is true.
     * @return true if the request should be rejected (429), false if allowed.
     */
    fun isLimited(clientIp: String, urlKey: String): Boolean {
        val key = if (config.useFullUriAsKey) "$clientIp|$urlKey" else clientIp
        val now = clockNanos()
        val capacity = config.burstAllowance.toDouble()

        val bucket = buckets.compute(key) { _, existing ->
            when {
                existing == null -> Bucket(capacity, now)
                now - existing.lastUpdateNanos >= config.idleForgetNanos -> Bucket(capacity, now)
                else -> existing
            }
        }!!

        synchronized(bucket) {
            if (now - bucket.lastUpdateNanos >= config.idleForgetNanos) {
                bucket.tokens = capacity
                bucket.lastUpdateNanos = now
            }

            val elapsed = now - bucket.lastUpdateNanos
            bucket.tokens =
                minOf(capacity, bucket.tokens + elapsed / config.refreshIntervalNanos.toDouble())
            bucket.lastUpdateNanos = now

            return if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0
                false
            } else {
                true
            }
        }
    }
}
