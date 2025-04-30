package org.horizontal.tella.mobile.views.fragment.peertopeer.data

data class NetworkInfo(
    val type: ConnectionType,
    val networkName: String?,
    val ipAddress: String?,
    var port: String = "53317"
)
