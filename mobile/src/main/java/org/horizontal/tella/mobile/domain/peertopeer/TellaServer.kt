package org.horizontal.tella.mobile.domain.peertopeer

interface TellaServer {
    fun start()
    fun stop()
    val certificatePem: String
}