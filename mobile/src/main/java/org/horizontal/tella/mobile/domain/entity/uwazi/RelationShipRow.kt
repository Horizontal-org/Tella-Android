package org.horizontal.tella.mobile.domain.entity.uwazi

data class RelationShipRow(
    val id: String,
    val name: String,
    val values: List<Value>,
    val version: Int,
    val type: String?,
    val entityViewPage: String?)
