package rs.readahead.washington.mobile.views.adapters.uwazi

data class ViewLanguageItem(
    val languageSmallText: String,
    val languageBigText: String,
    val key : String,
    val default : Boolean,
    val onLanguageClicked: () -> Unit,
) {
    fun onLanguageClicked() = onLanguageClicked.invoke()
}