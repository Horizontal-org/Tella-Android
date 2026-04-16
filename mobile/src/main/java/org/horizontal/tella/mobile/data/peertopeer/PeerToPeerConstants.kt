package org.horizontal.tella.mobile.data.peertopeer

/**
 * Created by wafa on 3/7/2025.
 */
object PeerToPeerConstants {
    /**
     * App-private receive subdirectory under [android.content.Context.getNoBackupFilesDir].
     * Excluded from backup; not user-visible shared storage.
     */
    const val P2P_RECEIVE_SUBDIR = "p2p-receive"

    /** Delete only receive temp files older than this at server start (crash orphans). */
    const val P2P_RECEIVE_STALE_MAX_AGE_MS = 48L * 60 * 60 * 1000

    const val TRANSMISSION_ID_KEY = "transmissionId"
    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE = "Content-Type"
    const val CONTENT_TYPE_OCTET = "application/octet-stream"
}