package org.horizontal.tella.mobile.domain.peertopeer

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.horizontal.tella.mobile.data.peertopeer.PeerProtocolConstants

/**
 * QR payloads for Nearby Sharing protocol v2.
 *
 * Receiver QR: `ip_address`, `port`, `certificate_hash`, `pin`, `protocol_version`, `sender_show_hash`
 * Sender QR: `certificate_hash` only
 */
data class ParsedReceiverQr(
    val ipAddresses: List<String>,
    val port: Int,
    val certificateHash: String,
    val pin: String,
    val protocolVersion: Int,
    val senderShowHash: Boolean,
)

data class ParsedSenderQr(
    val certificateHash: String,
)

/** @deprecated Use [ParsedReceiverQr] */
typealias ParsedPeerQr = ParsedReceiverQr

sealed class PeerQrParseResult {
    data class Receiver(val qr: ParsedReceiverQr) : PeerQrParseResult()
    data class Sender(val qr: ParsedSenderQr) : PeerQrParseResult()
    data object IncompatibleVersion : PeerQrParseResult()
    data object Invalid : PeerQrParseResult()
}

object PeerConnectionQrCodec {

    fun parseAny(qrContent: String): PeerQrParseResult {
        return try {
            val trimmed = qrContent.trim().trimStart('\uFEFF')
            val obj = JsonParser.parseString(trimmed).asJsonObject
            val hasIp = obj.has("ip_address")
            val hasPort = obj.has("port")
            val hasPin = obj.has("pin")
            when {
                hasIp && hasPort && hasPin -> parseReceiverObject(obj)?.let { PeerQrParseResult.Receiver(it) }
                    ?: receiverParseFailure(obj)
                obj.has("certificate_hash") && !hasIp -> {
                    val hash = obj.stringField("certificate_hash").orEmpty()
                    if (hash.isEmpty()) PeerQrParseResult.Invalid
                    else PeerQrParseResult.Sender(ParsedSenderQr(hash))
                }
                else -> PeerQrParseResult.Invalid
            }
        } catch (_: Exception) {
            PeerQrParseResult.Invalid
        }
    }

    private fun receiverParseFailure(obj: JsonObject): PeerQrParseResult {
        val versionEl = obj.get("protocol_version")
        val protocolVersion = when {
            versionEl == null || versionEl.isJsonNull -> 1
            else -> obj.intField("protocol_version")
        }
        return if (protocolVersion == null || protocolVersion != PeerProtocolConstants.PROTOCOL_VERSION) {
            PeerQrParseResult.IncompatibleVersion
        } else {
            PeerQrParseResult.Invalid
        }
    }

    /** Legacy entry point; returns null for sender-only QRs or incompatible versions. */
    fun parse(qrContent: String): ParsedReceiverQr? =
        when (val result = parseAny(qrContent)) {
            is PeerQrParseResult.Receiver -> result.qr
            else -> null
        }

    fun parseSender(qrContent: String): ParsedSenderQr? =
        when (val result = parseAny(qrContent)) {
            is PeerQrParseResult.Sender -> result.qr
            else -> null
        }

    /**
     * True for pre-v2 receiver QRs (no [protocol_version] and no [certificate_hash]).
     * Tella Desktop and other legacy v2 peers may omit [protocol_version] but still include [certificate_hash].
     */
    fun isV1ReceiverQr(qrContent: String): Boolean {
        return try {
            val obj = JsonParser.parseString(qrContent.trim().trimStart('\uFEFF')).asJsonObject
            obj.has("ip_address") && obj.has("port") && obj.has("pin") &&
                !obj.has("protocol_version") && !obj.has("certificate_hash")
        } catch (_: Exception) {
            false
        }
    }

    fun toReceiverJson(
        ipAddresses: List<String>,
        port: Int,
        certificateHash: String,
        pin: String,
        senderShowHash: Boolean = false,
        protocolVersion: Int = PeerProtocolConstants.PROTOCOL_VERSION,
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
        obj.addProperty("protocol_version", protocolVersion)
        obj.addProperty("sender_show_hash", senderShowHash)
        return obj.toString()
    }

    fun toSenderJson(certificateHash: String): String {
        require(certificateHash.isNotBlank()) { "certificateHash cannot be blank" }
        val obj = JsonObject()
        obj.addProperty("certificate_hash", certificateHash)
        return obj.toString()
    }

    /** @deprecated Use [toReceiverJson] */
    fun toJson(
        ipAddresses: List<String>,
        port: Int,
        certificateHash: String,
        pin: String,
    ): String = toReceiverJson(ipAddresses, port, certificateHash, pin)

    private fun parseReceiverObject(obj: JsonObject): ParsedReceiverQr? {
        val ipEl = obj.get("ip_address") ?: return null
        val ips = (when {
            ipEl.isJsonArray -> ipEl.asJsonArray.mapNotNull { element ->
                element.stringValue()?.trim()
            }
            else -> ipEl.stringValue()?.trim()?.let { listOf(it) }
        } ?: emptyList()).filter { it.isNotEmpty() }
        if (ips.isEmpty()) return null

        val port = obj.intField("port") ?: return null

        val cert = obj.stringField("certificate_hash").orEmpty()
        if (cert.isEmpty()) return null

        val pin = obj.stringField("pin").orEmpty()
        if (pin.isEmpty()) return null

        val versionEl = obj.get("protocol_version")
        val desktopLegacy = versionEl == null || versionEl.isJsonNull
        val protocolVersion = when {
            desktopLegacy -> {
                // Tella Desktop QR: same fields as v2 but omits protocol_version / sender_show_hash
                if (cert.isNotEmpty()) PeerProtocolConstants.PROTOCOL_VERSION else 1
            }
            else -> obj.intField("protocol_version") ?: return null
        }

        if (protocolVersion != PeerProtocolConstants.PROTOCOL_VERSION) return null

        val senderShowHash = obj.get("sender_show_hash")?.takeIf { !it.isJsonNull }?.asBoolean
            ?: desktopLegacy

        return ParsedReceiverQr(
            ipAddresses = ips,
            port = port,
            certificateHash = cert,
            pin = pin,
            protocolVersion = protocolVersion,
            senderShowHash = senderShowHash,
        )
    }

    private fun JsonObject.stringField(key: String): String? = get(key).stringValue()

    private fun JsonObject.intField(key: String): Int? = get(key).intValue()

    /** Accept string or numeric JSON primitives (iOS always uses strings for pin; some encoders use numbers). */
    private fun com.google.gson.JsonElement?.stringValue(): String? = when {
        this == null || isJsonNull -> null
        isJsonPrimitive -> asJsonPrimitive.let { primitive ->
            when {
                primitive.isString -> primitive.asString
                primitive.isNumber -> primitive.asNumber.toString()
                else -> null
            }
        }
        else -> null
    }

    private fun com.google.gson.JsonElement?.intValue(): Int? = when {
        this == null || isJsonNull -> null
        isJsonPrimitive -> asJsonPrimitive.let { primitive ->
            when {
                primitive.isNumber -> primitive.asInt
                primitive.isString -> primitive.asString.trim().toIntOrNull()
                else -> null
            }
        }
        else -> null
    }
}
