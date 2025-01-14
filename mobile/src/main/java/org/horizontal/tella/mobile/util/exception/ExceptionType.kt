package org.horizontal.tella.mobile.util.exception

import org.horizontal.tella.mobile.R

enum class ExceptionType(val id: Int) {
    NO_CONNECTIVITY(R.string.not_internet_msg),
    OTHER(R.string.default_error_msg)
}
