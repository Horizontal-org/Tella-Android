package rs.readahead.washington.mobile.domain.entity.uwazi

data class UwaziRow(val version: Int,
                    val _id: String,
                    val commonProperties: List<CommonProperty>,
                    val default: Boolean = false,
                    val name: String,
                    val properties: List<Property>)
