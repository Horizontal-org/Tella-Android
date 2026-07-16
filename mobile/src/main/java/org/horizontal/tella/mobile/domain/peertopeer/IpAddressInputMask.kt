package org.horizontal.tella.mobile.domain.peertopeer

/**
 * IPv4 input helper for manual mTLS entry.
 *
 * - Inserts `.` automatically after the 3rd digit of an octet (not after the 4th octet).
 * - User can type `.` manually to finish a 1- or 2-digit octet.
 */
object IpAddressInputMask {

    const val MAX_LENGTH = 15
    private const val OCTET_COUNT = 4
    private const val MAX_OCTET_VALUE = 255

    fun currentOctet(value: String): String {
        if (value.isEmpty() || value.endsWith('.')) return ""
        return value.substringAfterLast('.')
    }

    fun octetCount(value: String): Int {
        if (value.isEmpty()) return 0
        return value.count { it == '.' } + if (value.endsWith('.')) 0 else 1
    }

    fun appendChar(value: String, char: Char): String? {
        return when {
            char.isDigit() -> appendDigit(value, char)
            char == '.' -> appendDot(value)
            else -> null
        }
    }

    fun appendDigit(value: String, digit: Char): String? {
        if (value.length >= MAX_LENGTH) return null
        val current = currentOctet(value)
        if (current.length >= 3) return null

        val nextOctet = current + digit
        val nextValue = nextOctet.toIntOrNull() ?: return null
        if (nextValue > MAX_OCTET_VALUE) return null

        val withDigit = value + digit
        val completedOctets = withDigit.count { it == '.' } + 1
        if (nextOctet.length == 3 && completedOctets < OCTET_COUNT) {
            return "$withDigit."
        }
        return withDigit
    }

    fun appendDot(value: String): String? {
        if (value.isEmpty() || value.endsWith('.')) return null
        if (value.count { it == '.' } >= OCTET_COUNT - 1) return null
        val current = currentOctet(value)
        if (current.isEmpty()) return null
        return "$value."
    }

    fun applyInput(value: String, inserted: String): String? {
        if (inserted.isEmpty()) return value
        var result = value
        for (char in inserted) {
            result = appendChar(result, char) ?: return null
        }
        return result
    }

    fun isPartialValid(value: String): Boolean {
        if (value.isEmpty()) return true
        if (value.startsWith('.') || value.contains("..")) return false
        if (value.count { it == '.' } >= OCTET_COUNT) return false

        val parts = value.split('.')
        if (parts.size > OCTET_COUNT) return false

        parts.forEachIndexed { index, part ->
            if (part.isEmpty() && index < parts.lastIndex) return false
            if (part.length > 3) return false
            if (part.any { !it.isDigit() }) return false
            if (part.isNotEmpty() && (part.toIntOrNull()?.let { it in 0..MAX_OCTET_VALUE } != true)) {
                return false
            }
        }
        return true
    }

    fun isValidCompleteIp(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != OCTET_COUNT) return false
        return parts.all { part ->
            part.isNotEmpty() &&
                part.length <= 3 &&
                part.all { it.isDigit() } &&
                (part.toIntOrNull()?.let { it in 0..MAX_OCTET_VALUE } == true)
        }
    }

    fun toCanonicalIp(value: String): String? {
        if (!isValidCompleteIp(value)) return null
        return value.split('.').joinToString(".") { it.toInt().toString() }
    }
}
