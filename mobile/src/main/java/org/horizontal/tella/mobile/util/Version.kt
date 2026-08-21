package org.horizontal.tella.mobile.util

data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    companion object {
        fun parse(versionString: String?): Version? {
            val parts = versionString?.split(".")?.takeIf { it.size == 3 } ?: return null

            val (major, minor, patch) = parts.map { it.toIntOrNull() ?: return null }

            return Version(major, minor, patch)
        }
    }

    override fun compareTo(other: Version): Int {
        if (this.major != other.major) return this.major.compareTo(other.major)
        if (this.minor != other.minor) return this.minor.compareTo(other.minor)
        return this.patch.compareTo(other.patch)
    }
}