package org.horizontal.tella.mobile.domain.entity.peertopeer

data class DeviceMetadata(
    val deviceType: String = "mobile",
    val version: String = "2.0",
    val fingerprint: String,
    val serverPort: Int = 53317,
    val protocol: String = "https",
    val download: Boolean = true,
    val deviceModel: String = android.os.Build.MODEL ?: "Android",
    val alias: String = "AndroidDevice"
)
