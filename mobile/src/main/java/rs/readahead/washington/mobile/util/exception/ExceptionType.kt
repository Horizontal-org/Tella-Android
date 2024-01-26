package rs.readahead.washington.mobile.util.exception

import rs.readahead.washington.mobile.R

enum class ExceptionType(val id: Int) {
    NO_CONNECTIVITY(R.string.not_internet_msg),
    OTHER(R.string.default_error_msg)
}
