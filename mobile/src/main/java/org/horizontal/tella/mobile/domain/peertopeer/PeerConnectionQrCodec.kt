package org.horizontal.tella.mobile.domain.peertopeer

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * QR payload aligned with iOS [ConnectionInfo] JSON ( Swift `JSONEncoder` + [CodingKeys] ):
 * `ip_address` (JSON array of IPv4 strings), `port` (number), `certificate_hash` (hex), `pin` (string).
 * The default `port` must match [org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants.NEARBY_SHARING_TLS_PORT]
 * and the bound P2P HTTPS listener.
 */
data class ParsedPeerQr(
    val ipAddresses: List<String>,
    val port: Int,
    val certificateHash: String?,
    val pin: String,
)

object PeerConnectionQrCodec {

    fun parse(qrContent: String): ParsedPeerQr? {
        return try {
            val obj = JsonParser.parseString(qrContent).asJsonObject

            val ipEl = obj.get("ip_address") ?: return null
            val ips = when {
                ipEl.isJsonArray -> ipEl.asJsonArray.mapNotNull {
                    it.takeIf { it.isJsonPrimitive }?.asString?.trim()
                }
                ipEl.isJsonPrimitive -> listOf(ipEl.asString.trim())
                else -> emptyList()
            }.filter { it.isNotEmpty() }

            if (ips.isEmpty()) return null

            val port = obj.get("port")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                ?.asInt ?: return null

            val cert = obj.get("certificate_hash")
                ?.takeUnless { it.isJsonNull }
                ?.asString ?: return null

            val pin = obj.get("pin")
                ?.takeUnless { it.isJsonNull }
                ?.asString ?: return null

            ParsedPeerQr(ips, port, cert, pin)
        } catch (e: Exception) {
            null
        }
    }

    fun toJson(
        ipAddresses: List<String>,
        port: Int,
        certificateHash: String,
        pin: String
    ): String {
        require(ipAddresses.isNotEmpty()) { "ipAddresses cannot be empty" }
        require(port in 1..65535) { "Invalid port" }
        require(certificateHash.isNotBlank()) { "certificateHash cannot be blank" }
        require(pin.isNotBlank()) { "pin cannot be blank" }

        val obj = JsonObject()

        val ipArray = JsonArray()
        ipAddresses
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { ipArray.add(it) }

        require(ipArray.size() > 0) { "All IPs are empty" }

        obj.add("ip_address", ipArray)
        obj.addProperty("port", port)
        obj.addProperty("certificate_hash", certificateHash)
        obj.addProperty("pin", pin)

        return obj.toString()
    }
}
