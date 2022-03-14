package rs.readahead.washington.mobile.domain.entity.uwazi

data class TranslationContext(    val _id: String,
                                  val id: String,
                                  val label: String,
                                  val type: String,
                                  val values: Map<String,String>)
