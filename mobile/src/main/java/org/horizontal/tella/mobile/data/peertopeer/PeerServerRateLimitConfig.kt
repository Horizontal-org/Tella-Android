package org.horizontal.tella.mobile.data.peertopeer

import java.util.concurrent.TimeUnit

/** Matches Tella Desktop `TimedRateLimiter.SetBurstAllowance(1000)`. */
const val PEER_RATE_LIMIT_DESKTOP_BURST = 1000

/**
 * Cross-platform references (local clones may live e.g. under `~/Desktop/tella-desktop`,
 * `~/Documents/Tella-iOS`):
 *
 * **Tella Desktop** — `backend/core/modules/server/service.go` (`NewRateLimitingWare`),
 * `gomod.cblgh.org/cerca v0.2.2` / `limiter.TimedRateLimiter`. With `SetLimitAllRoutes(true)`, cerca keys
 * [golang.org/x/time/rate.Limiter] by **client IP only**; the URL argument does not affect the bucket.
 *
 * **Tella iOS** — `Tella/Data/Networking/NearbySharing/NearbySharingRateLimiter.swift`: same numeric defaults
 * (burst 1000, 30s refill, 24h idle cleanup) but buckets are **per IP and route** (`ip|route`, see `makeKey`);
 * `NearbySharingServer` passes the request endpoint path. That is closer to setting [useFullUriAsKey] to true
 * on Android than to Desktop/cerca IP-only behavior.
 */
data class PeerServerRateLimitConfig(
    /** Nanoseconds between adding one token (Desktop: 30 seconds). */
    val refreshIntervalNanos: Long = TimeUnit.SECONDS.toNanos(30),
    /** Drop limiter state for this key after this idle period (Desktop: 24 hours). */
    val idleForgetNanos: Long = TimeUnit.HOURS.toNanos(24),
    /** Maximum token bucket size / initial burst (same value as Desktop burst allowance). */
    val burstAllowance: Int = PEER_RATE_LIMIT_DESKTOP_BURST,
    /**
     * When false (default), the rate-limit bucket key is **client IP only**, matching cerca
     * `TimedRateLimiter` with `SetLimitAllRoutes(true)`. When true, key is IP plus URI (path + query),
     * which is stricter than Desktop/cerca.
     */
    val useFullUriAsKey: Boolean = false,
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
