package org.horizontal.tella.mobile.domain.peertopeer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerConnectionQrCodecTest {

    private val certHash = "a".repeat(64)

    @Test
    fun parseReceiverQr_iosStyleJson() {
        val json =
            """{"ip_address":["192.168.1.1"],"port":53320,"certificate_hash":"$certHash","pin":"123456","protocol_version":2}"""
        val result = PeerConnectionQrCodec.parseAny(json)
        assertTrue(result is PeerQrParseResult.Receiver)
        val qr = (result as PeerQrParseResult.Receiver).qr
        assertEquals(listOf("192.168.1.1"), qr.ipAddresses)
        assertEquals(53320, qr.port)
        assertEquals(certHash, qr.certificateHash)
        assertEquals("123456", qr.pin)
        assertEquals(2, qr.protocolVersion)
    }

    @Test
    fun parseReceiverQr_numericPin() {
        val json =
            """{"ip_address":["10.0.0.2"],"port":53320,"certificate_hash":"$certHash","pin":123456,"protocol_version":2}"""
        val result = PeerConnectionQrCodec.parseAny(json)
        assertTrue(result is PeerQrParseResult.Receiver)
        assertEquals("123456", (result as PeerQrParseResult.Receiver).qr.pin)
    }

    @Test
    fun parseSenderQr_iosStyleJson() {
        val json = """{"certificate_hash":"$certHash"}"""
        val result = PeerConnectionQrCodec.parseAny(json)
        assertTrue(result is PeerQrParseResult.Sender)
        assertEquals(certHash, (result as PeerQrParseResult.Sender).qr.certificateHash)
    }

    @Test
    fun senderQrRejectedWhenScanningReceiver() {
        val json = """{"certificate_hash":"$certHash"}"""
        val result = PeerConnectionQrCodec.parseAny(json)
        assertTrue(result is PeerQrParseResult.Sender)
    }

    @Test
    fun roundTripReceiverQr() {
        val json = PeerConnectionQrCodec.toReceiverJson(
            ipAddresses = listOf("192.168.0.5"),
            port = 53320,
            certificateHash = certHash,
            pin = "482910",
        )
        val result = PeerConnectionQrCodec.parseAny(json)
        assertTrue(result is PeerQrParseResult.Receiver)
    }

    @Test
    fun roundTripSenderQr() {
        val json = PeerConnectionQrCodec.toSenderJson(certHash)
        val result = PeerConnectionQrCodec.parseAny(json)
        assertTrue(result is PeerQrParseResult.Sender)
    }

    @Test
    fun parseReceiverQr_tellaDesktopLegacyJson() {
        val json =
            """{"ip_address":["192.168.1.5"],"port":53320,"certificate_hash":"$certHash","pin":"482910"}"""
        assertEquals(false, PeerConnectionQrCodec.isV1ReceiverQr(json))
        val result = PeerConnectionQrCodec.parseAny(json)
        assertTrue(result is PeerQrParseResult.Receiver)
        val qr = (result as PeerQrParseResult.Receiver).qr
        assertEquals(2, qr.protocolVersion)
        assertTrue(qr.senderShowHash)
    }
}
