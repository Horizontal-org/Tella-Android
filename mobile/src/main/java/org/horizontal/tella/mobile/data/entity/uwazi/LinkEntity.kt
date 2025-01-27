package org.horizontal.tella.mobile.data.entity.uwazi

data class LinkEntity(
    val _id: String,
    val sublinks: List<SublinkEntity>,
    val title: String,
    val type: String,
    val url: String
)