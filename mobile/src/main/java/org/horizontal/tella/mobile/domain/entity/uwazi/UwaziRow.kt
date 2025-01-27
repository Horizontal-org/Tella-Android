package org.horizontal.tella.mobile.domain.entity.uwazi

data class UwaziRow(val version: Int,
                    val _id: String,
                    val commonProperties: List<CommonProperty>,
                    val default: Boolean = false,
                    val name: String,
                    var translatedName: String = "",
                    val properties: List<Property>)
