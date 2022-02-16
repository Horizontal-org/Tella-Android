package rs.readahead.washington.mobile.data.entity.uwazi

data class TranslationRowEntity(
    val _id: String?,
    val contexts: List<TranslationContextEntity>?,
    val locale: String?
)