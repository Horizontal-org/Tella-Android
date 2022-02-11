package rs.readahead.washington.mobile.domain.entity.uwazi

data class RowDictionary(
    val version: Int,
    val _id: String,
    val name: String,
    val values: List<Value>
)
