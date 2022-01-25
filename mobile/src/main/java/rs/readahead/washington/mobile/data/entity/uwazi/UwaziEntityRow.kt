package rs.readahead.washington.mobile.data.entity.uwazi

import android.os.Parcelable


data class UwaziEntityRow(
    val __v: Int,
    val _id: String,
    val commonProperties: List<CommonProperty>,
    val default: Boolean = false,
    val name: String,
    val properties: List<Property>
)