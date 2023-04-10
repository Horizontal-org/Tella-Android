package rs.readahead.washington.mobile.util

/**
 * Application Status Provider.
 */
interface StatusProvider {

    /**
     * Checks network connectivity status.
     */
    fun isOnline(): Boolean
}
