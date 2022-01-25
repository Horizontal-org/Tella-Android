package rs.readahead.washington.mobile.data.entity.uwazi

data class Link(
    val _id: String,
    val sublinks: List<Any>,
    val title: String,
    val type: String,
    val url: String
)