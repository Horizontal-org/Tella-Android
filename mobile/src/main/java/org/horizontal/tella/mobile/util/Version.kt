package org.horizontal.tella.mobile.util

data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    companion object {
        fun parse(versionString: String?): Version {
            val parts = versionString?.split(".")?.map { it.toIntOrNull() ?: 0 }
            return Version(
                    major = parts?.getOrNull(0) ?: 0,
                    minor = parts?.getOrNull(1) ?: 0,
                    patch = parts?.getOrNull(2) ?: 0
            )
        }
    }

    override fun compareTo(other: Version): Int {
        if (this.major != other.major) return this.major.compareTo(other.major)
        if (this.minor != other.minor) return this.minor.compareTo(other.minor)
        return this.patch.compareTo(other.patch)
    }
}