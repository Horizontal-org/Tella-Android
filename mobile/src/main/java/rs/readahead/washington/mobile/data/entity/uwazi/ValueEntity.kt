package rs.readahead.washington.mobile.data.entity.uwazi

data class ValueEntity(
    val _id: String? = null,
    val id: String? = null,
    val label: String,
    val values : List<NestedValueEntity>?
)