package rs.readahead.washington.mobile.data.entity.uwazi

data class Row(
    val __v: Int,
    val _id: String,
    val color: String,
    val commonProperties: List<CommonProperty>,
    val default: Boolean,
    val entityViewPage: String,
    val name: String,
    val properties: List<Property>
)