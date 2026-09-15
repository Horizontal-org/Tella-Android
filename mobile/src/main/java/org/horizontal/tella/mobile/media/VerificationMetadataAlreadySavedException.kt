package org.horizontal.tella.mobile.media

class VerificationMetadataAlreadySavedException(val path: String) : Exception(path)

fun Throwable.asVerificationMetadataAlreadySaved(): VerificationMetadataAlreadySavedException? {
    var current: Throwable? = this
    while (current != null) {
        if (current is VerificationMetadataAlreadySavedException) {
            return current
        }
        current = current.cause
    }
    return null
}
