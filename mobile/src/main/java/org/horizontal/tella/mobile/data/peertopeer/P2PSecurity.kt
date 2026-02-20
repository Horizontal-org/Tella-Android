package org.horizontal.tella.mobile.data.peertopeer

object P2PSecurity {
    /** Allow trust-all TLS only for the *manual connect handshake* (e.g., /ping). */
    @JvmStatic var allowInsecureManualHandshake: Boolean = false
}