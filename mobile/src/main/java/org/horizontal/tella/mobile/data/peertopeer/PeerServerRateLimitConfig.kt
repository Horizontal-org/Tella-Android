package org.horizontal.tella.mobile.data.peertopeer

import java.util.concurrent.TimeUnit

/** Matches Tella Desktop `TimedRateLimiter.SetBurstAllowance(1000)`. */
const val PEER_RATE_LIMIT_DESKTOP_BURST = 1000

/**
 * Defaults align with Desktop cerca `TimedRateLimiter`-style limits (burst 1000, 30s refill, 24h idle).
 * With [useFullUriAsKey] false, behavior is IP-only (cerca `SetLimitAllRoutes(true)`). With true, buckets
 * are per client IP and request path (no query string).
 */
data class PeerServerRateLimitConfig(
    /** Nanoseconds between adding one token (Desktop: 30 seconds). */
    val refreshIntervalNanos: Long = TimeUnit.SECONDS.toNanos(30),
    /** Drop limiter state for this key after this idle period (Desktop: 24 hours). */
    val idleForgetNanos: Long = TimeUnit.HOURS.toNanos(24),
    /** Maximum token bucket size / initial burst (same value as Desktop burst allowance). */
    val burstAllowance: Int = PEER_RATE_LIMIT_DESKTOP_BURST,
    /**
     * When false, bucket key is client IP only (cerca-style). When true (default), key is `ip|requestPath`
     * using the URL path only (no query).
     */
    val useFullUriAsKey: Boolean = true,
    /**
     * Optional [Retry-After] response header when limited (seconds). Desktop returns 429 without this header; default null.
     */
    val retryAfterSecondsWhenLimited: Int? = null,
) {
    init {
        require(refreshIntervalNanos > 0)
        require(idleForgetNanos > 0)
        require(burstAllowance > 0)
    }

    companion object {
        val DEFAULT = PeerServerRateLimitConfig()
    }
}
