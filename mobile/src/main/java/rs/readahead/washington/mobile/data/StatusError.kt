package rs.readahead.washington.mobile.data

enum class StatusError(statusCode: Int) {
    UNAUTHORIZED(401),
    NOTFOUND(400),
    GENRALERROR(-1),
    SUCCESS(201)
}