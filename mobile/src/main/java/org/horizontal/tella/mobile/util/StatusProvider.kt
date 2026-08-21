package org.horizontal.tella.mobile.util

/**
 * Application Status Provider.
 */
interface StatusProvider {

    /**
     * Checks network connectivity status.
     */
    fun isOnline(): Boolean

    /**
     * Checks upstream bandwidth.
     */
    fun upstreamBandwidthKbps(): Int?
    
    /**
     * Checks if its connected to Wifi.
     */
    fun isConnectedToWifi(): Boolean
}
