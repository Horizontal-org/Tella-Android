package org.horizontal.tella.mobile.data.ouisync

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val READ_KEY_SIZE = 32
private const val NONCE_SIZE = 12
private const val TAG_BITS = 128
internal const val OUISYNC_READ_KEY_VERSION: Byte = 2
private fun keyContext(version: Byte) =
    "org.horizontal.tella.ouisync.keys.v$version".toByteArray(Charsets.UTF_8)

internal fun sealOuisyncReadKey(readKey: ByteArray, wrappingKey: SecretKey): ByteArray {
    require(readKey.size == READ_KEY_SIZE) { "Invalid Ouisync read key length" }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
    val nonce = cipher.iv
    check(nonce.size == NONCE_SIZE) { "Invalid Ouisync key nonce length" }
    cipher.updateAAD(keyContext(OUISYNC_READ_KEY_VERSION))
    cipher.updateAAD(byteArrayOf(OUISYNC_READ_KEY_VERSION))
    val ciphertext = cipher.doFinal(readKey)
    check(ciphertext.size == READ_KEY_SIZE + TAG_BITS / 8) { "Invalid Ouisync key ciphertext length" }
    return byteArrayOf(OUISYNC_READ_KEY_VERSION) + nonce + ciphertext
}

internal fun openOuisyncReadKey(envelope: ByteArray, wrappingKey: SecretKey): ByteArray {
    require(envelope.isNotEmpty()) { "Invalid Ouisync key envelope length" }
    val version = envelope[0]
    val plaintextSize = when (version) {
        1.toByte() -> READ_KEY_SIZE * 2
        OUISYNC_READ_KEY_VERSION -> READ_KEY_SIZE
        else -> throw IllegalArgumentException("Unsupported Ouisync key envelope version")
    }
    require(envelope.size == 1 + NONCE_SIZE + plaintextSize + TAG_BITS / 8) {
        "Invalid Ouisync key envelope length"
    }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val nonce = envelope.copyOfRange(1, 1 + NONCE_SIZE)
    cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(TAG_BITS, nonce))
    cipher.updateAAD(keyContext(version))
    cipher.updateAAD(byteArrayOf(version))
    val plaintext = cipher.doFinal(envelope, 1 + NONCE_SIZE, plaintextSize + TAG_BITS / 8)
    return try {
        plaintext.copyOfRange(0, READ_KEY_SIZE)
    } finally {
        plaintext.fill(0)
    }
}
