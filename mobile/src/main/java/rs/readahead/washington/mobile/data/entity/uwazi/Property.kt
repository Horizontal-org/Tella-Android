package rs.readahead.washington.mobile.data.entity.uwazi

data class Property(
    val _id: String,
    val content: String,
    val id: String,
    val label: String,
    val name: String,
    val nestedProperties: List<Any>,
    val required: Boolean,
    val showInCard: Boolean,
    val type: String
)