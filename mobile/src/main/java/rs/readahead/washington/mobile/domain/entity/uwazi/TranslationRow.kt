package rs.readahead.washington.mobile.domain.entity.uwazi

data class TranslationRow(val _id: String,
                          val contexts: List<TranslationContext>,
                          val locale: String)
