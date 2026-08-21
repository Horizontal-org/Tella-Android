package org.horizontal.tella.mobile.domain.peertopeer

import java.util.UUID

data class KeyStoreConfig(
    val alias: String = "tella-alias",
    val password: CharArray = UUID.randomUUID().toString().toCharArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyStoreConfig

        if (alias != other.alias) return false
        if (!password.contentEquals(other.password)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alias.hashCode()
        result = 31 * result + password.contentHashCode()
        return result
    }
}
