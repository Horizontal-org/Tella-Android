package rs.readahead.washington.mobile.views.dialog.dropbox.internal

internal enum class TokenType(private val string: String) {
    OAUTH2("oauth2:"), OAUTH2CODE("oauth2code:");

    override fun toString(): String {
        return string
    }
}