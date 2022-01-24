package rs.readahead.washington.mobile.data.entity.uwazi

data class UwaziEntityRow(
    val __v: Int,
    val _id: String,
    val commonProperties: List<CommonProperty>,
    val default: Boolean = false,
    val name: String,
    val properties: List<Property>
)