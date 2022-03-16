package rs.readahead.washington.mobile.data

object ParamsNetwork {
    private const val URL_API = "/api/"
    const val URL_LOGIN = "${URL_API}login"
    const val URL_TEMPLATES = "${URL_API}templates"
    const val URL_ENTITIES = "${URL_API}entities"
    const val URL_WHITE_LISTED_ENTITIES = "${URL_API}public"
    const val URL_SETTINGS = "${URL_API}settings"
    const val URL_TRANSLATE_SETTINGS = "${URL_API}translations/setasdeafult"
    const val URL_TRANSLATION = "${URL_API}translations"
    const val URL_DICTIONARIES ="${URL_API}dictionaries"
    const val COOKIE = "Cookie"
    const val X_REQUESTED_WITH = "X-Requested-With"
    const val LOCALE_COOKIE = "locale="
}
