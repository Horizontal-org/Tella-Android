package org.horizontal.tella.mobile.data.entity.uwazi

data class CommonPropertyEntity(
    val _id: String,
    val generatedId: Boolean,
    val isCommonProperty: Boolean,
    val label: String,
    val localID: String? = null,
    val name: String,
    val prioritySorting: Boolean,
    val type: String
)