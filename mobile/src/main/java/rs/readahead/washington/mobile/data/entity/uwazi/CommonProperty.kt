package rs.readahead.washington.mobile.data.entity.uwazi

data class CommonProperty(
    val _id: String,
    val generatedId: Boolean,
    val isCommonProperty: Boolean,
    val label: String,
    val localID: String,
    val name: String,
    val prioritySorting: Boolean,
    val type: String
)