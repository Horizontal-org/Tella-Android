package rs.readahead.washington.mobile.data.entity.uwazi

data class PropertyEntity(
    val _id: String,
    val content: String? = null,
    val id: String,
    val label: String? = null,
    val name: String? = null,
    val nestedProperties: List<Any>?,
    val required: Boolean,
    val showInCard: Boolean,
    val type: String
)